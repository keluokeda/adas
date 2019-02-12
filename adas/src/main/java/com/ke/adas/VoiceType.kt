package com.ke.adas

enum class VoiceType(val type: Int, val typeName: String) {
    None(0, "无"),
    Alarm(1, "报警音"),
    Speech(2, "语音"),


}

fun Int.toVoiceType(): VoiceType {
    return when (this) {
        1 -> VoiceType.Alarm
        2 -> VoiceType.Speech
        else -> VoiceType.None
    }
}
