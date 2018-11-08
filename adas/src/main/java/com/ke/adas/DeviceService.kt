package com.ke.adas

import android.content.Context
import bean.BLEDevice
import com.example.vispect_blesdk.DeviceHelper
import com.ke.adas.entity.Device
import com.ke.adas.exception.DeviceException
import interf.BleLoginListener
import interf.OnScanDeviceLisetener
import interf.Oninit
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
@Suppress("unused")
class DeviceService(
    private val logger: Logger
) {


    private val deviceHelper = DeviceHelper()

    private val deviceScheduler = Schedulers.computation()
    /**
     * 初始化
     */
    fun init(context: Context, appKey: String): Observable<Boolean> {
        return Observable.create {
            deviceHelper.initSDK(context.applicationContext, appKey, object : Oninit {
                override fun onSuccess() {
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    it.onNext(false)
                    it.onComplete()
                }

            })
        }


    }

    /**
     * 设备是否连接
     */
    fun isConnectedDevice(): Boolean {
        return deviceHelper.isConnectedDevice
    }

    /**
     * 扫描设备
     */
    fun scanDevice(): Observable<Device> {
        return Observable.create<Device> {
            deviceHelper.startScanDevice(object : OnScanDeviceLisetener {
                override fun onSuccess() {
                    logger.loggerMessage("扫描设备成功")
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("扫描设备出错")
                    it.onError(DeviceException(p0))
                }

                override fun onFindDevice(p0: BLEDevice) {
                    val device = Device(p0)
                    logger.loggerMessage("扫描到设备 $device")
                    it.onNext(device)
                }

            })
        }
            .distinct { it.address }
            .subscribeOn(deviceScheduler)
            .unsubscribeOn(deviceScheduler)
            .doOnDispose {
                deviceHelper.stopScanDevice()
            }

    }

    /**
     * 登录设备
     */
    fun loginDevice(device: Device): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.loginDevice(device.bleDevice, object : BleLoginListener {
                override fun onSuccess() {
                    logger.loggerMessage("登录设备成功")
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onPassworderro() {
                    logger.loggerMessage("登录设备密码错误")
                    it.onError(DeviceException(DeviceErrorCode.BLE_LOGIN_PASSWORD_INCORRECT))
                }

                override fun onNotService() {
                    logger.loggerMessage("登录设备找不到服务")
                    it.onError(DeviceException(DeviceErrorCode.DEVICE_NOT_FIND_SERVICE))

                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("登录设备失败 $p0")
                    it.onError(DeviceException(p0))

                }

            })
        }
            .subscribeOn(deviceScheduler)

    }


    /**
     * 断开设备
     */
    fun disconnectDevice() {
        deviceHelper.disconnectDevice()
    }
}