package com.alex.materialdiary.sys.net

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import androidx.navigation.NavController
import com.alex.materialdiary.NavGraphDirections
import com.alex.materialdiary.MainActivity
import com.alex.materialdiary.R
import com.alex.materialdiary.sys.DiaryPreferences
import com.alex.materialdiary.sys.ReadWriteJsonFileUtils
import com.alex.materialdiary.sys.net.Crypt.Companion.genPdaKey
import com.alex.materialdiary.sys.net.Crypt.Companion.randomSymbols
import com.alex.materialdiary.sys.net.models.ClassicBody
import com.alex.materialdiary.sys.net.models.pda.PDBody
import com.alex.materialdiary.sys.net.models.ShareUser
import com.alex.materialdiary.sys.net.models.get_user.UserInfoRequest
import com.alex.materialdiary.sys.net.models.all_periods.AllPeriods
import com.alex.materialdiary.sys.net.models.assistant_tips.AssistantTips
import com.alex.materialdiary.sys.net.models.assistant_tips.AssistantTipsRequestBody
import com.alex.materialdiary.sys.net.models.diary_day.DiaryDay
import com.alex.materialdiary.sys.net.models.dop_programs.DopProgramData
import com.alex.materialdiary.sys.net.models.dop_programs.DopPrograms
import com.alex.materialdiary.sys.net.models.get_user.UserData
import com.alex.materialdiary.sys.net.models.get_user.UserInfo
import com.alex.materialdiary.sys.net.models.marks.Item
import com.alex.materialdiary.sys.net.models.news.News
import com.alex.materialdiary.sys.net.models.period_marks.PeriodMarks
import com.alex.materialdiary.sys.net.models.periods.Periods
import com.alex.materialdiary.ui.login.LoginActivity
import com.alex.materialdiary.utils.MarksTranslator
import com.alex.materialdiary.utils.MarksTranslator.Companion.get_cur_period
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import retrofit2.HttpException
import xdroid.toaster.Toaster
import java.net.ConnectException
import java.util.*

class PskoveduApi(context: Context, navController: NavController?) {
    private val endpoints: PskoveduEndpoints = PskoveduClient.getEndpoints()
    var guid = ""
    var sid = ""
    val gson = Gson()
    val context: Context = context
    var pdaKey: String = "";
    var apikey = ""
    val DPrefs = DiaryPreferences(context)
    val jsonUtils = ReadWriteJsonFileUtils(context)

    companion object {
        var i: PskoveduApi? = null

        fun getInstance(context: Context, navController: NavController): PskoveduApi {
            if (i == null)
                i = PskoveduApi(context, navController)
            return i as PskoveduApi
        }

        fun getInstance(context: Context): PskoveduApi {
            if (i == null)
                i = PskoveduApi(context, null)
            return i as PskoveduApi
        }

        fun getInstance(): PskoveduApi {
            return i as PskoveduApi
        }
    }

    fun processException(e: Exception) {
        when (e) {
            is HttpException -> HttpError()
            is ConnectException -> ConnectError()
            else -> {
                FirebaseCrashlytics.getInstance().recordException(e)
                UnknownError()
            }
        }
    }

    init {
        if (DPrefs.contains(DiaryPreferences.GUID)) {
            guid = DPrefs.get(DiaryPreferences.GUID)
            if (guid.length > 1) apikey = Crypt().encryptSYS_GUID(guid)
            if (DPrefs.contains(DiaryPreferences.PDA)) {
                if (DPrefs.get(DiaryPreferences.PDA_GUID) == guid) pdaKey =
                    DPrefs.get(DiaryPreferences.PDA)
                else {
                    val gson = Gson()
                    val utils = ReadWriteJsonFileUtils(context)
                    val datas = utils.readJsonFileData("users.json")
                    if (datas != null && datas.length > 100) {
                        val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
                        val name =
                            entity.data.surname + " " + entity.data.name + " " + entity.data.secondname
                        CoroutineScope(Dispatchers.IO).launch {
                            checkPdaKey(name, guid, navController)
                        }
                    }
                }
            } else {
                val gson = Gson()
                val utils = ReadWriteJsonFileUtils(context)
                val datas = utils.readJsonFileData("users.json")
                if (datas != null && datas.length > 100) {
                    val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
                    val name =
                        entity.data.surname + " " + entity.data.name + " " + entity.data.secondname
                    CoroutineScope(Dispatchers.IO).launch {
                        checkPdaKey(name, guid, navController)
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        checkPdaKey(randomSymbols(10), guid, navController)
                    }
                }
            }
        } else {
            Handler(Looper.getMainLooper()).post { navController?.navigate(R.id.to_new_ch_users) }
        }

    }

    fun HttpError() {
        MainActivity.showSnack("Невозможно получить данные, попробуйте позже")
    }

    fun ConnectError() {
        MainActivity.showSnack("Невозможно получить данные, проверьте подключение к интернету")
    }

    fun UnknownError() {
        MainActivity.showSnack("Произошла ошибка, мы уже работаем над решением проблемы")
    }

