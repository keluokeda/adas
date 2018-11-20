package com.ke.adas.entity

/**
 * 报警灵敏度等级
 */
data class DeviceSensitivityLevel(
    /**
     * 车道偏离灵敏度
     */
    val ldw: Int,
    /**
     * 前车防碰撞灵敏度
     */
    val fcw: Int,
    /**
     * 行人识别灵敏度
     */
    val pcw: Int
)