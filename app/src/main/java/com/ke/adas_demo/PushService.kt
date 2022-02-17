package com.ke.adas_demo

import com.alipay.pushsdk.content.MPPushMsgServiceAdapter
import com.orhanobut.logger.Logger

class PushService : MPPushMsgServiceAdapter() {


    override fun onTokenReceive(token: String) {
        Logger.d("PushService onTokenReceive $token")
    }
}