package com.ke.adas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import bean.DrawShape
import com.ke.adas.databinding.KeAdasActivityAdasrealViewBinding
import com.ke.adas.entity.CondenseLevel
import com.ke.adas.entity.RealViewEntity
import com.ke.adas.widget.ADASSurfaceView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.math.sin

abstract class ADASRealViewActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()


    protected abstract fun handleError(throwable: Throwable)


    protected abstract fun getDeviceService(): DeviceService

    protected abstract fun loggerMessage(message: String)

    private val speedSubject = PublishSubject.create<String>()

    private val sensorSubject = PublishSubject.create<Float>()

    private val deviceWifiDisconnectedSubject = PublishSubject.create<Boolean>()

    private var isDeviceWifiConnected = false


    private var wifiName: String? = null

    private lateinit var emitter: SingleEmitter<Boolean>


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val netWorkInfo: NetworkInfo =
                intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO) ?: return

            loggerMessage("wifi状态发生变化")

            loggerMessage("网络连接状态 ${netWorkInfo.detailedState.name}")

            loggerMessage("网络信息 $netWorkInfo")

            if (netWorkInfo.detailedState == NetworkInfo.DetailedState.CONNECTED && isDeviceWifiConnected()
            ) {
                //连上设备的wifi
                //这里可能会走两次
                emitter.onSuccess(true)

            } else if (netWorkInfo.detailedState == NetworkInfo.DetailedState.DISCONNECTED && isDeviceWifiConnected()) {
                //设备wifi已经断开

                loggerMessage("设备wifi已经断开")

                if (isDeviceWifiConnected) {
                    deviceWifiDisconnectedSubject.onNext(true)
                }

            }

        }

    }

    override fun onResume() {
        super.onResume()

        if (isDeviceWifiConnected()) {
            emitter.onSuccess(true)
        }
    }

    private lateinit var binding: KeAdasActivityAdasrealViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFullScreen()

        binding = KeAdasActivityAdasrealViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        if (!getDeviceService().isConnectedDevice()) {
            toastMessage("设备蓝牙没有连接")
            finish()
        }

        binding.progressContainer.visibility = View.VISIBLE


        addWifiConnectedListener()

        openDeviceRealViewMode()

        addDeviceStateChangedListener()

        updateSpeed()


        updateSensorLine()

        initViewListener()

        val intentFilter = IntentFilter()
            .apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }

        registerReceiver(receiver, intentFilter)

        hideNavigation()


        deviceWifiDisconnectedSubject.debounce(1000, TimeUnit.MILLISECONDS).subscribe(
            {
                reconnectDeviceWifi()
            }, {
                handleError(it)
            }
        ).addTo(compositeDisposable)


        getDeviceService()
            .getCondenseLevel()
            .flatMap { level ->
                if (level != CondenseLevel.ExtraHeight) {
                    return@flatMap getDeviceService().setCondenseLevel(CondenseLevel.ExtraHeight)
                } else {
                    return@flatMap Observable.just(false)
                }


            }
            .subscribe({

            }, {
                handleError(it)
            })
            .addTo(compositeDisposable)
    }

    private fun updateSensorLine() {
        sensorSubject.throttleFirst(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                //                horizontal_line.rotation = it
//                horizontal_line.invalidate()

                binding.horizontalLine.animate().rotation(it).setDuration(100).start()

            }.addTo(compositeDisposable)
    }


    /**
     * 重新连接设备wifi
     */
    private fun reconnectDeviceWifi() {
        loggerMessage("开始重新连接设备wifi")
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val list = wifiManager.configuredNetworks

        val deviceWifiConfiguration = list.firstOrNull { isDeviceWifiSSID(it.SSID) }

        if (deviceWifiConfiguration != null) {
            val result = wifiManager.enableNetwork(deviceWifiConfiguration.networkId, true)
            loggerMessage("开始重新和设备建立wifi连接 连接结果 $result wifi信息 $deviceWifiConfiguration")
        } else {
            loggerMessage("重新连接时发现手机没有保存设备wifi信息")
        }
    }

    /**
     * 设备wifi是否连上
     */
    private fun isDeviceWifiConnected(): Boolean {

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        val ssid = wifiInfo?.ssid?.replace("\"", "")

        return TextUtils.equals(ssid, wifiName)

//        return isDeviceWifiSSID(networkInfo.extraInfo ?: return false)
    }

    private fun isDeviceWifiSSID(ssid: String): Boolean {
        return TextUtils.equals(wifiName, ssid.replace("\"", ""))
    }

    private fun addDeviceStateChangedListener() {
        getDeviceService().observeConnectState().observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (!it) {
                    loggerMessage("设备已经断开")
                    Toast.makeText(applicationContext, "蓝牙已断开", Toast.LENGTH_SHORT).show()
                    toastMessage("设备蓝牙已经断开")
                    finish()
                }
            }, {
                handleError(it)
            }).addTo(compositeDisposable)
    }

    private fun initViewListener() {
        binding.connect.setOnClickListener {

            //跳转到wifi设置界面
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        binding.back.setOnClickListener {
            finish()
        }
    }

    private fun updateSpeed() {
        speedSubject.throttleFirst(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.tvSpeed.text = it
            }.addTo(compositeDisposable)
    }

    private fun addWifiConnectedListener() {
        Single.create<Boolean> {
            emitter = it
        }
            .subscribe(Consumer {
                isDeviceWifiConnected = true
                initRealView()
                startRealView()
            }).addTo(compositeDisposable)
    }

    private fun openDeviceRealViewMode() {
        getDeviceService()
            .openDeviceRealViewMode()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    binding.progressContainer.visibility = View.GONE
                    binding.layoutConnect.visibility = View.VISIBLE
                    binding.wifiName.text = it.first
                    binding.wifiPassword.text = it.second
                    this.wifiName = it.first

                }, {
                    toastMessage("开启实况模式失败")
                    handleError(it)
                    finish()
                }
            ).addTo(compositeDisposable)
    }

    private fun hideNavigation() {
        val params = window.attributes
        params.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.attributes = params

    }

    private var drawShapeList: MutableList<DrawShape> = mutableListOf()

    val sensorShape = DrawShape()


    private fun initRealView() {
        getDeviceService()
            .initRealView()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .subscribe({ realViewEntity ->
                //                loggerMessage(realViewEntity.toString())
                when (realViewEntity.type) {
                    RealViewEntity.TYPE_FRAME -> {
                        //                        mBooleanSingleEmitter.onSuccess(java.lang.Boolean.TRUE)
                        binding.videoSurfaceView.setOnePixData(realViewEntity.mBytes, realViewEntity.size)
                    }
                    RealViewEntity.TYPE_ADAS_SENSOR -> {
                        val x = realViewEntity.x
                        val y = realViewEntity.y

                        val isNewSensor = x <= y

                        val degreeX = if (isNewSensor) ADASSurfaceView.todegree(
                            y,
                            x
                        ) else ADASSurfaceView.todegree(x, y)

                        val lasty =
                            ((640 / sin((90 - degreeX) * Math.PI / 180)) * sin(degreeX * Math.PI / 180))


                        sensorShape.color = Color.RED
                        sensorShape.type = 3
                        sensorShape.isDashed = false
                        sensorShape.x0 = 0f
                        sensorShape.y0 = (360 - lasty).toFloat()
                        sensorShape.x1 = 1280f
                        sensorShape.y1 = (360 + lasty).toFloat()

                        drawShapeList.add(sensorShape)

                        binding.adasSurfaceView.setDrawList(drawShapeList)

//                        sensorSubject.onNext(realViewEntity.x * -9f)


                    }
                    RealViewEntity.TYPE_ADAS_INFO -> {
                        drawShapeList = realViewEntity.mDrawShapes
//                        adas_surface_view.setDrawList(realViewEntity.mDrawShapes)
                    }
                    RealViewEntity.TYPE_SPEED -> {
                        //需要在主线程更新
//                        tv_speed.text = realViewEntity.speed
                        speedSubject.onNext(realViewEntity.speed)
                    }
                    RealViewEntity.TYPE_ERROR -> {
                        loggerMessage("发生了错误 $realViewEntity")
                    }
                    else -> {
                    }
                }//x  贴纸向左边-10 贴纸向上0 贴纸向右边10
                //y  镜头向上10 镜头向前 0  镜头向下下 -10
                //z 后10 上0 前10
            }, {
                handleError(it)
                toastMessage("初始化实况模式失败")
                setResult(RESULT_CANCELED)
                finish()
            })
            .addTo(compositeDisposable)
    }


    override fun onBackPressed() {
        loggerMessage("点了返回按钮")
        super.onBackPressed()
    }

    /**
     * 开启实况模式
     */
    private fun startRealView() {
        binding.layoutConnect.visibility = View.GONE
        binding.progressContainer.visibility = View.VISIBLE

        getDeviceService().startRealView()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //开启实况模式成功后 取消连接wifi提示框
                binding.layoutConnect.visibility = View.GONE
                binding.progressContainer.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE

                loggerMessage(
                    "开启实况模式结果 $it"
                )
            }, {
                toastMessage("开始实况模式失败")
                handleError(it)
                finish()
            })
            .addTo(compositeDisposable)

        binding.videoSurfaceView
            .initSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //收到第一帧后 隐藏进度条
                binding.progressContainer.visibility = View.GONE
                binding.divider.visibility = View.VISIBLE
            }, {
                handleError(it)
            }).addTo(compositeDisposable)
    }

    override fun onDestroy() {
        super.onDestroy()


        unregisterReceiver(receiver)

        compositeDisposable.dispose()

    }


    private fun requestFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        /*set it to be full screen*/
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }


    private fun toastMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}
