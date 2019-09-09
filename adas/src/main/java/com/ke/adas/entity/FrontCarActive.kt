package com.ke.adas.entity

enum class FrontCarActive(val level: Int, val introduction: String) {
    Zero(0, "不报警"),
    One(1, "4M"),
    Two(2, "5M"),
    Three(3, "6M"),
    Four(4, "7M"),
    Five(5, "8M")
}


fun Int.toFrontCarActive() = when (this) {
    1 -> FrontCarActive.One
    2 -> FrontCarActive.Two
    3 -> FrontCarActive.Three
    4 -> FrontCarActive.Four
    5 -> FrontCarActive.Five
    else -> FrontCarActive.Zero
}