package com.ke.adas

import android.content.Context
import android.net.wifi.WifiManager
import bean.BLEDevice
import bean.DVRInfo
import bean.DrawShape
import com.example.vispect_blesdk.DeviceHelper
import com.ke.adas.entity.*
import com.ke.adas.exception.DeviceException
import interf.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DeviceService(
    private val logger: Logger
) {

    private val checkUpdateService: CheckUpdateService

    init {
        val baseUrl = "http://server.vispect.net:8080/"

        val client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

        checkUpdateService = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .baseUrl(baseUrl)
            .build()
            .create(CheckUpdateService::class.java)
    }

    private val deviceHelper = DeviceHelper()

    private val connectStateSubject = PublishSubject.create<Boolean>()

    private val adasEventSubject = PublishSubject.create<Int>()


    private lateinit var context: Context

    /**
     * 初始化
     */
    fun init(context: Context, appKey: String): Observable<Boolean> {
        this.context = context
        val o = Observable.create<Boolean> {
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

        deviceHelper.setDeviceConnectStateListener(object : OnDeviceConnectionStateChange {
            override fun onConnectionStateChange(p0: Int) {
                logger.loggerMessage("设备连接状态发生变化 $p0")
                connectStateSubject.onNext(p0 == 1)
            }

            override fun onSocketStateChange(p0: Int) {
                logger.loggerMessage("设备socket状态发生变化 $p0")
            }

        })

        //开启工程模式
        deviceHelper.openEngineeringModel()

        deviceHelper.openADASEventListener {
            adasEventSubject.onNext(it)
        }



        return o


    }

    /**
     * 设备是否连接
     */
    fun isConnectedDevice(): Boolean {
        return deviceHelper.isConnectedDevice
    }


    /**
     * 设备wifi是否连上
     */
    fun isWifiConnected(): Observable<Boolean> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        val ssid = wifiInfo?.ssid?.replace("\"", "")

        logger.loggerMessage("当前连接的Wi-Fi名称 $ssid")

        return getDeviceWifiInfo().map { it.first }
            .map { it == ssid }

    }

    /**
     * 观察设备连接状态
     */
    fun observeConnectState(): Observable<Boolean> = connectStateSubject


    /**
     * 设置音量大小
     */
    fun setDeviceVoice(voice: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.setVoice(voice, getSetDeviceInfoCallback(it))
        }
    }


    /**
     * 扫描设备
     */
    fun scanDevice(): Observable<Device> {
        return Observable.create<Device> {
            logger.loggerMessage("开始扫描设备")
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
            .doOnDispose {
                logger.loggerMessage("停止扫描设备")
                deviceHelper.stopScanDevice()
            }

    }

    /**
     * 登录设备
     */
    fun loginDevice(device: Device): Observable<Boolean> {
        return Observable.create<Boolean> {
            logger.loggerMessage("开始登录设备")
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


    fun openDeviceWifi(): Observable<Pair<String, String>> {
        return Observable.create {
            logger.loggerMessage("打开设备wifi")
            deviceHelper.openDeviceDownloadDVRMode(getOnWifiOpenListener(it))
        }
    }

    fun closeDeviceWifi() {
        logger.loggerMessage("关闭设备Wi-Fi")
        deviceHelper.closeDeviceDownloadDVRMode()
    }


    /**
     * 打开设备实况模式 1
     */
    fun openDeviceRealViewMode(): Observable<kotlin.Pair<String, String>> {
        return Observable.create<kotlin.Pair<String, String>> {
            logger.loggerMessage("打开设备实况模式")
            deviceHelper.openDeviceRealViewMode(getOnWifiOpenListener(it))
        }

            .doOnDispose {
                logger.loggerMessage("关闭设备实况模式")
                deviceHelper.closeDeviceRealViewMode()
            }
    }

    /**
     * 关闭实况模式
     */
    fun closeDeviceRealViewMode() {
        deviceHelper.closeDeviceRealViewMode()
    }


    /**
     * 初始化实况模式 3
     */
    fun initRealView(): Observable<RealViewEntity> {
        return Observable.create<RealViewEntity> {
            logger.loggerMessage("初始化实况模式")
            deviceHelper.initRealView(
                getRealViewCallback(it)
            )
        }.doOnDispose {
            logger.loggerMessage("注销实况模式")
            deviceHelper.initRealView(null)
        }
    }

    /**
     * 开启实况模式 2
     */
    fun startRealView(): Observable<Boolean> {

        return Observable.create<Boolean> {
            logger.loggerMessage("开始实况模式")
            deviceHelper.startRealView()
            it.onNext(true)
            //调用了onComplete 就不会走doOnDispose
//            it.onComplete()
        }

            .doOnDispose {
                logger.loggerMessage("关闭实况模式")
                deviceHelper.stopRealView()
            }

    }


    /**
     * 获取视频列表
     */
    fun getVideoList(pageNo: Int, pageSize: Int = 100): Observable<List<DeviceVideo>> {
        return Observable.create {
            deviceHelper.getDVRLists(pageNo * pageSize, pageSize, object : DrivingVideoOperationListener {
                override fun onLockOrUnlockResult(p0: Boolean) {

                }

                override fun onGetVideoList(p0: ArrayList<*>) {
                    logger.loggerMessage("onGetVideoList $pageNo $p0")


                    val list = p0.map { any ->
                        any as DVRInfo
                    }
                        .map { info ->
                            info.toDeviceVideo()
                        }
                    it.onNext(list)
                }

                override fun onLast() {

                    logger.loggerMessage("getVideoList $pageNo onLast")
                    it.onNext(emptyList())
                }

            })

        }
    }


    /**
     * 下载视频
     */
    fun downloadVideo(videoName: String): Observable<DownloadVideoInfo> {
        return Observable.create {
            logger.loggerMessage("开始下载视频 视频名称 $videoName")
            deviceHelper.downloadDVR(videoName, object : ProgressCallback {
                override fun onDone(p0: String, p1: String) {
                    val downloadVideoInfo = DownloadVideoInfo(videoName = videoName, filePath = p0, fileMd5 = p1)

                    logger.loggerMessage("下载视频完成 $downloadVideoInfo")
                    it.onNext(downloadVideoInfo)
                    it.onComplete()
                }

                override fun onProgressChange(p0: Long) {
                    it.onNext(DownloadVideoInfo(videoName = videoName, progress = p0))
                }

                override fun onErro(p0: Int) {
                    logger.loggerMessage("下载视频失败 视频名称 $videoName")
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 获取下载视频的目录
     */
    fun getDownloadVideoPath(): String {
        return deviceHelper.downloadDir
    }


    /**
     * 设置设备wifi密码
     */
    fun setDeviceWifiPassword(password: String): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.setDeviceWifiPassword(password, getSetDeviceInfoCallback(it))
        }
    }

    /**
     * 获取设备wifi信息
     */
    fun getDeviceWifiInfo(): Observable<Pair<String, String>> {
        return Observable.create<Pair<String, String>> {
            deviceHelper.getDeviceWifiNameAndPassword(getOnWifiOpenListener(it))
        }
    }

    /**
     * 设置报警灵敏度
     */
    fun setDeviceSensitivityLevel(deviceSensitivityLevel: DeviceSensitivityLevel): Observable<Boolean> {
        return Observable.create<Boolean> {
            logger.loggerMessage("开始设置报警灵敏度 $deviceSensitivityLevel")
            deviceHelper.setADASSensitivityLevel(
                deviceSensitivityLevel.ldw,
                deviceSensitivityLevel.fcw,
                deviceSensitivityLevel.pcw,
                object : SetDeviceInfoCallback {
                    override fun onSuccess() {
                        logger.loggerMessage("设置报警灵敏度 结果 成功 $deviceSensitivityLevel")
                        it.onNext(true)
                    }

                    override fun onFail(p0: Int) {
                        logger.loggerMessage("设置报警灵敏度 结果 失败 $p0 $deviceSensitivityLevel")
                        it.onError(DeviceException(p0))
                    }

                })
        }
    }

    fun observeAdasEvent(): Observable<Int> {
        return adasEventSubject
    }

    /**
     * 获取报警灵敏度
     */
    fun getDeviceSensitivityLevel(): Observable<DeviceSensitivityLevel> {

        return Observable.create<DeviceSensitivityLevel> {
            logger.loggerMessage("开始获取报警灵敏度")
            deviceHelper.getADASSensitivityLevel(object : GetADASInfoCallback {
                override fun OnSuccess(p0: Int, p1: Int, p2: Int) {
                    logger.loggerMessage("获取报警灵敏度成功")
                    it.onNext(DeviceSensitivityLevel(p0, p1, p2))
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("获取报警灵敏度失败")
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 设置hmw时间 6-25
     */
    fun setHMWTime(value: Int): Observable<Boolean> {
        val time = if (value > 25) 25 else if (value < 8) 8 else value

        return Observable.create {
            deviceHelper.setHMWTime(time, getSetDeviceInfoCallback(it))
        }
    }

    fun getHMWTime(): Observable<Int> {
        return Observable.create {
            deviceHelper.getHMWTime(object : onGetHMWTimeCallback {
                override fun onSuccess(p0: Int) {
                    it.onNext(p0)
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 获取报警启动车速
     */
    fun getSpeedThreshold(): Observable<SpeedThreshold> {
        return Observable.create<SpeedThreshold> {
            logger.loggerMessage("开始获取报警启动车速")
            deviceHelper.GetADASSpeedThreshold(object : GetADASInfoCallback {
                override fun OnSuccess(p0: Int, p1: Int, p2: Int) {
                    logger.loggerMessage("获取报警启动车速成功")
                    it.onNext(SpeedThreshold(p0, p1, p2))
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("获取报警启动车速失败 $p0")
                    it.onError(DeviceException(p0))
                }
            })
        }
    }

    /**
     * 设置报警启动车速
     */
    fun setSpeedThreshold(speedThreshold: SpeedThreshold): Observable<Boolean> {
        return Observable.create<Boolean> {
            logger.loggerMessage("开始设置报警启动车速 $speedThreshold")
            deviceHelper.setADASSpeedThreshold(
                speedThreshold.ldw,
                speedThreshold.fcw,
                speedThreshold.pcw,
                object : SetDeviceInfoCallback {
                    override fun onSuccess() {
                        logger.loggerMessage("设置报警启动车速成功")

                        it.onNext(true)
                    }

                    override fun onFail(p0: Int) {
                        logger.loggerMessage("设置报警启动车速失败 $p0")

                        it.onError(DeviceException(p0))
                    }

                })
        }
    }

    /**
     * 获取设备报警配置
     */
    fun getAlarmConfiguration(): Observable<AlarmConfiguration> {
        return Observable.create<AlarmConfiguration> {
            logger.loggerMessage("开始获取设备报警配置")
            deviceHelper.getADASAlarmConfiguration(object : onGetADASAlarmConfigurationCallback {
                override fun onSuccess(p0: Int, p1: Int, p2: Int, p3: Int) {
                    logger.loggerMessage("获取设备报警配置成功")
                    it.onNext(AlarmConfiguration(p0, p1, p2, p3))
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("获取设备报警配置失败 $p0")

                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 设置报警配置
     */
    fun setAlarmConfiguration(alarmConfiguration: AlarmConfiguration): Observable<Boolean> {
        return Observable.create<Boolean> {
            logger.loggerMessage("开始设置设备报警配置")
            deviceHelper.ADASAlarmConfiguration(
                alarmConfiguration.p0,
                alarmConfiguration.p1,
                alarmConfiguration.p2,
                alarmConfiguration.p3,
                getSetDeviceInfoCallback(it)
            )
        }
    }

    fun getVoice(): Observable<Int> {
        return Observable.create<Int> {
            deviceHelper.getVoice(object : onGetVoiceCallback {
                override fun onSuccess(p0: Int) {
                    it.onNext(p0)
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    fun setVoice(voice: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.setVoice(voice, getSetDeviceInfoCallback(it))
        }
    }

    fun getVoiceType(): Observable<Int> {

        return Observable.create<Int> {
            deviceHelper.getADASVoiceParam(object : onGetADASVoiceTypeCallback {
                override fun onSuccess(p0: Int, p1: Int, p2: Int, p3: Int) {
                    it.onNext(p0)
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    fun setVoiceType(type: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.setADASVoice(type, type, type, type, getSetDeviceInfoCallback(it))
        }
    }


    /**
     * 获取版本信息
     */
    fun getVersion(): Observable<Version> {
        return Observable.create {
            deviceHelper.getDeviceVersion(object : GetBleInfoVersionCallback {
                override fun onSuccess(p0: String, p1: String, p2: String, p3: String, p4: String, p5: String) {
                    it.onNext(Version(p0, p1, p2, p3, p4, p5))
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }


    /**
     * 获取更新信息
     */
    fun getUpdateMessage(
        updateType: UpdateType,
        obdVersion: String,
        buzzerVersion: String
    ): Observable<CheckUpdateResult> {


        return checkUpdateService.checkUpdate(
            updateType.type,
            obdVersion = obdVersion,
            buzzerVersion = buzzerVersion
        )
            .subscribeOn(Schedulers.io())
            .onErrorReturn { throwable ->

                logger.loggerMessage(throwable.message ?: "出错了")
                return@onErrorReturn CheckUpdateResult(result = 110, message = null)
            }

    }


    /**
     * 更新设备app
     */
    fun updateDeviceApp(path: String): Observable<Int> {
        return Observable.create {

            deviceHelper.deviceAPKUpdate(path, createProgressCallback(it))
        }

    }


    /**
     * 开启设备升级模式
     */
    fun openDeviceUpdateMode(): Observable<Pair<String, String>> {

        return Observable.create<Pair<String, String>> {
            deviceHelper.openDeviceUpdateMode(getOnWifiOpenListener(it))
        }.doOnDispose {
            deviceHelper.closeDeviceUpdateMode()
        }.doOnComplete {
            deviceHelper.closeDeviceUpdateMode()
        }
    }


    /**
     * 更新设备obd
     */
    fun updateDeviceObd(path: String): Observable<Int> {
        return Observable.create {
            deviceHelper.uploadOBDUpdataFile(path, createProgressCallback(it))
        }
    }


    /**
     * 更新设备固件
     */
    fun updateHardWare(path: String): Observable<Int> {

        return Observable.create {
            deviceHelper.deviceOTAUpdate(path, createProgressCallback(it))
        }
    }

    private fun createProgressCallback(observableEmitter: ObservableEmitter<Int>): ProgressCallback {

        return object : ProgressCallback {
            override fun onDone(p0: String, p1: String) {
                observableEmitter.onComplete()
            }

            override fun onProgressChange(p0: Long) {
                observableEmitter.onNext(p0.toInt())
            }

            override fun onErro(p0: Int) {
                observableEmitter.onError(DeviceException(p0))
            }

        }
    }


    private fun getSetDeviceInfoCallback(e: ObservableEmitter<Boolean>): SetDeviceInfoCallback {
        return object : SetDeviceInfoCallback {
            override fun onSuccess() {
                e.onNext(true)
            }

            override fun onFail(p0: Int) {
                logger.loggerMessage("设置设备信息失败 $p0")
                e.onError(DeviceException(p0))
            }

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

fun DVRInfo.toDeviceVideo(): DeviceVideo {
    return DeviceVideo(
        name = name,
        lock = "0" != state,
        state = state
    )
}