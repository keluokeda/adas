package com.ke.adas.exception

import java.lang.RuntimeException

class DeviceException(errorCode: Int) : RuntimeException("设备异常 $errorCode")