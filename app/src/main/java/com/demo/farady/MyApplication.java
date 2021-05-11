package com.demo.farady;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.demo.farady.activity.RouterUtil;
import com.qunar.im.ui.sdk.QIMSdk;


/**
 * Created by lihaibin.li on 2018/2/22.
 */

public class MyApplication extends Application {

    public static Context getContext() {
        return context;
    }

    public static Context context;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        QIMSdk.getInstance().init(this);
//        QIMSdk.getInstance().openDebug();
        RouterUtil routerUtil = new RouterUtil();
        routerUtil.init(this);
    }
}
