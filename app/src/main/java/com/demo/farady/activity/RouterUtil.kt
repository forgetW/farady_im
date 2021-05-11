package com.demo.farady.activity

import android.content.Context
import com.demo.farady.activity.splash.FirstFlutterActivity
import com.yuxiaor.flutter.g_faraday.Faraday
import com.demo.farady.router.CustomNavigator
import com.yuxiaor.flutter.g_faraday.FaradayActivity

class RouterUtil {
    fun init(context: Context){
        Faraday.startFlutterEngine(context, CustomNavigator)


    }

    fun start2newHome(context: Context){
        val intent = FaradayActivity.builder("NewHomePage").build(context)
        // 直接打开flutter 页面
        context.startActivity(intent)
    }

    fun start2First(context: Context){
        // 跳转到 flutter `home` 路由
        val intent = FirstFlutterActivity.build(context)

        // 直接打开flutter 页面
        context.startActivity(intent)
    }
}