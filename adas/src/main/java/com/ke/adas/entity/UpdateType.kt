package com.ke.adas.entity

import android.os.Parcel
import android.os.Parcelable

enum class UpdateType(val type: Int) : Parcelable {
    Hardware(1),
    Obd(9),
    App(10);

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
    }

    override fun describeContents(): Int {
        return 0
    }


    companion object CREATOR : Parcelable.Creator<UpdateType> {
        override fun createFromParcel(parcel: Parcel): UpdateType {
            return when (parcel.readInt()) {
                1 -> Hardware
                9 -> Obd
                10 -> App
                else -> Hardware
            }
        }

        override fun newArray(size: Int): Array<UpdateType?> {
            return arrayOfNulls(size)
        }
    }
}