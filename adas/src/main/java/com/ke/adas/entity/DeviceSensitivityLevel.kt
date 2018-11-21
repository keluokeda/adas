package com.ke.adas.entity

import android.support.annotation.IntRange

/**
 * 报警灵敏度等级
 */
data class DeviceSensitivityLevel(
    /**
     * 车道偏离灵敏度
     */
    @IntRange(from = 0, to = 3)
    val ldw: Int,
    /**
     * 前车防碰撞灵敏度
     */
    @IntRange(from = 0, to = 3)
    val fcw: Int,
    /**
     * 行人识别灵敏度
     */
    @IntRange(from = 0, to = 3)
    val pcw: Int
)