package com.ke.adas.entity

import android.support.annotation.IntRange

data class SpeedThreshold(
    @IntRange(from = 0, to = 60)
    val ldw: Int,
    @IntRange(from = 0, to = 60)
    val fcw: Int,
    @IntRange(from = 0, to = 60)
    val pcw: Int
)