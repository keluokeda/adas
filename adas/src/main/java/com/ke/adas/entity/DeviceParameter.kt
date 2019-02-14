package com.ke.adas.entity

data class DeviceParameter(
    /**
     * 车辆宽度
     */
    val width: Int,
    /**
     * 摄像头距车中心的距离
     */
    val center: Int,

    /**
     * 摄像头距地面的距离
     */
    val height: Int,
    /**
     * 摄像头距地车头的距离
     */
    val front: Int
)