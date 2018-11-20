package com.ke.adas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.ke.adas.entity.RealViewEntity
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_adasreal_view.*
import java.util.concurrent.TimeUnit

abstract class ADASRealViewActivity : AppCompatActivity() {


    private var d1: Disposable? = null
    private var d2: Disposable? = null
    private var d3: Disposable? = null
    private var d4: Disposable? = null
    private var d5: Disposable? = null
    private var d6: Disposable? = null


    protected abstract fun handleError(throwable: Throwable)


    protected abstract fun getDeviceService(): DeviceService

    protected abstract fun loggerMessage(message: String)

    private val speedSubject = PublishSubject.create<String>()


    private var wifiName: String? = null

    private lateinit var emitter: SingleEmitter<Boolean>


    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val netWorkInfo: NetworkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)

            loggerMessage("wifi状态发生变化")

            loggerMessage("网络连接状态 ${netWorkInfo.detailedState.name}")

            loggerMessage("网络信息 $netWorkInfo")

            if (netWorkInfo.detailedState == NetworkInfo.DetailedState.CONNECTED && TextUtils.equals(
                    wifiName,
                    netWorkInfo.extraInfo.replace("\"", "")
                )
            ) {
                //连上设备的wifi
                //这里可能会走两次
                emitter.onSuccess(true)

            }

        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFullScreen()
        setContentView(R.layout.activity_adasreal_view)


        if (!getDeviceService().isConnectedDevice()) {
            finish()
        }

        progress_container.visibility = View.VISIBLE


        d4 = Single.create<Boolean> {
            emitter = it
        }
            .subscribe(Consumer {
                initRealView()
                startRealView()
            })

        d1 = getDeviceService()
            .openDeviceRealViewMode()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                {
                    progress_container.visibility = View.GONE
                    layout_connect.visibility = View.VISIBLE
                    wifi_name.text = it.first
                    wifi_password.text = it.second
                    this.wifiName = it.first

                }, {
                    handleError(it)
                    finish()
                }
            )


        d5 = speedSubject.debounce(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
            tv_speed.text = it
        }

        connect.setOnClickListener {

            //跳转到wifi设置界面
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        back.setOnClickListener {
            finish()
        }

        val intentFilter = IntentFilter()
            .apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }

        registerReceiver(receiver, intentFilter)

        hideNavigation()
    }

    private fun hideNavigation() {
        val params = window.attributes
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE
        window.attributes = params

    }

    private fun initRealView() {
        d3 = getDeviceService()
            .initRealView()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .subscribe({ realViewEntity ->
                //                loggerMessage(realViewEntity.toString())
                when (realViewEntity.type) {
                    RealViewEntity.TYPE_FRAME -> {
                        //                        mBooleanSingleEmitter.onSuccess(java.lang.Boolean.TRUE)
                        video_surface_view.setOnePixData(realViewEntity.mBytes, realViewEntity.size)
                    }
                    RealViewEntity.TYPE_ADAS_SENSOR -> {
                    }
                    RealViewEntity.TYPE_ADAS_INFO -> adas_surface_view.setDrawList(realViewEntity.mDrawShapes)
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
                setResult(RESULT_CANCELED)
                finish()
            })
    }

    override fun onBackPressed() {
        loggerMessage("点了返回按钮")
        super.onBackPressed()
    }

    /**
     * 开启实况模式
     */
    private fun startRealView() {
        layout_connect.visibility = View.GONE
        progress_container.visibility = View.VISIBLE

        d2 = getDeviceService().startRealView()
            .subscribeOn(Schedulers.io())
            .unsubscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //开启实况模式成功后 取消连接wifi提示框
                layout_connect.visibility = View.GONE
                progress_container.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE

                loggerMessage(
                    "开启实况模式结果 $it"
                )
            }, {
                handleError(it)
                finish()
            })

        d6 = video_surface_view.initSubject.subscribe({
            //收到第一帧后 隐藏进度条
            progress_container.visibility = View.GONE
            divider.visibility = View.GONE
        }, {
            handleError(it)
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        d1?.dispose()
        d2?.dispose()
        d3?.dispose()
        d4?.dispose()
        d5?.dispose()
        d6?.dispose()

        unregisterReceiver(receiver)
    }


    private fun requestFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        /*set it to be full screen*/
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }


}
