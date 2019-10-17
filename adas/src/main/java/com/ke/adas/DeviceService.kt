package com.ke.adas

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.net.wifi.WifiManager
import bean.*
import com.example.vispect_blesdk.DeviceHelper
import com.ke.adas.entity.*
import com.ke.adas.exception.DeviceException
import interf.*
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused")
class DeviceService(
    private val logger: Logger
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val timeFormat = SimpleDateFormat("HH:mm:ss")

    private val pathFormat = SimpleDateFormat("yyyyMMddHHmmss")

    private lateinit var downloadedVideoRepository: DownloadedVideoRepository
    private val checkUpdateService: CheckUpdateService


//    private val loadVideoListSubject: Subject<Pair<Int, DeviceVideo>> = PublishSubject.create()

    private val allVideoListSubject: Subject<List<DeviceVideo>> = BehaviorSubject.create()
    private val allVideoList = mutableListOf<DeviceVideo>()

    private val collisionVideoListSubject: Subject<List<DeviceVideo>> = BehaviorSubject.create()
    private val collisionVideoList = mutableListOf<DeviceVideo>()

    private val alarmVideoListSubject: Subject<List<DeviceVideo>> = BehaviorSubject.create()
    private val alarmVideoList = mutableListOf<DeviceVideo>()


    private var allDownloadedVideoList = emptyList<DownloadedVideo>()

    private var collisionDownloadedVideoList = emptyList<DownloadedVideo>()

    private var alarmDownloadedVideoList = emptyList<DownloadedVideo>()

    private val compositeDisposable = CompositeDisposable()


    val allVideoListObservable: Observable<List<DeviceVideo>> = allVideoListSubject

    val collisionVideoListObservable: Observable<List<DeviceVideo>> = collisionVideoListSubject

    val alarmVideoListObservable: Observable<List<DeviceVideo>> = alarmVideoListSubject

    val allDownloadVideoListObservable: Observable<List<DownloadedVideo>>
        get() = downloadedVideoRepository.allVideoListObservable


    val collisionDownloadVideoListObservable: Observable<List<DownloadedVideo>>
        get() = downloadedVideoRepository.collisionVideoListObservable


    val alarmDownloadVideoListObservable: Observable<List<DownloadedVideo>>
        get() = downloadedVideoRepository.alarmVideoListObservable


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

    private val connectStateSubject = BehaviorSubject.create<Boolean>()

    private val adasEventSubject = PublishSubject.create<Int>()


    private lateinit var context: Context

    /**
     * 初始化
     */
    fun init(context: Context, appKey: String): Observable<Boolean> {
        this.context = context.applicationContext




        downloadedVideoRepository = DownloadedVideoRepository(this.context)



        downloadedVideoRepository.allVideoListObservable.subscribe {
            allDownloadedVideoList = it
        }.addTo(compositeDisposable)

        downloadedVideoRepository.collisionVideoListObservable.subscribe {
            collisionDownloadedVideoList = it
        }
            .addTo(compositeDisposable)

        downloadedVideoRepository.alarmVideoListObservable.subscribe {
            alarmDownloadedVideoList = it
        }.addTo(compositeDisposable)


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

//        deviceHelper.getADASWarringVideos()


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
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
     * 停止扫描设备
     */
    fun stopScanDevice() {
        deviceHelper.stopScanDevice()
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

                    if (!it.isDisposed) {
                        it.onError(DeviceException(DeviceErrorCode.BLE_LOGIN_PASSWORD_INCORRECT))
                    }
                }

                override fun onNotService() {
                    logger.loggerMessage("登录设备找不到服务")
                    if (!it.isDisposed) {
                        it.onError(DeviceException(DeviceErrorCode.DEVICE_NOT_FIND_SERVICE))
                    }

                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("登录设备失败 $p0")
                    if (!it.isDisposed) {
                        it.onError(DeviceException(p0))
                    }

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
            override fun onGetSideRecInfo(p0: ArrayList<SideAlarmEvent>?) {

            }

            override fun onGetDSMAlarmInfo(p0: MutableMap<Int, MutableList<Point>>?) {
            }

            override fun onGetPixData(bytes: ByteArray, i: Int) {
                e.onNext(RealViewEntity(bytes, i))
            }

            override fun onGetADASRecInfo(arrayList: ArrayList<DrawShape>) {
                val list = arrayList.map {
                    it
                }

                e.onNext(RealViewEntity(list))
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


    fun openDeviceDownloadDVRMode(): Observable<Pair<String, String>> {
        return Observable.create {
            logger.loggerMessage("打开设备wifi")
            deviceHelper.openDeviceDownloadDVRMode(getOnWifiOpenListener(it))
        }
    }

    fun closeDeviceDownloadDVRMode() {
        logger.loggerMessage("关闭设备Wi-Fi")
        deviceHelper.closeDeviceDownloadDVRMode()
    }


    /**
     * 打开设备实况模式 1
     */
    fun openDeviceRealViewMode(): Observable<Pair<String, String>> {
        return Observable.create<Pair<String, String>> {
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
     * 加载视频列表
     */
    fun loadVideoList(pageNo: Int, videoType: VideoType): Observable<Boolean> {


        return Observable.create {


            logger.loggerMessage("开始加载视频 pageNo = $pageNo videoType = $videoType")

            val pageSize = 20



            when (videoType) {

                VideoType.All -> deviceHelper.getDVRLists(
                    pageNo * pageSize,
                    pageSize,
                    object : DrivingVideoOperationListener {
                        override fun onLockOrUnlockResult(p0: Boolean) {

                        }

                        override fun onGetVideoList(p0: ArrayList<DVRInfo>) {
                            val list = p0.map { any ->
                                any
                            }
                                .map { info ->
                                    convertToDeviceVideo(info, videoType)
                                }

                            addVideoListToList(list, videoType, pageNo)
                            logger.loggerMessage("成功获取到视频 ${list.size} pageNo = $pageNo videoType = $videoType")

                            it.onNext(true)
                        }

                        override fun onLast() {
                            logger.loggerMessage("没有发现任何视频数据 pageNo = $pageNo videoType = $videoType")

                            addVideoListToList(emptyList(), videoType, pageNo)
                            it.onNext(false)
                        }

                    }
                )

                VideoType.Collision -> deviceHelper.getCollisionVideos(
                    pageNo * pageSize,
                    pageSize,
                    object : CollosionVideoOperationListener {
                        override fun onLockOrUnlockResult(p0: Boolean) {
                        }

                        override fun onGetVideoList(p0: ArrayList<DVRInfo>) {
                            val list = p0.map { any ->
                                any
                            }
                                .map { info ->
                                    convertToDeviceVideo(info, videoType)
                                }

                            addVideoListToList(list, videoType, pageNo)
                            logger.loggerMessage("成功获取到视频 ${list.size} pageNo = $pageNo videoType = $videoType")

                            it.onNext(true)
                        }

                        override fun onLast() {
                            logger.loggerMessage("没有发现任何视频数据 pageNo = $pageNo videoType = $videoType")

                            addVideoListToList(emptyList(), videoType, pageNo)
                            it.onNext(false)
                        }

                    }
                )
                VideoType.Alarm -> deviceHelper.getADASWarringVideos(
                    pageNo * pageSize,
                    pageSize,
                    object : WarringVideoOperationListener {
                        override fun onLockOrUnlockResult(p0: Boolean) {

                        }

                        override fun onGetVideoList(p0: ArrayList<DVRInfo>) {
                            val list =
                                p0.map { info ->
                                    convertToDeviceVideo(info, videoType)
                                }

                            addVideoListToList(list, videoType, pageNo)
                            logger.loggerMessage("成功获取到视频 ${list.size} pageNo = $pageNo videoType = $videoType")

                            it.onNext(true)
                        }

                        override fun onLast() {
                            logger.loggerMessage("没有发现任何视频数据 pageNo = $pageNo videoType = $videoType")

                            addVideoListToList(emptyList(), videoType, pageNo)
                            it.onNext(false)
                        }

                    }
                )
            }
        }


    }


    private fun convertToDeviceVideo(dvrInfo: DVRInfo, videoType: VideoType): DeviceVideo {
        val date = pathFormat.parse(dvrInfo.name)


        val videoList = when (videoType) {

            VideoType.All -> allDownloadedVideoList
            VideoType.Collision -> collisionDownloadedVideoList
            VideoType.Alarm -> alarmDownloadedVideoList
        }

        val downloadedVideo = videoList.find { it.name == dvrInfo.name }

        return DeviceVideo(
            name = dvrInfo.name,
            date = dateFormat.format(date),
            time = timeFormat.format(date),
            downloaded = downloadedVideo != null,
            path = downloadedVideo?.path,
            progress = 0,
            downloading = false
        )
    }

    private fun addVideoListToList(list: List<DeviceVideo>, videoType: VideoType, pageNo: Int) {
        when (videoType) {

            VideoType.All -> {
                if (pageNo == 0) {
                    allVideoList.clear()
                }
                allVideoList.addAll(list)
                allVideoListSubject.onNext(allVideoList)
            }
            VideoType.Collision -> {
                if (pageNo == 0) {
                    collisionVideoList.clear()
                }
                collisionVideoList.addAll(list)
                collisionVideoListSubject.onNext(collisionVideoList)
            }
            VideoType.Alarm -> {
                if (pageNo == 0) {
                    alarmVideoList.clear()
                }
                alarmVideoList.addAll(list)
                alarmVideoListSubject.onNext(alarmVideoList)
            }
        }


    }

//    /**
//     * 获取视频列表
//     */
//    fun getVideoList(pageNo: Int, pageSize: Int = 100, videoType: VideoType): Observable<List<DeviceVideo>> {
//
//        return Observable.create {
//
//            when (videoType) {
//
//                VideoType.All -> deviceHelper.getDVRLists(
//                    pageNo * pageSize,
//                    pageSize,
//                    createDrivingVideoOperationListener(pageNo, it)
//                )
//
//                VideoType.Collision -> deviceHelper.getCollisionVideos(
//                    pageNo * pageSize,
//                    pageSize,
//                    createDrivingVideoOperationListener(pageNo, it)
//                )
//                VideoType.Alarm -> deviceHelper.getADASWarringVideos(
//                    pageNo * pageSize,
//                    pageSize,
//                    createDrivingVideoOperationListener(pageNo, it)
//                )
//            }
//
//
//        }
//    }
//
//    private fun createDrivingVideoOperationListener(
//        pageNo: Int,
//        it: ObservableEmitter<List<DeviceVideo>>
//    ): DrivingVideoOperationListener {
//        return object : DrivingVideoOperationListener {
//            override fun onLockOrUnlockResult(rolling: Boolean) {
//
//            }
//
//            override fun onGetVideoList(rolling: ArrayList<*>) {
//                logger.loggerMessage("onGetVideoList $pageNo $rolling")
//
//
//                val list = rolling.map { any ->
//                    any as DVRInfo
//                }
//                    .map { info ->
//                        info.toDeviceVideo()
//                    }
//                it.onNext(list)
//            }
//
//            override fun onLast() {
//
//                logger.loggerMessage("getVideoList $pageNo onLast")
//                it.onNext(emptyList())
//            }
//
//        }
//    }


    /**
     * 获取已经下载的视频列表
     */
    fun getDownloadedVideoList(videoType: VideoType): Observable<List<DownloadedVideo>> {
        return when (videoType) {
            VideoType.All -> downloadedVideoRepository.allVideoListObservable
            VideoType.Collision -> downloadedVideoRepository.collisionVideoListObservable
            VideoType.Alarm -> downloadedVideoRepository.alarmVideoListObservable
        }
    }

    /**
     * 删除已经下载的视频
     */
    fun deleteDownloadedVideo(videoType: VideoType, downloadedVideo: DownloadedVideo): Boolean {

        val videoPath = downloadedVideo.path

        val result = when (videoType) {

            VideoType.All -> downloadedVideoRepository.deleteVideo(videoPath)
            VideoType.Collision -> downloadedVideoRepository.deleteCollisionVideo(videoPath)
            VideoType.Alarm -> downloadedVideoRepository.deleteAlarmVideo(videoPath)
        }
        if (result) {
            //如果删除成功 更新对应的视频数据

            val videoList = when (videoType) {

                VideoType.All -> allVideoList
                VideoType.Collision -> collisionVideoList
                VideoType.Alarm -> alarmVideoList
            }


//            val deletedVideo = videoList.find { it.name == deviceVideo.name }

            val deviceVideo = videoList.find { videoPath == it.path }

            if (deviceVideo != null) {
                val index = videoList.indexOf(deviceVideo)
                videoList[index] = deviceVideo.onDelete()

                when (videoType) {
                    VideoType.All -> allVideoListSubject
                    VideoType.Collision -> collisionVideoListSubject
                    VideoType.Alarm -> alarmVideoListSubject
                }.onNext(videoList)

            }


        }

        return result
    }

    /**
     * 下载视频
     */
    fun downloadVideo(videoName: String, videoType: VideoType) {
        logger.loggerMessage("开始下载视频 视频名称 $videoName 视频类型 $videoType")



        onVideoStartDownload(videoType, videoName)


        val callback = object : ProgressCallback {
            override fun onDone(path: String, md5: String) {
                val downloadVideoInfo =
                    DownloadVideoInfo(videoName = videoName, filePath = path, fileMd5 = md5)

                logger.loggerMessage("下载视频完成 $downloadVideoInfo")

                val pair = when (videoType) {
                    VideoType.All -> {
                        downloadedVideoRepository.onVideoDownloaded(path)
                        allVideoList to allVideoListSubject
                    }
                    VideoType.Collision -> {
                        downloadedVideoRepository.onCollisionVideoDownloaded(path)
                        collisionVideoList to collisionVideoListSubject
                    }
                    VideoType.Alarm -> {
                        downloadedVideoRepository.onAlarmVideoDownloaded(path)
                        alarmVideoList to alarmVideoListSubject
                    }
                }


                val deviceVideo = pair.first.find { it.name == videoName }

                if (deviceVideo != null) {

                    //更新数据
                    pair.first[pair.first.indexOf(deviceVideo)] = deviceVideo.onDownloaded(path)
                    //发布更新
                    pair.second.onNext(pair.first)
                }


            }

            override fun onProgressChange(progress: Long) {

                val pair = when (videoType) {
                    VideoType.All -> {
                        allVideoList to allVideoListSubject
                    }
                    VideoType.Collision -> {
                        collisionVideoList to collisionVideoListSubject
                    }
                    VideoType.Alarm -> {
                        alarmVideoList to allVideoListSubject
                    }
                }


                val deviceVideo = pair.first.find { it.name == videoName }

                if (deviceVideo != null) {

                    //更新数据
                    pair.first[pair.first.indexOf(deviceVideo)] =
                        deviceVideo.onProgressChange(progress.toInt())
                    //发布更新
                    pair.second.onNext(pair.first)
                }
            }

            override fun onErro(p0: Int) {

            }

        }

        when (videoType) {
            VideoType.All -> deviceHelper.downloadDVR(videoName, callback)
            VideoType.Collision -> deviceHelper.downCollisionMultiMediaFile(0, videoName, callback)
            VideoType.Alarm -> deviceHelper.downADASWarringVideoFile(2, videoName, callback)
        }

    }

    private fun onVideoStartDownload(videoType: VideoType, videoName: String) {
        val pair = when (videoType) {
            VideoType.All -> {
                allVideoList to allVideoListSubject
            }
            VideoType.Collision -> {
                collisionVideoList to collisionVideoListSubject
            }
            VideoType.Alarm -> {
                alarmVideoList to allVideoListSubject
            }
        }


        val deviceVideo = pair.first.find { it.name == videoName }

        if (deviceVideo != null) {

            //更新数据
            pair.first[pair.first.indexOf(deviceVideo)] = deviceVideo.onStartDownload()
            //发布更新
            pair.second.onNext(pair.first)
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
//        deviceHelper.getHasUpdate()

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
                deviceSensitivityLevel.fcw
//                2
                ,
                deviceSensitivityLevel.pcw,
                object : SetDeviceInfoCallback {
                    override fun onSuccess() {
                        if (it.isDisposed) {
                            return
                        }
                        logger.loggerMessage("设置报警灵敏度 结果 成功 $deviceSensitivityLevel ${it.isDisposed}")
                        it.onNext(true)
                        it.onComplete()
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
            deviceHelper.getADASSensitivityLevel(object : GetADASSensitivityCallback {
                override fun OnSuccess(p0: Int, p1: Int, p2: Int) {
                    if (it.isDisposed) {
                        return
                    }
                    logger.loggerMessage("获取报警灵敏度成功")
                    it.onNext(DeviceSensitivityLevel(p0, p1, p2))
                    it.onComplete()
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
        val time = if (value > 25) 25 else if (value < 6) 6 else value

        return Observable.create {
            deviceHelper.setHMWTime(time, getSetDeviceInfoCallback(it))
        }
    }

    fun getHMWTime(): Observable<Int> {
        return Observable.create {
            deviceHelper.getHMWTime(object : onGetHMWTimeCallback {
                override fun onSuccess(p0: Int) {
                    if (it.isDisposed) {
                        return
                    }
                    it.onNext(p0)
                    it.onComplete()
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
                    if (it.isDisposed) {
                        return
                    }
                    logger.loggerMessage("获取报警启动车速成功 ${it.isDisposed}")
                    it.onNext(SpeedThreshold(p0, p1, p2))
                    it.onComplete()
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
                        if (it.isDisposed) {
                            return
                        }

                        logger.loggerMessage("设置报警启动车速成功 ${it.isDisposed}")

                        it.onNext(true)
                        it.onComplete()
                    }

                    override fun onFail(p0: Int) {
                        logger.loggerMessage("设置报警启动车速失败 $p0 ${it.isDisposed}")

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

                    if (it.isDisposed) {
                        return
                    }
                    logger.loggerMessage("获取设备报警配置成功 ${it.isDisposed}")
                    it.onNext(AlarmConfiguration(p0, p1, p2, p3))
                    it.onComplete()
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
                alarmConfiguration.rolling,
                alarmConfiguration.frontCarActive,
                alarmConfiguration.p2,
                alarmConfiguration.p3,
                getSetDeviceInfoCallback(it)
            )
        }
    }


    /**
     * 获取车辆长宽高
     */
    fun getCarParameter(): Observable<CarParameter> {

        return Observable.create {
            deviceHelper.getCarPara(object : OnGetDeviceCarInfoListener {
                override fun onSuccess(p0: Int, p1: Int, p2: Int) {
                    it.onNext(CarParameter(width = p0, height = p1, length = p2))
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 设置车辆长宽高
     */
    fun setCarParameter(carPara: CarParameter): Observable<Boolean> {
        return Observable.create {
            deviceHelper.setCarPara(
                carPara.width,
                carPara.height,
                carPara.length,
                getSetDeviceInfoCallback(it)
            )
        }
    }

    /**
     * 获取摄像头安装参数
     */
    fun getCameraParameter(): Observable<CameraParameter> {

        return Observable.create {
            deviceHelper.getCameraPara(object : OnGetDeviceCameraInfoListener {
                override fun onSuccess(p0: Int, p1: Int, p2: Int) {
                    it.onNext(CameraParameter(p0, p1, p2))
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }


    /**
     * 设置摄像头安装参数
     */
    fun setCameraParameter(cameraPara: CameraParameter): Observable<Boolean> {
        return Observable.create {
            deviceHelper.setCameraPara(
                cameraPara.center,
                cameraPara.height,
                cameraPara.front,
                getSetDeviceInfoCallback(it)
            )
        }


    }


    /**
     * 获取标定参数
     */
    fun getDeviceParameter(): Observable<DeviceParameter> {

        return getCarParameter().flatMap { carParameter ->
            val width = carParameter.width

            return@flatMap getCameraParameter().map {
                return@map DeviceParameter(
                    width = width,
                    center = it.center,
                    height = it.height,
                    front = it.front
                )
            }
        }
    }

    /**
     * 设置标定参数
     */
    fun setDeviceParameter(deviceParameter: DeviceParameter): Observable<Boolean> {


        return getCarParameter()
            .flatMap {
                return@flatMap setCarParameter(
                    CarParameter(
                        width = deviceParameter.width,
                        height = it.height,
                        length = it.length
                    )
                )
            }
            .flatMap { setCarParameterResult ->

                if (setCarParameterResult) {
                    return@flatMap setCameraParameter(
                        CameraParameter(
                            center = deviceParameter.center,
                            front = deviceParameter.front,
                            height = deviceParameter.height
                        )
                    )
                } else {
                    return@flatMap Observable.just(false)
                }

            }
    }


    /**
     * 获取车速
     */
    fun getSpeed(): Observable<String> {
        return Observable.create {
            deviceHelper.getCarInfoData(object : GetCarInfoListener {
                override fun onGetCarInfoData(p0: Vispect_SDK_CarInfo) {

                    if (it.isDisposed) {
                        return
                    }

                    it.onNext(p0.vehicleSpeed)
                    it.onComplete()

                }

                override fun onErro(p0: Int) {
                    if (it.isDisposed) {
                        return
                    }

                    it.onError(DeviceException(p0))
                }

            })
        }
    }


    /**
     * 设置音量大小
     */
    fun setDeviceVoice(voice: Int): Observable<Boolean> {
        return Observable.create<Boolean> {
            deviceHelper.setVoice(voice, getSetDeviceInfoCallback(it))
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

    /**
     * 获取声音类型
     */
    fun getVoiceType(): Observable<VoiceType> {

        return Observable.create<VoiceType> {
            deviceHelper.getADASVoiceParam(object : onGetADASVoiceTypeCallback {
                override fun onSuccess(p0: Int, p1: Int, p2: Int, p3: Int) {
                    it.onNext(p0.toVoiceType())
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    it.onError(DeviceException(p0))
                }

            })
        }
    }

    /**
     * 设置声音类型
     */
    fun setVoiceType(voiceType: VoiceType): Observable<Boolean> {
        return Observable.create<Boolean> {
            val type = voiceType.type
            deviceHelper.setADASVoice(type, type, type, type, getSetDeviceInfoCallback(it))
        }
    }


    /**
     * 获取版本信息
     */
    fun getVersion(): Observable<Version> {
        return Observable.create {
            deviceHelper.getDeviceVersion(object : GetBleInfoVersionCallback {
                override fun onSuccess(
                    p0: String,
                    p1: String,
                    p2: String,
                    p3: String,
                    p4: String,
                    p5: String
                ) {
                    it.onNext(Version(p0, p1, p2, p3, p4, p5))
                    it.onComplete()
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
        buzzerVersion: String,
        currentVersion: String
    ): Observable<CheckUpdateResult> {


        return checkUpdateService.checkUpdate(
            updateType.type,
            obdVersion = obdVersion,
            buzzerVersion = buzzerVersion
        ).map {

            if (deviceHelper.checkoutNeedUpdateCWS(
                    updateType.type,
                    currentVersion,
                    it.message?.versionCode ?: 0
                )
            ) {
                logger.loggerMessage("需要升级 ${updateType.name} $it")
                it
            } else {
                logger.loggerMessage("不需要升级 ${updateType.name} $it")

                CheckUpdateResult(result = 110, message = null)
            }

        }
            .subscribeOn(Schedulers.io())
            .onErrorReturn { throwable ->
                throwable.printStackTrace()

                logger.loggerMessage(throwable.message ?: "出错了")
                return@onErrorReturn CheckUpdateResult(result = 110, message = null)
            }

    }


//    /**
//     *获取升级信息
//     */
//    fun getUpdateMessage(type: Int): Observable<CheckUpdateResult> {
//
//        return Observable.just(1)
//            .observeOn(Schedulers.io())
//            .map {
//                val updateFile = deviceHelper.getHasUpdate(type)
//
//                if (updateFile == null)
//                    CheckUpdateResult(result = 110, message = null)
//                else
//                    CheckUpdateResult(
//                        result = 0, message = UpdateMessage(
//                            path = updateFile.path,
//                            fileName = updateFile.filename,
//                            length = updateFile.length,
//                            versionCode = updateFile.versioncode,
//                            versionName = updateFile.versionname,
//                            md5 = updateFile.md5
//                        )
//                    )
//
//            }
//            .onErrorReturn { throwable ->
//
//                logger.loggerMessage(throwable.message ?: "出错了")
//                return@onErrorReturn CheckUpdateResult(result = 110, message = null)
//            }
//    }


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
        }
    }

    /**
     * 关闭设备升级模式
     */
    fun closeDeviceUpdateMode() {
        deviceHelper.closeDeviceUpdateMode()
    }


    /**
     * 获取通讯板版本号
     */
    fun getConnectionBoardVersion(): Observable<String> {
        return Observable.create {
            logger.loggerMessage("开始获取通讯板版本号")
            deviceHelper.getCommuBoardVersion(object : GetNetVersionCallback {
                override fun onSuccess(p0: String, p1: String) {
                    logger.loggerMessage("获取通讯板版本号成功 $p0 $p1")
                    it.onNext(p1)
                    it.onComplete()
                }

                override fun onFail(p0: Int) {
                    logger.loggerMessage("获取通讯板版本号失败")
                    it.onError(DeviceException(p0))
                }

            })
        }
    }


    /**
     * 上传通讯板升级文件
     */
    fun uploadConnectionBoardUpdateFile(path: String): Observable<Int> {

//        return Observable.create {
//            deviceHelper.uploadNETUpdataFile(path, createProgressCallback(it))
//        }

        return Observable.create<Int> { progressEmitter ->
            //            deviceHelper.uploadOBDUpdataFile(path, createProgressCallback(it))


            deviceHelper.uploadNETUpdataFile(path, object : ProgressCallback {
                override fun onDone(p0: String, p1: String) {

                    logger.loggerMessage("上传通讯板升级文件成功 $p0 $p1 ")


                    deviceHelper.startGetNetUpdateProgress(object : ProgressCallback {
                        override fun onDone(v1: String?, v2: String?) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            progressEmitter.onComplete()
                        }

                        override fun onProgressChange(progress: Long) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            logger.loggerMessage("获取更新通讯板进度 $progress")

                            progressEmitter.onNext((progress / 2).toInt() + 50)
                        }

                        override fun onErro(p0: Int) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            logger.loggerMessage("获取更新通讯板出错 $p0")

                            progressEmitter.onError(DeviceException(p0))
                        }

                    }, 1000)
                }

                override fun onProgressChange(progress: Long) {


                    if (progressEmitter.isDisposed) {
                        return
                    }

                    logger.loggerMessage("上传通讯板文件进度更新 $progress")


                    progressEmitter.onNext((progress / 2).toInt())

                }

                override fun onErro(p0: Int) {

                    if (progressEmitter.isDisposed) {
                        return
                    }

                    logger.loggerMessage("上传通讯板升级文件出错 $p0")

                    progressEmitter.onError(DeviceException(p0))
                }

            })
        }
            .doOnDispose {
                deviceHelper.stopGetNetUpdataProgress()
            }
            .doOnError {
                deviceHelper.stopGetNetUpdataProgress()
            }
            .doOnComplete {
                deviceHelper.stopGetNetUpdataProgress()
            }
    }


    /**
     * 更新设备obd
     */
    fun updateDeviceObd(path: String): Observable<Int> {
        return Observable.create<Int> { progressEmitter ->
            //            deviceHelper.uploadOBDUpdataFile(path, createProgressCallback(it))


            deviceHelper.uploadOBDUpdataFile(path, object : ProgressCallback {
                override fun onDone(p0: String, p1: String) {

                    logger.loggerMessage("上传obd升级文件成功 $p0 $p1 ")


                    deviceHelper.startGetOBDUpdataProgress(object : ProgressCallback {
                        override fun onDone(v1: String?, v2: String?) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            progressEmitter.onComplete()
                        }

                        override fun onProgressChange(progress: Long) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            logger.loggerMessage("获取更新obd进度 $progress")

                            progressEmitter.onNext((progress / 2).toInt() + 50)
                        }

                        override fun onErro(p0: Int) {

                            if (progressEmitter.isDisposed) {
                                return
                            }

                            logger.loggerMessage("获取更新obd出错 $p0")

                            progressEmitter.onError(DeviceException(p0))
                        }

                    }, 1000)
                }

                override fun onProgressChange(progress: Long) {


                    if (progressEmitter.isDisposed) {
                        return
                    }

                    logger.loggerMessage("上传obd文件进度更新 $progress")


                    progressEmitter.onNext((progress / 2).toInt())

                }

                override fun onErro(p0: Int) {

                    if (progressEmitter.isDisposed) {
                        return
                    }

                    logger.loggerMessage("上传obd升级文件出错 $p0")

                    progressEmitter.onError(DeviceException(p0))
                }

            })
        }
            .doOnDispose {
                deviceHelper.stopGetOBDUpdataProgress()
            }
            .doOnError {
                deviceHelper.stopGetOBDUpdataProgress()
            }
            .doOnComplete {
                deviceHelper.stopGetOBDUpdataProgress()
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


    /**
     * 开始校准
     */
    fun startCorrecting(): Observable<Boolean> {

        return Observable.create {
            deviceHelper.startCorrecting(object : CorrectingCallback {
                override fun onGetOperationResult(p0: Boolean) {

                    logger.loggerMessage("onGetOperationResult $p0")

                    if (it.isDisposed) {
                        return
                    }

                    it.onNext(p0)
                    it.onComplete()

                }

                override fun onGetCenterPoint(p0: PointF) {

                    logger.loggerMessage("onGetCenterPoint $p0")
                }

                override fun onGetProgress(p0: Int) {

                    logger.loggerMessage("onGetProgress $p0")
                }

            })
        }


    }


    /**
     * 是否已经校准完成
     */
    fun isCenterPointCorrecting(): Observable<Boolean> {
        return Observable.create { emitter ->

            deviceHelper.getCenterPoint(object : CorrectingCallback {
                override fun onGetOperationResult(p0: Boolean) {

                    logger.loggerMessage("onGetOperationResult $p0")

                }

                override fun onGetCenterPoint(point: PointF) {

                    logger.loggerMessage("onGetCenterPoint $point")


                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onNext(point.x > 0 && point.y > 0)
                    emitter.onComplete()
                }

                override fun onGetProgress(p0: Int) {

                    logger.loggerMessage("onGetProgress $p0")
                }

            })
        }
    }


    /**
     * 获取校准进度
     */
    fun getCorrectingProgress(): Observable<Int> {

        return Observable.create {
            deviceHelper.getCorrectingProgress(object : CorrectingCallback {
                override fun onGetOperationResult(p0: Boolean) {

                    logger.loggerMessage("onGetOperationResult $p0")

                }

                override fun onGetCenterPoint(point: PointF) {

                    logger.loggerMessage("onGetCenterPoint $point")
                }

                override fun onGetProgress(p0: Int) {

                    logger.loggerMessage("onGetProgress $p0")

                    if (it.isDisposed) {
                        return
                    }

                    it.onNext(p0)


                }

            })
        }


    }

    /**
     * 清除OBD故障码
     */
    fun clearOBDErrorCode(): Observable<Boolean> {

        return Observable.create {
            deviceHelper.clearOBDErrorcode(object : SetOBDCrackDataCallback {
                override fun onSuccess() {
                    if (!it.isDisposed) {
                        it.onNext(true)
                    }
                }

                override fun onFail(p0: Int) {
                    if (!it.isDisposed) {
                        it.onNext(false)
                    }
                }

            })
        }

    }


    /**
     * 开始获取obd数据表
     * @param time 需要的时间，单位毫秒
     */
    fun startGetAllCANDataList(time: Long = 13000): Observable<AllCanDataListResult> {


        return Observable.create { emitter ->

            deviceHelper.startGetAllCANDataList(time.toInt(), object : OBDAutoCrackCallback {
                override fun onGetAllCANDataListFinish(p0: Int) {
                    logger.loggerMessage("startGetAllCANDataList onGetAllCANDataListFinish $p0")
                    //如果 rolling 不为0 表示成功
                    if (emitter.isDisposed) {
                        return
                    }
                    emitter.onNext(GetAllCANDataListFinish(p0))
                }

                override fun onGetCurBeFilterListSuccess(p0: ArrayList<OBDAutoCrackElement>) {
                    logger.loggerMessage("startGetAllCANDataList onGetCurBeFilterListSuccess $p0")


                    if (p0.size == 1) {
                        if (emitter.isDisposed) {
                            return
                        }

                        emitter.onNext(CurBeFilter(p0.first()))
                    }
                }

                override fun onStartFilterOutChangedDataFailed() {
                    logger.loggerMessage("startGetAllCANDataList onStartFilterOutChangedDataFailed ")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onStartFilterOutChangedDataFailed"))
                }

                override fun onGetCurReviewStatusFailed() {
                    logger.loggerMessage("startGetAllCANDataList onGetCurReviewStatusFailed ")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onGetCurReviewStatusFailed"))

                }

                override fun onStartToFilterOutFixedDataSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onStartToFilterOutFixedDataSuccess ")

                }

                override fun onStartFilterOutChangedDataSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onStartFilterOutChangedDataSuccess ")

                }

                override fun onToFilterOutFixedDataFinish(p0: Int) {
                    logger.loggerMessage("startGetAllCANDataList onToFilterOutFixedDataFinish $p0")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onNext(FilterOutFixedDataFinish(p0))
                }

                override fun onFilterOutChangedDataFinish(p0: Int) {
                    logger.loggerMessage("startGetAllCANDataList onFilterOutChangedDataFinish $p0")

                    //完成后是否只剩下一条数据

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onNext(FilterOutChangedDataFinish(p0))
                }

                override fun onStartReviewSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onStartReviewSuccess")

                }

                override fun onStartToFilterOutFixedDataFailed() {
                    logger.loggerMessage("startGetAllCANDataList onStartToFilterOutFixedDataFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onStartToFilterOutFixedDataFailed"))


                }

                override fun onStartGetAllCANDataListFailed() {
                    logger.loggerMessage("startGetAllCANDataList onStartGetAllCANDataListFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onStartGetAllCANDataListFailed"))

                }

                override fun onStartGetAllCANDataListSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onStartGetAllCANDataListSuccess")


                }

                override fun onRestartToCrackFailed() {
                    logger.loggerMessage("startGetAllCANDataList onRestartToCrackFailed")

                    if (emitter.isDisposed) {
                        return
                    }
                    emitter.onNext(RestartToCrackResult(false))
                }

                override fun onRestartToCrackSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onRestartToCrackSuccess")

                    if (emitter.isDisposed) {
                        return
                    }
                    emitter.onNext(RestartToCrackResult(true))
                }

                override fun onStartReviewFailed() {
                    logger.loggerMessage("startGetAllCANDataList onStartReviewFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onStartReviewFailed"))
                }

                override fun onStopReviewSuccess() {
                    logger.loggerMessage("startGetAllCANDataList onStopReviewSuccess")

                }

                override fun onGetCurBeFilterListSizeSuccess(p0: Int) {
                    logger.loggerMessage("startGetAllCANDataList onGetCurBeFilterListSizeSuccess $p0")

                }

                override fun onStopReviewFailed() {
                    logger.loggerMessage("startGetAllCANDataList onStopReviewFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onStopReviewFailed"))

                }

                override fun onGetCurBeFilterListSizeFailed() {
                    logger.loggerMessage("startGetAllCANDataList onGetCurBeFilterListSizeFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onGetCurBeFilterListSizeFailed"))

                }

                override fun onGetCurBeFilterListFailed() {
                    logger.loggerMessage("startGetAllCANDataList onGetCurBeFilterListFailed")

                    if (emitter.isDisposed) {
                        return
                    }

                    emitter.onError(RuntimeException("onGetCurBeFilterListFailed"))

                }

                override fun onGetCurReviewStatusSuccess(p0: Int, p1: Int) {
                    logger.loggerMessage("startGetAllCANDataList onGetCurReviewStatusSuccess $p0 $p1")

                }


            })

        }


    }


    /**
     * 获取当前嫌疑人列表
     */
    fun getCurBeFilterList() {
        deviceHelper.getCurBeFilterList()
    }

    /**
     * 开始破解新的一边
     */
    fun restartToCrack() {
        deviceHelper.restartToCrack()
    }


//    /**
//     * 停止获取obd数据
//     */
//    fun stopGetAllCANDataList(): Observable<Boolean> {
//
//
//        return Observable.create { emitter ->
//            deviceHelper.stopGetAllCANDataList(object : OBDDebugCallback {
//                override fun onGetCANData(rolling: ByteArray, p1: ByteArray) {
//
//
//                }
//
//                override fun onGetResult(result: Boolean) {
//                    if (emitter.isDisposed) {
//                        return
//                    }
//                    emitter.onNext(result)
//                }
//
//            })
//        }
//
//    }


    /**
     * 手动设置obd
     */
    fun setObdData(data: String): Observable<Boolean> {


        return Observable.create<Boolean> {
            deviceHelper.setOBDCrackData(data, object : SetOBDCrackDataCallback {
                override fun onSuccess() {
                    if (it.isDisposed) {
                        return
                    }
                    it.onNext(true)
                }

                override fun onFail(p0: Int) {
                    if (it.isDisposed) {
                        return
                    }
                    it.onNext(false)
                }

            })
        }
    }


    /**
     * 开始破解
     */
    fun openCanListener(): Observable<Boolean> {

        return Observable.create {
            deviceHelper.openCANListener(object : OBDDebugCallback {
                override fun onGetCANData(p0: ByteArray, p1: ByteArray) {


                }

                override fun onGetResult(p0: Boolean) {
                    logger.loggerMessage("openCanListener onGetResult $p0")

                    if (it.isDisposed) {
                        return
                    }

                    it.onNext(p0)
                    it.onComplete()
                }

            })
        }

    }


    fun stopAutoCrack(): Observable<Boolean> {

//        deviceHelper.getReviewStatus()

        return Observable.create {
            deviceHelper.stopAutoCrack(object : OBDDebugCallback {
                override fun onGetCANData(p0: ByteArray, p1: ByteArray) {
                    logger.loggerMessage("stopAutoCrack onGetCANData $p0 $p1")
                }

                override fun onGetResult(p0: Boolean) {
                    logger.loggerMessage("stopAutoCrack onGetResult $p0")

                    if (it.isDisposed) {
                        return
                    }
                    it.onNext(p0)
                    it.onComplete()
                }

            })
        }

    }

    /**
     * 验证破解出来的obd数据对不对
     */
    fun startReview(left: OBDAutoCrackElement, right: OBDAutoCrackElement) {

        deviceHelper.startReview(left, right)
    }

    /**
     * 验证obd数据是否正确
     */
    fun getReviewStatus() {
        deviceHelper.getReviewStatus()
    }


    /**
     * 设置obd数据
     */
    fun setOBDCrackData(
        left: OBDAutoCrackElement,
        right: OBDAutoCrackElement
    ): Observable<Boolean> {

        return Observable.create { emitter ->
            deviceHelper.setOBDCrackData(left, right, object : SetOBDCrackDataCallback {
                override fun onSuccess() {
                    if (emitter.isDisposed) {
                        return
                    }
                    emitter.onNext(true)
                }

                override fun onFail(p0: Int) {
                    if (emitter.isDisposed) {
                        return
                    }
                    emitter.onNext(false)

                }
            })
        }
    }

    /**
     * 开始过滤发生变化
     */
    fun startToFilterOutChangedData(time: Long = 9000) {
        deviceHelper.startToFilterOutChangedData(time)
    }

    /**
     * 开始过滤掉没发生变化
     */
    fun startToFilterOutFixedData(time: Long = 13000) {
        deviceHelper.startToFilterOutFixedData(time)
    }


    /**
     * 开始验证
     */
    fun startVerification(interval: Long = 1000): Observable<TurnSignal> {

        return Observable.create<TurnSignal> { emitter ->

            logger.loggerMessage(
                "开始验证转向灯信号"
            )
            deviceHelper.startVerification(interval) {


                logger.loggerMessage("获取到obd信号 $it")
                if (!emitter.isDisposed) {
                    when (it) {
                        Vispect_SDK_ARG.LIGHT_LEFT -> emitter.onNext(TurnSignal.Left)
                        Vispect_SDK_ARG.LIGHT_RIGHT -> emitter.onNext(TurnSignal.Right)
                        else -> emitter.onNext(TurnSignal.None)
                    }
                }


            }


        }.doOnDispose {
            logger.loggerMessage("停止验证obd")
            deviceHelper.stopVerification()
        }


    }

    private fun createProgressCallback(observableEmitter: ObservableEmitter<Int>): ProgressCallback {

        return object : ProgressCallback {
            override fun onDone(p0: String, p1: String) {
                if (observableEmitter.isDisposed) {
                    return
                }
                observableEmitter.onComplete()
            }

            override fun onProgressChange(p0: Long) {

                if (observableEmitter.isDisposed) {
                    return
                }
                observableEmitter.onNext(p0.toInt())
            }

            override fun onErro(p0: Int) {
                if (observableEmitter.isDisposed) {
                    return
                }
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

    private fun getOnWifiOpenListener(e: ObservableEmitter<Pair<String, String>>): OnWifiOpenListener {
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