    fun changeGuid(guid: String, name: String, navController: NavController) {
        this.guid = guid
        (context as MainActivity).checkNav()
        if (guid.length > 1) apikey = Crypt().encryptSYS_GUID(guid)
        DPrefs.set(DiaryPreferences.GUID, guid)
        CoroutineScope(Dispatchers.IO).launch {
            checkPdaKey(name, guid, navController)
            forceUpdatePeriods()
            navController.navigate(R.id.to_home)
        }
    }

    fun getUserType(): String? {
        val datas = jsonUtils.readJsonFileData("users.json")
        if (datas != null) {
            val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
            if (entity.data.schools.size > 0) {
                val roles = entity.data.schools[0].roles
                return if (roles.contains("participant")) "participant" else "parents"
            }
            return ""
        }
        return ""
    }

    fun getUserName(): String? {
        if (DPrefs.contains("name")) return DPrefs.get("name")
        val datas = jsonUtils.readJsonFileData("users.json")
        if (datas != null) {
            val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
            entity.data.name?.let { DPrefs.set("name", it) }
            return entity.data.name
        }
        val name_surname = getShared().find { it.guid == guid }?.name
        if (name_surname != null) {
            if (name_surname.split(" ").size > 0)
                return name_surname.split(" ")[0]
        }
        return null
    }

    fun getUserSnils(): String? {
        val datas = jsonUtils.readJsonFileData("users.json")
        if (datas != null) {
            val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
            return entity.data.login
        } else {
            val shared = getShared()
            if (shared != null) {
                return shared.find { it.guid == guid }?.snils
            }
        }
        return null
    }

    suspend fun login(cookies: String): UserData? {
        var X1: String?
        val split = cookies.split("; ".toRegex()).toTypedArray()
        X1 = split.find { it.startsWith("X1_SSO=") }?.replace("X1_SSO=", "")
        if (X1 == null) {
            CookieManager.getInstance().removeAllCookies(null)
            return null
        } else sid = X1
        val req = UserInfoRequest(Crypt().encryptSYS_GUID(sid), sid)
        try {
            val user = endpoints.getUserInfo(req) ?: return null
            if (!user.success) return null
            jsonUtils.createJsonFileData("users.json", gson.toJson(user))
            return user.data
        } catch (e: Exception) {
            processException(e)
        }
        return null
    }

    fun getSavedUsers(): UserData? {
        val datas = jsonUtils.readJsonFileData("users.json", true)
        if (datas != null && datas.length > 100) {
            val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
            return entity.data
        }
        return null
    }

//    suspend fun getUserInfo(silent: Boolean = false): UserData? {
//        val datas = jsonUtils.readJsonFileData("users.json")
//        if (datas != null && datas.length > 100) {
//            val entity = gson.fromJson(datas.toString(), UserInfo::class.java)
//            return entity.data
//        }
//        val i = Intent(context, LoginActivity::class.java)
//        val cookies = CookieManager.getInstance().getCookie("one.pskovedu.ru")
//        if (cookies == null) {
//            Log.d("redirect", "no-cookies")
//            if (!silent) context.startActivity(i)
//            return null
//        } else {
//            var X1: String?
//            val split = cookies.split("; ".toRegex()).toTypedArray()
//            X1 = split.find { it.startsWith("X1_SSO=") }?.replace("X1_SSO=", "")
//            if (X1 == null) {
//                CookieManager.getInstance().removeAllCookies(null)
//                Log.d("redirect", "x1-empty")
//                if (!silent) context.startActivity(i)
//                return null
//            } else sid = X1
//        }
//        val req = UserInfoRequest(Crypt().encryptSYS_GUID(sid), sid)
//        try {
//            val user = endpoints.getUserInfo(req) ?: return null
//            if (!user.success) return null
//            jsonUtils.createJsonFileData("users.json", gson.toJson(user))
//            return user.data
//        } catch (e: HttpException) {
//            HttpError()
//        } catch (e: ConnectException) {
//            ConnectError()
//        } catch (e: Exception) {
//            FirebaseCrashlytics.getInstance().recordException(e)
//            UnknownError()
//        }
//        return null
//    }

    fun getShared(): List<ShareUser> {
        val readed = jsonUtils.readJsonFileData("shared.json") ?: return ArrayList()
        val listType = object : TypeToken<ArrayList<ShareUser>>() {}.type
        return gson.fromJson(readed, listType)
    }

    fun addShared(shareUser: ShareUser?) {
        val current = getShared() + shareUser
        val json = gson.toJson(current)
        jsonUtils.createJsonFileData("shared.json", json)
    }

    fun addMarksCache(items: List<Item?>?) {
        jsonUtils.createJsonFileData("marks.json", gson.toJson(items))
    }

    fun getCachedPeriods(): AllPeriods? {
        val readed = jsonUtils.readJsonFileData("periods.json")
        if (readed == null || readed.length < 75) return null
        return gson.fromJson(readed, AllPeriods::class.java)
    }

    suspend fun updateMarksCache() {
        val body = ClassicBody()
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        val cached: AllPeriods = getCachedPeriods() ?: return
        val period: List<LocalDate> = get_cur_period(cached.data)
        if (period.size < 2) return
        body.from = period[0].toString()
        body.to = period[1].toString()
        try {
            val rsp = endpoints.getPeriodMarks(body) ?: return
            addMarksCache(MarksTranslator(rsp.data).items)
        } catch (_: Exception) {
        }
    }

