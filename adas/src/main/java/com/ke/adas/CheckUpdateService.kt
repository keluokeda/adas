package com.ke.adas

import com.ke.adas.entity.CheckUpdateResult
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.POST

interface CheckUpdateService {

    @POST("Vispect_service_test/checkhasupdate")
    fun checkUpdate(
        /**
         * 类型编号 1，固件 3，操作系统 9，OBD，10，设备app
         */
        @Field("type") type: Int,
        @Field("hasLimitingCondition") hasLimitingCondition: Int = 0,
        @Field("obdversion") obdVersion: String,
        @Field("buzzerversion") buzzerVersion: String
    ): Observable<CheckUpdateResult>
}