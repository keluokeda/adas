package com.ke.adas.entity

enum class CondenseLevel(val description:String,val level:Int) {
    ExtraLow("64kb/s",1),
    Low("256kb/s",2),
    Middle("512kb/s",3),
    Hight("1MB/s",4)

}