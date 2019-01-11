package com.ke.adas.entity

import com.google.gson.annotations.SerializedName

data class UpdateMessage(@SerializedName("path")
               val path: String = "",
                         @SerializedName("filename")
               val fileName: String = "",
                         @SerializedName("length")
               val length: Int = 0,
                         @SerializedName("versioncode")
               val versionCode: Int = 0,
                         @SerializedName("versionname")
               val versionName: String = "",
                         @SerializedName("md5")
               val md5: String = "")