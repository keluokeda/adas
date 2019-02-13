package com.ke.adas.exception

class DeviceException(errorCode: Int) : RuntimeException("设备异常 $errorCode")