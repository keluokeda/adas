package com.ke.adas_demo

import android.app.Application
import com.ke.adas.DeviceService
import com.mpaas.mps.adapter.api.MPPush
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogger()

        MPPush.setup(this)
        MPPush.init(this)

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

