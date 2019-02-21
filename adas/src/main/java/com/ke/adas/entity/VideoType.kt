package com.ke.adas.entity

import android.os.Parcel
import android.os.Parcelable

enum class VideoType(val type: Int) : Parcelable {
    All(0),
    Collision(1),
    Alarm(2);

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }


    companion object CREATOR : Parcelable.Creator<VideoType> {
        override fun createFromParcel(parcel: Parcel): VideoType {
            return when (parcel.readInt()) {
                0 -> All
                1 -> Collision
                2 -> Alarm
                else -> Alarm
            }
        }

        override fun newArray(size: Int): Array<VideoType?> {
            return arrayOfNulls(size)
        }
    }
}