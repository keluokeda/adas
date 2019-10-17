package com.ke.adas.entity

enum class CondenseLevel(val description:String,val level:Int) {
    ExtraLow("64kb/s",1),
    Low("256kb/s",2),
    Middle("512kb/s",3),
    Height("1MB/s",4),
    ExtraHeight("2MB/s",5)

}