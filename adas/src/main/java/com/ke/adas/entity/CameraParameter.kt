package com.ke.adas.entity

data class CameraParameter(
    /**
     * 设备距中心点的位置
     */
    val center: Int,
    /**
     * 设备距地面的高度
     */
    val height: Int,
    /**
     * 设备和车头之间的距离
     */
    val front: Int
)