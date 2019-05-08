package com.ke.adas.entity

import com.google.gson.annotations.SerializedName

data class CheckUpdateResult(
    @SerializedName("result")
    val result: Int = 0,
    @SerializedName("msg")
    val message: UpdateMessage?
) {
    val needUpdate: Boolean
        get() {
            return message != null
        }
}