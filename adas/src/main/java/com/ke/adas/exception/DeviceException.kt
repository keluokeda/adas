package com.ke.adas.exception

class DeviceException(errorCode: Int) : RuntimeException(message = "设备异常 $errorCode")