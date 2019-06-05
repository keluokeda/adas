package com.ke.adas.entity

enum class OBDOperationResult {
    /**
     * 开启成功
     */
    StartSuccess,
    /**
     * 开启失败
     */
    StartFailure,

    /**
     * 左边破解成功
     */
    LeftCrackSuccess,

    /**
     * 右边破解成功
     */
    RightCrackSuccess,

    /**
     * 左边破解失败
     */
    LeftCrackFailure,

    /**
     * 右边破解失败
     */
    RightCrackFailure
}