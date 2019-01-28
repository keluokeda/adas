package com.ke.adas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout
//import io.flutter.facade.Flutter

class FlutterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flutter)

//
//        val flutterView = Flutter.createView(this, lifecycle, "/")
//
//
////添加flutter view
//        addContentView(
//            flutterView,
//            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
//        )


    }
}


fun Context.toFlutterActivity() {
    val intent = Intent(this, FlutterActivity::class.java)
    startActivity(intent)
}
