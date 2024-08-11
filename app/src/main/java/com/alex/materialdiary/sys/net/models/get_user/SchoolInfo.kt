package com.alex.materialdiary.sys.net.models.get_user

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SchoolInfo (
    @SerializedName("SYS_GUID")
    var sysGuid: String? = null,

    @SerializedName("ID")
    var id: Int? = null,

    @SerializedName("NAME")
    var name: String? = null,

    @SerializedName("SHORT_NAME")
    var shortName: String? = null
) : Parcelable