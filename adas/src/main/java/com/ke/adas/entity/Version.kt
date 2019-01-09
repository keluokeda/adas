package com.ke.adas.entity

data class Version(
    /**
     * 硬件版本
     */
    val hardwareVersion: String,
    /**
     * 软件版本
     */
    val softwareVersion: String,
    /**
     * 操作系统版本
     */
    val operatingSystemVersion: String,
    /**
     * obd版本
     */
    val obdVersion: String,
    /**
     * buzzer版本
     */
    val buzzerVersion: String,
    /**
     * gps版本
     */
    val gpsVersion: String
)