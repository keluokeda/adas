package com.ke.adas.entity

import androidx.annotation.IntRange

data class AlarmConfiguration(
    /**
     * 溜车
     */
    @IntRange(from = 0, to = 5)
    val rolling: Int,
    /**
     * 前车启动
     */
    @IntRange(from = 0, to = 5)
    val frontCarActive: Int,
    @IntRange(from = 0, to = 1)
    val p2: Int,
    @IntRange(from = 0, to = 1)
    val p3: Int
)