package com.ke.adas

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.ke.adas.entity.RealViewEntity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_adasreal_view.*

abstract class ADASRealViewActivity : AppCompatActivity() {


    private var d1: Disposable? = null
    private var d2: Disposable? = null
    private var d3: Disposable? = null

    protected abstract fun handleError(throwable: Throwable)


    protected abstract fun getDeviceService(): DeviceService

    protected abstract fun loggerMessage(message: String)


    private var wifiName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFullScreen()
        setContentView(R.layout.activity_adasreal_view)


        if (!getDeviceService().isConnectedDevice()) {
            finish()
        }

        progress_container.visibility = View.VISIBLE





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

        connect.setOnClickListener {
            if (TextUtils.equals(wifiName, getCurrentWifiName())) {
                initRealView()
                startRealView()
            }
        }

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
                layout_connect.visibility = View.GONE
                progress_container.visibility = View.GONE
                loggerMessage(
                    "开启实况模式结果 $it"
                )
            }, {
                handleError(it)
                //                setResult(Activity.RESULT_CANCELED)
                finish()
            })
    }

    override fun onDestroy() {
        super.onDestroy()

        d1?.dispose()
        d2?.dispose()
        d3?.dispose()
    }


    private fun requestFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        /*set it to be full screen*/
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }


    protected fun getCurrentWifiName(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiInfo = wifiManager.connectionInfo

        return wifiInfo.ssid.replace("\"", "")
    }
}
