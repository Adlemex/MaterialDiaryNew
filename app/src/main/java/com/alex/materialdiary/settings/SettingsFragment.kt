package com.alex.materialdiary.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.alex.materialdiary.R
import com.alex.materialdiary.ui.bottom_sheets.ChooseColorSchemeBottomSheet
import okhttp3.logging.HttpLoggingInterceptor
import xdroid.toaster.Toaster.toast


class SettingsFragment : PreferenceFragmentCompat() {
    lateinit var okHttp: okhttp3.OkHttpClient
    lateinit var mainHandler: Handler
    var logging = HttpLoggingInterceptor()
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        mainHandler = Handler(requireContext().mainLooper)
        okHttp = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = super.onCreateView(inflater, container, savedInstanceState)
        if (result != null) {
            val lv = result.findViewById<View>(android.R.id.list)
            if (lv is ListView) {
                lv.setOnItemLongClickListener { parent, _, pos, _ ->
                    val listAdapter: ListAdapter = (parent as ListView).adapter
                    val obj: Any = listAdapter.getItem(pos)

                    println(obj.hashCode())
                    if (obj is Preference) {
                        val p = obj
                        if (p.key.equals("news")) {
                            findNavController().navigate(R.id.to_about)
                            return@setOnItemLongClickListener true
                        }
                    } /*if*/

                    return@setOnItemLongClickListener false
                }
            }
        } else {
            //The view created is not a list view!
        }
        /*
        val api = GoogleApiAvailability.getInstance()
        val resultCode =
            api.isGooglePlayServicesAvailable(requireContext())*/
        val color =
            preferenceManager.findPreference<Preference>("marks_color_scheme") as Preference
        color.setOnPreferenceClickListener {
            ChooseColorSchemeBottomSheet().show(
                requireActivity().supportFragmentManager,
                "ChooseColorSchemeBottomSheet"
            )
            true
        }
        (preferenceManager.findPreference<SwitchPreference>("dynamic_colors") as Preference)
            .setOnPreferenceChangeListener{ preference: Preference, any: Any ->
                preference.summary = "Перезапустите приложение для применения"
                true
        }
        val theme = preferenceManager.findPreference<ListPreference>("theme")
        theme?.entries = arrayOf("Темная", "Светлая", "Системная")
        theme?.entryValues = arrayOf("dark", "light", "system")
        if (theme?.value == null) theme?.setValueIndex(2)
        theme?.summary = when(theme?.value.toString()){
            "dark" -> "Темная"
            "light" -> "Светлая"
            "system" -> "Такая же как в системе"
            else -> ""
        }
        theme?.setOnPreferenceChangeListener{ preference: Preference, any: Any ->
            //theme.summary = theme.va
            preference.summary = when(any.toString()){
                "dark" -> "Темная"
                "light" -> "Светлая"
                "system" -> "Такая же как в системе"
                else -> ""
            }
            AppCompatDelegate.setDefaultNightMode(
                when (any.toString()){
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
                true
        }
        val news =
            preferenceManager.findPreference<Preference>("news") as Preference
        news.setOnPreferenceClickListener {
            try {
                val telegram =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/pskovedu_diary"))
                startActivity(telegram)
            } catch (e: Exception) {
                toast("Telegram не установлен")
            }
            true
        }

        val dev =
            preferenceManager.findPreference<Preference>("dev") as Preference
        dev.setOnPreferenceClickListener {
            findNavController().navigate(R.id.to_about)
            true
        }/*
                val about =
                    preferenceManager.findPreference<Preference>("about") as Preference
                about.setOnPreferenceClickListener {
                        findNavController().navigate(R.id.to_about)
                        true
                    }

                val kr_en =
                    preferenceManager.findPreference<SwitchPreferenceCompat>("kr") as SwitchPreferenceCompat
                val marks_en =
                    preferenceManager.findPreference<SwitchPreferenceCompat>("marks") as SwitchPreferenceCompat
                if ( Storage.FIREBASE_TOKEN == null){
                    kr_en.isEnabled = false
                    kr_en.summary = "Временно недоступно!"
                    marks_en.isEnabled = false
                    marks_en.summary = "Временно недоступно!"
                }
                if (resultCode != ConnectionResult.SUCCESS) {
                    kr_en.isEnabled = false
                    kr_en.summary = "Недоступно на вашем устройстве!"
                    marks_en.isEnabled = false
                    marks_en.summary = "Недоступно на вашем устройстве!"
                }
                kr_en.setOnPreferenceChangeListener { _, newValue ->
                    when (newValue) {
                        true -> {
                            toast("Отправляем запрос на уведомления...")
                            val request = Request.Builder()
                                .url("https://pskovedu.ml/api/notify/kr?token=" + Storage.FIREBASE_TOKEN)
                                .get()
                                .build()
                            okHttp.newCall(request).enqueue(object: okhttp3.Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    e.printStackTrace()
                                    toast("Произошла ошибка")
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    val runnable = Runnable {
                                        if (response.code() == 400){
                                            toast("Не удалось подписаться на уведомления")
                                            kr_en.isChecked = false
                                        }
                                        else if (response.code() == 200){
                                            toast("Успешно!")
                                        }
                                        else {
                                            toast("Неизвестная ошибка!")
                                            kr_en.isChecked = true
                                        }
                                    }
                                    mainHandler.post(runnable)
                                }
                            })
                        }
                        false -> {
                            toast("Отправляем запрос на уведомления...")
                            val request = Request.Builder()
                            .url("https://pskovedu.ml/api/notify/kr?token=" + Storage.FIREBASE_TOKEN)
                            .delete()
                            .build()
                            okHttp.newCall(request).enqueue(object: okhttp3.Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    e.printStackTrace()
                                    toast("Произошла ошибка")
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    val runnable = Runnable {
                                        if (response.code() == 400) {
                                            toast("Не удалось отписаться на уведомления")
                                            kr_en.isChecked = true
                                        } else if (response.code() == 200) {
                                            toast("Успешно!")
                                        } else {
                                            toast("Неизвестная ошибка!")
                                            kr_en.isChecked = true
                                        }
                                    }
                                    mainHandler.post(runnable)
                                }
                            })
                        }
                    }
                    true
                }
                marks_en.setOnPreferenceChangeListener { _, newValue ->
                    when (newValue) {
                        true -> {
                            toast("Отправляем запрос на уведомления...")
                            val request = Request.Builder()
                                .url("https://pskovedu.ml/api/notify/marks?token=" + Storage.FIREBASE_TOKEN)
                                .get()
                                .build()
                            okHttp.newCall(request).enqueue(object: okhttp3.Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    e.printStackTrace()
                                    toast("Произошла ошибка")
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    val runnable = Runnable {
                                        if (response.code() == 400){
                                            toast("Не удалось подписаться на уведомления")
                                            marks_en.isChecked = false
                                        }
                                        else if (response.code() == 200){
                                            toast("Успешно!")
                                        }
                                        else {
                                            toast("Неизвестная ошибка!")
                                            marks_en.isChecked = true
                                        }
                                    }
                                    mainHandler.post(runnable)
                                }
                            })
                        }
                        false -> {
                            toast("Отправляем запрос на уведомления...")
                            val request = Request.Builder()
                            .url("https://pskovedu.ml/api/notify/marks?token=" + Storage.FIREBASE_TOKEN)
                            .delete()
                            .build()
                            okHttp.newCall(request).enqueue(object: okhttp3.Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    e.printStackTrace()
                                    toast("Произошла ошибка")
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    val runnable = Runnable {
                                        if (response.code() == 400) {
                                            toast("Не удалось отписаться на уведомления")
                                            marks_en.isChecked = true
                                        } else if (response.code() == 200) {
                                            toast("Успешно!")
                                        } else {
                                            toast("Неизвестная ошибка!")
                                            marks_en.isChecked = true
                                        }
                                    }
                                    mainHandler.post(runnable)
                                }
                            })
                        }
                    }
                    true
                }*/
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}