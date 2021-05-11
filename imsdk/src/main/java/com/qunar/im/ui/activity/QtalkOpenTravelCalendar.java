package com.qunar.im.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

import com.qunar.im.base.protocol.Protocol;
import com.qunar.im.core.manager.IMNotificaitonCenter;
import com.qunar.im.permission.PermissionCallback;
import com.qunar.im.permission.PermissionDispatcher;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.ui.R;
import com.qunar.im.ui.view.QtNewActionBar;
import com.qunar.im.ui.view.swipBackLayout.SwipeBackActivity;
import com.qunar.im.utils.ConnectionUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class QtalkOpenTravelCalendar  extends SwipeBackActivity  implements IMNotificaitonCenter.NotificationCenterDelegate, PermissionCallback {

    protected QtNewActionBar qtNewActionBar;//头部导航

    private static final int PERMISSION_REQUIRE = PermissionDispatcher.getRequestCode();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atom_ui_travel_calendar);
        bindView();
        addEvent();
        startRNApplicationWithBundle(getExtendBundle());
        bindData();
    }

    private void bindData() {
        setActionBarRightSpecial(0);
//        setActionBarSingleTitle(mTitles[mViewPager.getCurrentItem()]);
        setActionBarRightIcon(R.string.atom_ui_new_select_calendar);
        setActionBarRightIconClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TimePickerView tpv = new TimePickerBuilder(QtalkOpenTravelCalendar.this, new OnTimeSelectListener() {
//                    @Override
//                    public void onTimeSelect(Date date, View v) {
//                        String time = new SimpleDateFormat("yyyy-MM-dd").format(date);
//                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.SELECT_DATE,time);
//                    }
//                }).build();
//
//                tpv.show();
            }
        });
        checkPermission();
    }

    /**
     * 检查权限
     */
    private void checkPermission(){
        PermissionDispatcher.requestPermissionWithCheck(this,
                new int[]{PermissionDispatcher.REQUEST_READ_CALENDAR,PermissionDispatcher.REQUEST_WRITE_CALENDAR
                        /*, PermissionDispatcher.REQUEST_READ_PHONE_STATE*/}, this,
                PERMISSION_REQUIRE);
    }

    private void bindView() {
        qtNewActionBar = (QtNewActionBar) this.findViewById(R.id.my_action_bar);
        setNewActionBar(qtNewActionBar);
        setActionBarTitle("行程");
    }

    private void startRNApplicationWithBundle(Bundle extendBundle) {
        String module = extendBundle.getString("name");
        // render react
    }

    private Bundle getExtendBundle() {
        Intent intent = getIntent();
        Uri data = intent.getData();
        Bundle bundle = intent.getExtras();
        if(bundle == null){
            bundle = new Bundle();
        }
        if(data != null){//schema 跳转
            HashMap<String, String> map = Protocol.splitParams(data);
            for (Map.Entry<String,String> entry : map.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
            bundle.putString("name", map.get("module"));
        }else { //intent 跳转
            bundle.putString("name", intent.getStringExtra("module"));
        }
        return bundle;

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (mReactRootView != null) {
//            mReactRootView.unmountReactApplication();
////            mReactRootView = null;
//        }
        removeEvent();

    }

    public void updatePage() {
        startRNApplicationWithBundle(getExtendBundle());
    }

    @Override
    public void onBackPressed() {

            super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void addEvent() {
        ConnectionUtil.getInstance().addEvent(this, QtalkEvent.RN_UPDATE);
        ConnectionUtil.getInstance().addEvent(this,QtalkEvent.UPDATE_TRIP);
    }

    private void removeEvent() {
        ConnectionUtil.getInstance().removeEvent(this, QtalkEvent.RN_UPDATE);
        ConnectionUtil.getInstance().removeEvent(this,QtalkEvent.UPDATE_TRIP);
    }


    @Override
    public void didReceivedNotification(String key, Object... args) {
        switch (key) {
            case QtalkEvent.RN_UPDATE:
                updatePage();
                break;
            case QtalkEvent.UPDATE_TRIP:
                //请求最新的好友
//                clearBridge();
                break;
        }
    }

    @Override
    public void responsePermission(int requestCode, boolean granted) {
        if (requestCode == PERMISSION_REQUIRE) {
            if (!granted) {
                Toast.makeText(this,"没有日历权限,Qtalk无法同步日程到系统日历", Toast.LENGTH_LONG).show();
//                finish();
                return;
            }


        }
    }
}
