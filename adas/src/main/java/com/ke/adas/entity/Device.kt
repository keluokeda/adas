package com.ke.adas.entity

import bean.BLEDevice

data class Device(
   internal  val bleDevice: BLEDevice
) {
    val name = bleDevice.bluetoothDevice.name
    val address = bleDevice.bluetoothDevice.address


    override fun toString(): String {
        return "Device(name='$name', address='$address')"
    }


}