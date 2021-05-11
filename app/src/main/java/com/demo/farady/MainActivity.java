package com.demo.farady;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.farady.activity.RouterUtil;
import com.qunar.im.base.module.Nick;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.ui.activity.TabMainActivity;
import com.qunar.im.ui.broadcastreceivers.PdChatIMsgSendReceiver;
import com.qunar.im.ui.sdk.QIMSdk;
import com.qunar.im.ui.util.NotifyUtilTest;
import com.qunar.im.utils.ConnectionUtil;
import com.yuxiaor.flutter.g_faraday.FaradayActivity;


import java.util.List;

public class MainActivity extends Activity {

    private Button autoLoginButton,startPlatForm;
    private TextView logcat_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoLoginButton = (Button) findViewById(R.id.autoLoginButton);
        startPlatForm = (Button) findViewById(R.id.startPlatForm);
        logcat_text = (TextView) findViewById(R.id.logcat_text);

        startPlatForm.setText("启动" + CommonConfig.currentPlat);
        initChatMsgReceiver();

    }

    /**
     * IM界面关闭广播
     */
    private void initChatMsgReceiver() {
        //注册广播接收器
        PdChatIMsgSendReceiver mPdChatIMsgSendReceiver = new PdChatIMsgSendReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.broadcasereceiver.PdChatIMsgSendReceiver");
        MyApplication.getContext().registerReceiver(mPdChatIMsgSendReceiver, intentFilter);
        //因为这里需要注入Message，所以不能在AndroidManifest文件中静态注册广播接收器
        mPdChatIMsgSendReceiver.setOnChatMsgClick(new PdChatIMsgSendReceiver.OnChatMsgClick() {

            @Override
            public void onLeftImageClickEvent(Context context) {
                Intent intent = FaradayActivity.Companion.builder("native2flutter", "", false).build(context);
                context.startActivity(intent);
            }
        });
    }

    /**
     * 初始化sdk
     *只在Application 里调用一次
     * @param view
     */
    public void initQIMSdk(View view) {
        toast("已初始化");
    }

    /**
     * 初始化导航配置
     *
     * @param view
     */
    public void configNavigation(View view) {
        String url = "http://113.250.15.69:5203/newapi/nck/qtalk_nav.qunar";//导航URl
        if(TextUtils.isEmpty(url)){
            toast("请配置正确的导航地址");
            return;
        }
        QIMSdk.getInstance().setNavigationUrl(url);
        toast("导航配置成功");
        logcat_text.append("导航地址：" + url + "\n");
    }

    /**
     * 登录
     *
     * @param view
     */
    public void login(View view) {
        if (!QIMSdk.getInstance().isConnected()){
            final ProgressDialog pd = ProgressDialog.show(this, "提示", "正在登录中。。。");
            final String uid = "por13881980873";//用户名
            final String password = "0ef3e16f4356";//密码
            QIMSdk.getInstance().login(uid, password, (b, s) -> {
                logcat_text.append("Uid：" + uid + "\n" + "Password：" + password);
                pd.dismiss();
                autoLoginButton.setText(s);
                toast(s);
            });
        }
        else
            toast("已登录！");
    }

    /**
     * 普通二人会话
     * @param view
     */
    public void goToChat(View view){
        List<Nick> fList = ConnectionUtil.getInstance().SelectFriendList();
        for (Nick nick : fList) {
            Log.e("MainActivity", "goToChat: " + nick.getName());
        }
        QIMSdk.getInstance().goToChatConv(this,"ba14536ee3b011eabf010242ac110002",0);
    }

    /**
     * 群会话
     * @param view
     */
    public void goToGroup(View view){
        NotifyUtilTest.sendSimpleNotification(this);
    }

    public void startMainActivity(View view) {

        startActivity(new Intent(this, TabMainActivity.class));
    }

    /**
     * 会话页
     * @param view
     */
    public void startConversationActivity(View view){
        new RouterUtil().start2newHome(this);
    }

    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }
}