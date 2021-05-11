package com.qunar.im.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.qunar.im.ui.R;
import com.qunar.im.ui.view.QtNewActionBar;
import com.qunar.im.ui.view.swipBackLayout.SwipeBackActivity;

public class QtalkServiceExternalRNActivity extends SwipeBackActivity {

    protected QtNewActionBar qtNewActionBar;//头部导航





    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atom_ui_activity_rn_external);
        bindView();


    }

    private void bindView() {
        try {
            qtNewActionBar = (QtNewActionBar) this.findViewById(R.id.my_action_bar);
            setNewActionBar(qtNewActionBar);
            boolean isShow = Boolean.parseBoolean(getIntent().getStringExtra("showNativeNav"));
            if (isShow) {
                String title = getIntent().getStringExtra("navTitle");
                setActionBarTitle(title);
                setActionBarVisibility(true);
            } else {
                setActionBarVisibility(false);
            }
//        mReactRootView = new ReactRootView(this);
            startRNApplicationWithBundle(getExtendBundle());
//        setContentView(mReactRootView);
        }catch (Exception e){
            Logger.i("打开外部RN应用出现不可预知错误:"+e.getMessage());
        }
    }

    private void startRNApplicationWithBundle(Bundle extendBundle) {
        String bundleName = extendBundle.getString("Bundle");
        String Entrance = extendBundle.getString("Entrance");
        if(TextUtils.isEmpty(bundleName)){
            finish();
            return;
        }
//        }
        String module = extendBundle.getString("name");
        // render react
    }

    private Bundle getExtendBundle() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        bundle.putString("name", intent.getStringExtra("module"));
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




}