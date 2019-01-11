package com.ke.adas.entity

enum class UpdateType(val type: Int) {
    Hardware(1),
    OperatingSystem(3),
    Obd(9),
    SoftWare(10)
}