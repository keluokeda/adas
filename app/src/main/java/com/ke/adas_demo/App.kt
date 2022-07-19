package com.ke.adas_demo

import android.app.Application
import android.content.Context
import com.ke.adas.DeviceService
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.tencent.bugly.crashreport.CrashReport
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.utils.UMUtils
import com.umeng.message.PushAgent
import com.umeng.message.api.UPushRegisterCallback
import kotlin.concurrent.thread

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogger()



        CrashReport.initCrashReport(this, "0271eded8e", true)


        preInitPush("62cfdd72fb3e4a2cbaec2e51", "93427b93f93ba2a5594b931b9993487c")

        initPush("62cfdd72fb3e4a2cbaec2e51", "93427b93f93ba2a5594b931b9993487c") {
            Logger.d(it)
        }



        initDeviceService()

    }

    private fun initDeviceService() {
        val deviceService = DeviceService(object : com.ke.adas.Logger {
            override fun loggerMessage(message: String) {
                Logger.d(message)
            }

        })

        deviceService.init(this, "d76f9e2da4ec45269e4feeb62aaf4af8")
            .subscribe {
            }
    }


    private fun initLogger() {
        val formatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(true)
            .methodCount(5)
            .tag("logger")
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return BuildConfig.DEBUG
            }
        })
    }


}


/**
 * 预初始化 在同意协议之前调用
 */
fun Context.preInitPush(
    appKey: String,
    appSecret: String
) {
    UMConfigure.setLogEnabled(com.umeng.commonsdk.BuildConfig.DEBUG)
    //pre init
    PushAgent.setup(this, appKey, appSecret)
    UMConfigure.preInit(this, appKey, "umeng")
}

/**
 * 初始化 在同意协议之后调用
 */
fun Context.initPush(
    appKey: String,
    appSecret: String,
    callback: (PushRegisterResult) -> Unit
) {
    UMConfigure.init(
        this, appKey, "umeng",
        UMConfigure.DEVICE_TYPE_PHONE, appSecret
    )
    //自动采集页面
    MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO)


    if (UMUtils.isMainProgress(this)) {
        thread {
            PushAgent.getInstance(this).register(object : UPushRegisterCallback {
                override fun onSuccess(token: String) {
                    callback(PushRegisterResult.Success(token))
                }

                override fun onFailure(p0: String, p1: String) {
                    callback(PushRegisterResult.Error(p0, p1))
                }

            })
        }
    } else {
        PushAgent.getInstance(this).register(object : UPushRegisterCallback {
            override fun onSuccess(token: String) {


                callback(PushRegisterResult.Success(token))
            }

            override fun onFailure(p0: String, p1: String) {
                callback(PushRegisterResult.Error(p0, p1))
            }

        })
    }
}


sealed interface PushRegisterResult {
    data class Success(val token: String) : PushRegisterResult

    data class Error(val errCode: String, val errDesc: String) : PushRegisterResult
}

