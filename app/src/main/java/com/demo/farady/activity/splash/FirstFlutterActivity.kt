package com.demo.farady.activity.splash

import android.content.Context
import android.os.Bundle
import com.yuxiaor.flutter.g_faraday.Faraday
import com.yuxiaor.flutter.g_faraday.FaradayActivity
import com.yuxiaor.flutter.g_faraday.channels.registerNotification
import com.yuxiaor.flutter.g_faraday.channels.unregisterNotification

class FirstFlutterActivity : FaradayActivity() {

    companion object {
        fun build(context: Context) = builder<FirstFlutterActivity>("home", null, false).build(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Faraday.registerNotification("GlobalNotification") {
            showNotification()
        }
    }

    private fun showNotification() {

    }

    override fun onDestroy() {
        super.onDestroy()
        Faraday.unregisterNotification("GlobalNotification")
    }
}