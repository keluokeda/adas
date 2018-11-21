package com.ke.adas.entity

import android.support.annotation.IntRange

data class AlarmConfiguration(
    @IntRange(from = 0, to = 5)
    val p0: Int,
    @IntRange(from = 0, to = 5)
    val p1: Int,
    @IntRange(from = 0, to = 1)
    val p2: Int,
    @IntRange(from = 0, to = 1)
    val p3: Int
)