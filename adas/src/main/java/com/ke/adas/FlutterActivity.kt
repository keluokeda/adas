package com.ke.adas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class FlutterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flutter)


    }
}


fun Context.toFlutterActivity() {
    val intent = Intent(this, FlutterActivity::class.java)
    startActivity(intent)
}