    suspend fun getDay(date: String = ""): DiaryDay? {
        val body = ClassicBody()
        if (pdaKey === "") return null
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        body.date = date
        try {
            val day = endpoints.getDiaryDay(body)
            Log.e("aadiary", day.toString())
            return day
        } catch (e: Exception) {
            processException(e)
        }
        return null
    }

    suspend fun getAllMarks(): PeriodMarks? {
        val body = ClassicBody()
        if (pdaKey === "") return null
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        try {
            val marks = endpoints.getPeriodMarks(body) ?: return null
            return marks
        } catch (e: Exception) {
            processException(e)
        }
        return null
    }

    suspend fun getPeriodMarks(from: String, to: String): Pair<PeriodMarks?, Boolean> {
        val body = ClassicBody()
        if (pdaKey === "") return null to false
        body.guid = guid
        body.apikey = apikey
        body.from = from
        body.pdakey = pdaKey
        body.to = to
        try {
            val marks = endpoints.getPeriodMarks(body) ?: return null to false
            val formatter = DateTimeFormat.forPattern("dd.MM.yyyy")
            val dates = ArrayList<LocalDate>()
            dates.add(LocalDate.parse(from, formatter))
            dates.add(LocalDate.parse(to, formatter))
            return marks to (getCachedPeriods()?.let { get_cur_period(it.data) } == dates)
        } catch (e: ConnectException) {
            ConnectError()
        } catch (_: Exception) {
        }
        return null to false
    }

    suspend fun getAssistantTips(): AssistantTips? {
        val body = AssistantTipsRequestBody()
        if (pdaKey === "") return null
        if (getUserSnils() === "") return null
        body.login = getUserSnils()!!
        body.apikey = Crypt().encryptSYS_GUID(body.login!!)
        try {
            return endpoints.getAssistantTips(body) ?: return null
        } catch (e: ConnectException) {
            ConnectError()
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun getSchoolNews(): News? {
        val body = ClassicBody()
        try {
            return endpoints.getSchoolNews(body) ?: return null
        } catch (e: ConnectException) {
            ConnectError()
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun getEduNews(): News? {
        val body = ClassicBody()
        try {
            return endpoints.getEduNews(body) ?: return null
        } catch (e: ConnectException) {
            ConnectError()
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun getDopPrograms(bias: Int = 1, count: Int = 5): List<DopProgramData>? {
        val body = ClassicBody()
        body.areaname = "г.Псков"
        body.bias = bias
        body.blocksize = count
        try {
            return endpoints.getDopPrograms(body)?.data ?: return null
        } catch (e: ConnectException) {
            ConnectError()
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun getPeriods(): AllPeriods? {
        getCachedPeriods()?.let { return it }
        val body = ClassicBody()
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        try {
            val periods = endpoints.getPeriods(body) ?: return null
            jsonUtils.createJsonFileData("periods.json", gson.toJson(periods))
            return periods
        } catch (_: Exception) {
        }
        return null
    }

    private suspend fun forceUpdatePeriods(): AllPeriods? {
        val body = ClassicBody()
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        try {
            val periods = endpoints.getPeriods(body) ?: return null
            jsonUtils.createJsonFileData("periods.json", gson.toJson(periods))
            return periods
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun getItogMarks(): Periods? {
        val body = ClassicBody()
        body.guid = guid
        body.apikey = apikey
        body.pdakey = pdaKey
        try {
            val periods = endpoints.getItogMarks(body) ?: return null
            return periods
        } catch (_: Exception) {
        }
        return null
    }

    suspend fun checkPdaKey(name: String, guid: String, navController: NavController?) {
        val pdBody = PDBody()
        pdBody.sysguid = guid
        try {
            val pda = endpoints.getPda(pdBody) ?: return
            if (pda.status == "not found") {
                setPdaKey(name, guid, navController)
            } else {
                if (pda.pdakey === "") Toaster.toast("Ошибка потверждения PDA")
                else {
                    pdaKey = pda.pdakey
                    DPrefs.set(DiaryPreferences.PDA, pda.pdakey)
                    DPrefs.set(DiaryPreferences.PDA_GUID, guid)
                    try {
                        navController?.navigate(navController.currentDestination!!.id)
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            processException(e)
        }
    }

    suspend fun setPdaKey(name: String, guid: String, navController: NavController?) {
        val pdBody = PDBody()
        pdBody.name = name
        pdBody.sysguid = guid
        val localPda: String = genPdaKey()
        pdBody.pdakey = localPda
        try {
            val pda = endpoints.setPda(pdBody) ?: return
            if (pda.status == "error") Toaster.toast("Ошибка PDA №2")
            else if (pda.status == "ok") {
                DPrefs.set(DiaryPreferences.PDA, pda.pdakey)
                DPrefs.set(DiaryPreferences.PDA_GUID, guid)
                try {
                    navController?.navigate(navController.currentDestination!!.id)
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            processException(e)
        }
    }

}