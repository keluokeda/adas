package com.ke.adas

import android.content.Context
import android.support.v4.util.Pair
import bean.BLEDevice
import bean.DrawShape
import com.example.vispect_blesdk.DeviceHelper
import com.ke.adas.entity.Device
import com.ke.adas.entity.RealViewEntity
import com.ke.adas.exception.DeviceException
import interf.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import java.util.*

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
                    logger.loggerMessage("初始化成功")
                    it.onNext(true)
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("初始化失败")
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


    private fun getRealViewCallback(e: ObservableEmitter<RealViewEntity>): RealViewCallback {
        return object : RealViewCallback {
            override fun onGetPixData(bytes: ByteArray, i: Int) {
                e.onNext(RealViewEntity(bytes, i))
            }

            override fun onGetADASRecInfo(arrayList: ArrayList<*>) {
                val list = arrayList.map {
                    it as DrawShape
                }

                e.onNext(RealViewEntity(list))
            }

            override fun onGetSideRecInfo(arrayList: ArrayList<*>) {

            }

            override fun onGetDSMAlarmInfo(map: Map<*, *>) {

            }

            override fun onGetSensorData(x: Float, y: Float, z: Float) {
                e.onNext(RealViewEntity(x, y, z))
            }

            override fun onGetSpeedData(s: String) {
                e.onNext(RealViewEntity(s))
            }

            override fun onErro(i: Int) {
                e.onError(DeviceException(i))
            }
        }
    }


    /**
     * 开始设备实况模式
     */
    fun openDeviceRealViewMode(): Observable<kotlin.Pair<String, String>> {
        return Observable.create<kotlin.Pair<String, String>> {
            deviceHelper.openDeviceRealViewMode(getOnWifiOpenListener(it))
        }
            .subscribeOn(deviceScheduler)
            .unsubscribeOn(deviceScheduler)
            .doOnDispose {
                deviceHelper.closeDeviceRealViewMode()
            }
    }


    /**
     * 初始化实况模式
     */
    fun initRealView(): Observable<RealViewEntity> {
//       deviceHelper.initRealView()
        return Observable.create<RealViewEntity> {
            getRealViewCallback(it)
        }
            .subscribeOn(deviceScheduler)
            .unsubscribeOn(deviceScheduler)
            .doOnDispose {
                deviceHelper.initRealView(null)
            }
    }

    /**
     * 开启实况模式
     */
    fun startRealView(): Observable<Boolean> {

        return Observable.create<Boolean> {
            deviceHelper.startRealView()
            it.onNext(true)
            it.onComplete()
        }
            .subscribeOn(deviceScheduler)
            .unsubscribeOn(deviceScheduler)
            .doOnDispose {
                deviceHelper.stopRealView()
            }

    }


    private fun getOnWifiOpenListener(e: ObservableEmitter<kotlin.Pair<String, String>>): OnWifiOpenListener {
        return object : OnWifiOpenListener {
            override fun onSuccess(s: String, s1: String) {
                logger.loggerMessage("get wifi account = $s password = $s1")
                e.onNext(s to s1)
            }

            override fun onFail(i: Int) {
                //onNext不允许为null
                logger.loggerMessage("open wifi error = $i")
                e.onError(DeviceException(i))
            }

            override fun onGetSanResult(s: String) {

                logger.loggerMessage("scan result $s")

            }
        }
    }

}