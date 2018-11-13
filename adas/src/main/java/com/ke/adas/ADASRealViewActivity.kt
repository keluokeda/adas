package com.ke.adas

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import com.ke.adas.entity.RealViewEntity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_adasreal_view.*

abstract class ADASRealViewActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()


    protected abstract fun handleError(throwable: Throwable)


    protected abstract fun getDeviceService(): DeviceService

    protected abstract fun loggerMessage(message: String)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestFullScreen()
        setContentView(R.layout.activity_adasreal_view)

        if (!getDeviceService().isConnectedDevice()) {
            finish()
        }


        //初始化
        getDeviceService().initRealView().observeOn(Schedulers.newThread())
            .subscribe({ realViewEntity ->
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
            }).addTo(compositeDisposable)

        //开启实况模式
        getDeviceService().startRealView()
            .subscribe({

                loggerMessage(
                    "开启实况模式结果 $it"
                )
            }, {
                handleError(it)
//                setResult(Activity.RESULT_CANCELED)
                finish()
            })
            .addTo(compositeDisposable)

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
