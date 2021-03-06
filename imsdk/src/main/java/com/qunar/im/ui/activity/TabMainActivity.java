package com.qunar.im.ui.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.qunar.im.base.common.ConversitionType;
import com.qunar.im.base.jsonbean.AtInfo;
import com.qunar.im.base.jsonbean.ExtendMessageEntity;
import com.qunar.im.base.jsonbean.LogInfo;
import com.qunar.im.base.module.Nick;
import com.qunar.im.base.protocol.NativeApi;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.base.shortutbadger.ShortcutBadger;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.FileUtils;
import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.base.util.ListUtil;
import com.qunar.im.base.util.Utils;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.core.manager.IMNotificaitonCenter;
import com.qunar.im.core.services.FeedBackServcie;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.core.utils.GlobalConfigManager;
import com.qunar.im.log.LogConstans;
import com.qunar.im.log.LogService;
import com.qunar.im.log.QLog;
import com.qunar.im.permission.PermissionCallback;
import com.qunar.im.permission.PermissionDispatcher;
import com.qunar.im.protobuf.Event.ConnectionErrorEvent;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.protobuf.common.LoginType;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.dispatch.DispatchHelper;
import com.qunar.im.thirdpush.core.QPushClient;
import com.qunar.im.ui.R;
import com.qunar.im.ui.broadcastreceivers.ConnectionStateReceiver;
import com.qunar.im.ui.broadcastreceivers.ShareReceiver;
import com.qunar.im.ui.fragment.BuddiesFragment;
import com.qunar.im.ui.fragment.ConversationFragment;
import com.qunar.im.ui.fragment.MineFragment;
import com.qunar.im.ui.fragment.WorkWorldFragment;
import com.qunar.im.ui.presenter.ILoginPresenter;
import com.qunar.im.ui.presenter.IMainPresenter;
import com.qunar.im.ui.presenter.factory.LoginFactory;
import com.qunar.im.ui.presenter.impl.MainPresenter;
import com.qunar.im.ui.presenter.views.ILoginView;
import com.qunar.im.ui.presenter.views.IMainView;
import com.qunar.im.ui.schema.QOpenHomeTabImpl;
import com.qunar.im.ui.services.PullPatchService;
import com.qunar.im.ui.services.PushServiceUtils;
import com.qunar.im.ui.util.NotificationUtils;
import com.qunar.im.ui.util.ParseErrorEvent;
import com.qunar.im.ui.util.QRRouter;
import com.qunar.im.ui.util.ReflectUtil;
import com.qunar.im.ui.util.UpdateManager;
import com.qunar.im.ui.view.CommonDialog;
import com.qunar.im.ui.view.HomeMenuPopWindow;
import com.qunar.im.ui.view.OnDoubleClickListener;
import com.qunar.im.ui.view.QtNewActionBar;
import com.qunar.im.ui.view.progressbarview.NumberProgressBar;
import com.qunar.im.ui.view.tableLayout.CommonTabLayout;
import com.qunar.im.ui.view.tableLayout.bean.TabEntity;
import com.qunar.im.ui.view.tableLayout.listener.CustomTabEntity;
import com.qunar.im.ui.view.tableLayout.listener.OnTabSelectListener;
import com.qunar.im.ui.view.tableLayout.utils.ViewFindUtils;
import com.qunar.im.ui.view.zxing.activity.CaptureActivity;
import com.qunar.im.utils.AppFrontBackHelper;
import com.qunar.im.utils.ConnectionUtil;
import com.qunar.im.utils.HttpUtil;
import com.qunar.im.utils.QtalkStringUtils;
import com.qunar.im.utils.UrlCheckUtil;
import com.qunar.im.base.protocol.NativeApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qunar.im.common.CommonConfig.globalContext;
import static com.qunar.im.ui.activity.ShareWorkWorldRouteActivity.WORKWORLDSHARE;

/**
 * Created by hubin on 2017/12/18.
 * ?????????ui ?????????MainActivity
 */

public class TabMainActivity extends IMBaseActivity implements PermissionCallback, IMainView, FeedBackServcie.Callback {


    private static final int CHECK_UPDATE = PermissionDispatcher.getRequestCode();
    private static final int SCAN_REQUEST = PermissionDispatcher.getRequestCode();
    private static final int LOCATION_REQUIRE = PermissionDispatcher.getRequestCode();

    private boolean isWorkWorldShare = false;

    //??????
    private LinearLayout feed_layout;
    private TextView feed_text;
    private NumberProgressBar feed_progress_bar;

    //tab?????????
    private CommonTabLayout mCommonTabLayou;
    //??????fragment?????????viewpager
    private ViewPager mViewPager;
    //adaptr
    private FragmentStatePagerAdapter mAdapter;
    //????????????list
    private ArrayList<CustomTabEntity> mTabEntities = new ArrayList<>();
    //fragment List
    private ArrayList<Fragment> mFragments = new ArrayList<>();

    private ConnectionUtil connectionUtil;
    //???????????????
    private IMainPresenter mMainPresenter;
    //??????????????????
    private ILoginPresenter loginPresenter;

    private boolean startActivity;

    private ConversationFragment conversationFragment;

    private long refreshTimestamp = 0;

    private long refreshInterval = 60 * 5 * 1000;

    private WorkWorldFragment workWorldFragment;

    private String[] mTitles;
//    private int[] mTitleUnSelectIcons = {R.string.atom_ui_new_message_unselect, R.string.atom_ui_new_contact_unselect, R.string.atom_ui_new_found_unselect, R.string.atom_ui_new_my_unselect};
//    private int[] mTitleSelectIcons = {R.string.atom_ui_new_message_select, R.string.atom_ui_new_contact_select, R.string.atom_ui_new_found_select, R.string.atom_ui_new_my_select};
//
//    private int[] mTitleUnSelectIconsQtalk = {R.string.atom_ui_new_message_unselect, R.string.atom_ui_new_calendar_unselect, R.string.atom_ui_new_contact_unselect, R.string.atom_ui_new_found_unselect, R.string.atom_ui_new_my_unselect};
//    private int[] mTitleSelectIconsQtalk = {R.string.atom_ui_new_message_select, R.string.atom_ui_new_calendar_select, R.string.atom_ui_new_contact_select, R.string.atom_ui_new_found_select, R.string.atom_ui_new_my_select};


    private int[] mTitleUnSelectIcons = {R.string.atom_ui_new_message_tab, R.string.atom_ui_new_contact_tab, R.string.atom_ui_new_found_tab, R.string.atom_ui_new_mind_tab};
    private int[] mTitleSelectIcons = {R.string.atom_ui_new_message_tab, R.string.atom_ui_new_contact_tab, R.string.atom_ui_new_found_tab, R.string.atom_ui_new_mind_tab};

    private int[] mTitleUnSelectIconsQtalk = {R.string.atom_ui_new_message_tab, R.string.atom_ui_new_contact_tab, R.string.atom_ui_new_found_tab, R.string.atom_ui_new_workworld_tab, R.string.atom_ui_new_mind_tab};
    private int[] mTitleSelectIconsQtalk = {R.string.atom_ui_new_message_tab, R.string.atom_ui_new_contact_tab, R.string.atom_ui_new_found_tab, R.string.atom_ui_new_workworld_tab, R.string.atom_ui_new_mind_tab};



    private boolean noticeShow, OPSShow = false;

    private boolean isShowWorkWorld = true;

    private HomeMenuPopWindow homeMenuPopWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atom_ui_activity_tabmainactivity);
        connectionUtil = ConnectionUtil.getInstance();

        isShowWorkWorld = ConnectionUtil.getInstance().SelectWorkWorldPremissions();
        //?????????????????????,???????????????
        LruCache<String,String> markups = ConnectionUtil.getInstance().selectMarkupNames();
        CurrentPreference.getInstance().setMarkupNames(markups);
        if (GlobalConfigManager.isQtalkPlat() && isShowWorkWorld) {
            mTitles = getResources().getStringArray(R.array.atom_ui_tab_title_qtalk);
        } else {
            mTitles = getResources().getStringArray(R.array.atom_ui_tab_title);
        }


//        initAction();
        initViewPager();
        initView();
        initActionBar();
        initMyCommTabLayout();

        initData();

    }

    private void initData() {
        loginPresenter = LoginFactory.createLoginPresenter();
        mMainPresenter = new MainPresenter(this);

        loginPresenter.setLoginView(new ILoginView() {
            @Override
            public String getUserName() {
                return CurrentPreference.getInstance().getPreferenceUserId();
            }

            @Override
            public String getPassword() {
                return CurrentPreference.getInstance().getToken();
            }

            @Override
            public void setLoginResult(final boolean success, int errcode) {
                if (!success) {
                    startLoginView();
                } else {
                    if (commonDialog != null && commonDialog.isShowing()) {
                        commonDialog.dismiss();
                    }
                }
            }

            @Override
            public boolean isSwitchAccount() {
                return false;
            }

            @Override
            public String getPrenum() {
                return "";
            }

            @Override
            public Context getContext() {
                return getApplicationContext();
            }

            @Override
            public void getVirtualUserRole(boolean b) {
                if (b) {
                    getHandler().post(() -> {
                        Logger.i("??????????????????");
                    });

                }

            }

            @Override
            public void setHeaderImage(final Nick nick) {
                runOnUiThread(() -> {
//                        myActionBar.getSelfGravatarImage().setImageUrl(nick.getHeaderSrc(), true);
                });

            }

            @Override
            public void LoginFailure(int str) {
                if (TabMainActivity.this.isFinishing()) {
                    return;
                }
                if (commonDialog != null && commonDialog.isShowing()) {
                    commonDialog.dismiss();
//                    return;
                }

                if (str == ConnectionErrorEvent.fire) {//????????????
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//??????????????????
                        ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
                    }
                }

                commonDialog.setTitle(getString(R.string.atom_ui_tip_dialog_prompt));
                commonDialog.setMessage(getString(R.string.atom_ui_tip_login_failed) + ParseErrorEvent.getError(str, TabMainActivity.this));
                commonDialog.setPositiveButton(getString(R.string.atom_ui_common_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConnectionUtil.clearLastUserInfo();
                        setLoginResult(false, 0);

                        dialog.dismiss();
                    }
                });
                commonDialog.setCancelable(false);
                commonDialog.create().show();
//                commonDialog = dialog.show();
            }

            @Override
            public void connectInterrupt() {
                runOnUiThread(() -> setErrorTitle(getString(R.string.atom_ui_tip_disconnected)));
            }

            @Override
            public void noNetWork() {
                runOnUiThread(() -> setErrorTitle(getString(R.string.atom_ui_tip_disconnected)));
            }

            @Override
            public void tryToConnect(final String str) {
                runOnUiThread(() -> setErrorTitle(getString(R.string.atom_ui_tip_connecting)));
            }
        });
        handleShareAction(getIntent());

        injectExtra(getIntent());

        initActivityLifeCycle();

        File groupGravatar = new File(FileUtils.getExternalFilesDir(globalContext), "gravatar");
        if (!groupGravatar.exists()) {
            groupGravatar.mkdirs();
        }

        checkNotificationDialog();

    }

    void scanQrCode() {
        Intent scanQRCodeIntent = new Intent(getApplicationContext(), CaptureActivity.class);
        startActivityForResult(scanQRCodeIntent, SCAN_REQUEST);
    }


    @Override
    public void onBackPressed() {
        Utils.jump2Desktop(TabMainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent clearIntent = new Intent();
        clearIntent.setAction("com.qunar.ops.push.CLEAR_NOTIFY");
        clearIntent.setPackage(this.getApplicationContext().getPackageName());
        Utils.sendLocalBroadcast(clearIntent, this.getApplicationContext());

        //push??????
        QPushClient.clearNotification(this);

        regiestNetWorkChangeListener();

        //????????????????????????
        mMainPresenter.getUnreadConversationMessage();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handleLogin();
    }

    private void handleLogin() {
        if (startActivity) {
            startActivity(getIntent());
            startActivity = false;
        }
    }

    ConnectionStateReceiver connectionStateReceiver;

    private void regiestNetWorkChangeListener() {
        if (connectionStateReceiver == null)
            connectionStateReceiver = new ConnectionStateReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(connectionStateReceiver, filter);
    }

    public synchronized void login() {
        //???????????????????????????????????????true
        //TODO qchatv7.20 bug ??????????????????qvt???????????? ?????????????????????qvt?????????
        if (!CommonConfig.isQtalk && TextUtils.isEmpty(DataUtils.getInstance(this).getPreferences(Constants.Preferences.qchat_qvt, ""))) {
            IMUserDefaults.getStandardUserDefaults().newEditor(this).removeObject(Constants.Preferences.usertoken).synchronize();
        }
        boolean autoLogin = CurrentPreference.getInstance().isAutoLogin();
        if (autoLogin) {
            Logger.i("??????????????????????????????:" + connectionUtil.isCanAutoLogin());
            if (!connectionUtil.isCanAutoLogin()) {
                startLoginView();
                return;
            } else {
//                presenter.loadPreference(this, false);
                loginPresenter.autoLogin();
            }
        } else {
            startLoginView();
        }
    }


    public void setTabViewUnReadCount(int count) {
        if (isFinishing()) {
            return;
        }
        if (count > 0) {
            mCommonTabLayou.showMsg(0, count);
        } else {
            mCommonTabLayou.hideMsg(0);
        }

//        TextView view = (TextView) tab.getTabAt(0).findViewById(R.id.textView_new_msg);
    }


    public void setTabViewWorkWorldRed(boolean notice, int count) {

        if (GlobalConfigManager.isQtalkPlat() && isShowWorkWorld) {
            if (notice) {
//                if (CommonConfig.isQtalk) {
                mCommonTabLayou.showMsg(3, count);
//                } else {
//                    mCommonTabLayou.showMsg(1, 0);
//                }

            } else {
//                if (CommonConfig.isQtalk) {
                mCommonTabLayou.hideMsg(3);
//                } else {
//                    mCommonTabLayou.hideMsg(1;
//                }

//                mCommonTabLayou.showMsg(2, 0);
            }
        } else {
//            if (CommonConfig.isQtalk) {
            mCommonTabLayou.hideMsg(3);
//            } else {
//                mCommonTabLayou.hideMsg(1);
//            }

        }
    }

    public void setTabViewFindRed(boolean ops) {
        if ("ejabhost1".equals(QtalkNavicationService.getInstance().getXmppdomain())
                || "ejabhost2".equals(QtalkNavicationService.getInstance().getXmppdomain())) {
            if (ops) {
                if (CommonConfig.isQtalk) {
                    mCommonTabLayou.showMsg(2, 0);
                } else {
                    mCommonTabLayou.showMsg(1, 0);
                }

            } else {
                if (CommonConfig.isQtalk) {
                    mCommonTabLayou.hideMsg(2);
                } else {
                    mCommonTabLayou.hideMsg(1);
                }

//                mCommonTabLayou.showMsg(2, 0);
            }
        } else {
            if (CommonConfig.isQtalk) {
                mCommonTabLayou.hideMsg(2);
            } else {
                mCommonTabLayou.hideMsg(1);
            }

        }
    }

    private void checkUpdate() {
        PermissionDispatcher.
                requestPermissionWithCheck(this, new int[]{PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE,
                                PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE}, this,
                        CHECK_UPDATE);
    }


    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        //??????
        handleShareAction(intent);
        injectExtra(intent);

        handleLogin();

        //????????????
        if (!connectionUtil.isLoginStatus() || !connectionUtil.isConnected()) {
            login();
        }

    }

    /**
     * ??????????????????
     */
    private void checkNotificationDialog() {
        //????????????
        boolean isNoneedCheck = DataUtils.getInstance(this).getPreferences("CheckNotification", false);
        //??????????????????????????????
        long lastCheckTime = DataUtils.getInstance(this).getPreferences("lastCheckTime", 0);
        if (isNoneedCheck && lastCheckTime - System.currentTimeMillis() > 1000 * 60 * 60 * 24 * 7) {
            //??????????????????????????????????????????????????????
            DataUtils.getInstance(TabMainActivity.this).putPreferences("CheckNotification", false);
        }
        if (!NotificationUtils.areNotificationsEnabled(this) && !isNoneedCheck) {
            CommonDialog.Builder remindDialog = new CommonDialog.Builder(this);
            remindDialog.setTitle(getString(R.string.atom_ui_tip_dialog_prompt));
            remindDialog.setMessage(getString(R.string.atom_ui_open_notification_switch));
            remindDialog.setPositiveButton(getString(R.string.atom_ui_setting_title), (dialog, which) -> {
                dialog.dismiss();
//                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS, Uri.parse("package:" + MainActivity.this.getPackageName()));
//                    startActivity(intent);
                NotificationUtils.startNotificationSettings(TabMainActivity.this);

            });
            remindDialog.setNeutralButton(getString(R.string.atom_ui_btn_not_remind), (dialog, which) -> {
                DataUtils.getInstance(TabMainActivity.this).putPreferences("CheckNotification", true);
                DataUtils.getInstance(TabMainActivity.this).putPreferences("lastCheckTime", System.currentTimeMillis());
                dialog.dismiss();
            });
            remindDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), (dialog, which) -> dialog.dismiss());
            remindDialog.create().show();
        }
    }

    private void injectExtra(Intent intent) {
        if (intent == null) return;
        Uri uri = intent.getData();
        Logger.i("injectExtra  uri = " + uri);
        Logger.i("injectExtra  intent = " + intent.getExtras());
        if (uri != null) {
            String jid = uri.getQueryParameter("jid");
            if (!TextUtils.isEmpty(jid) && !jid.equals("null")) {
                if (jid.equals("headline")) {
                    intent.setClass(this, RobotExtendChatActivity.class);
                    intent.putExtra(PbChatActivity.KEY_JID, Constants.SYS.SYSTEM_MESSAGE);
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, Constants.SYS.SYSTEM_MESSAGE);
                    startActivity = true;
                } else {
                    boolean isFromChatRoom = jid.contains("@conference");
                    intent.setClass(this, PbChatActivity.class);
                    intent.putExtra(PbChatActivity.KEY_JID, jid);
                    intent.putExtra(PbChatActivity.KEY_IS_CHATROOM,
                            isFromChatRoom);
                    startActivity = true;
                }
            }
        } else if (intent.getExtras() != null) {//??????????????????push??????
            String jid = intent.getExtras().getString("jid");
            int type = 0;
            Object obj = intent.getExtras().get("type");
            if(obj != null) {
                type = Integer.valueOf(intent.getExtras().get("type").toString());
            }
            if (!TextUtils.isEmpty(jid) && !jid.equals("null") && type >= 0) {
                if (type == ProtoMessageOuterClass.SignalType.SignalTypeHeadline_VALUE) {
                    intent.setClass(this, RobotExtendChatActivity.class);
                    intent.putExtra(PbChatActivity.KEY_JID, Constants.SYS.SYSTEM_MESSAGE);
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, Constants.SYS.SYSTEM_MESSAGE);
                    startActivity = true;
                } else if ((type == ProtoMessageOuterClass.SignalType.SignalTypeChat_VALUE
                        || type == ProtoMessageOuterClass.SignalType.SignalTypeGroupChat_VALUE
                        || type == ProtoMessageOuterClass.SignalType.SignalTypeConsult_VALUE)
                        && jid.contains("@")) {
                    int converType = 0;
//                    if(ConnectionUtil.getInstance().isHotline(jid)) {
//                        converType = ConversitionType.MSG_TYPE_CONSULT;
//                    } else {
                    String chatid = intent.getExtras().getString("chatid");
                    converType = ConversitionType.getConversitionType(type, chatid);
                    if (converType == ConversitionType.MSG_TYPE_CONSULT) {
                        converType = ConversitionType.MSG_TYPE_CONSULT_SERVER;
                        intent.putExtra(PbChatActivity.KEY_REAL_JID, intent.getExtras().getString("realjid"));
                    } else if (converType == ConversitionType.MSG_TYPE_CONSULT_SERVER) {
                        converType = ConversitionType.MSG_TYPE_CONSULT;
                    }
//                    }
                    intent.putExtra(PbChatActivity.KEY_CHAT_TYPE, converType + "");

                    boolean isFromChatRoom = jid.contains("@conference");
                    intent.setClass(this, PbChatActivity.class);
                    intent.putExtra(PbChatActivity.KEY_JID, jid);
                    intent.putExtra(PbChatActivity.KEY_IS_CHATROOM,
                            isFromChatRoom);
                    startActivity = true;
                }
            }
        }

        //???????????????tab
        String key = intent.getStringExtra(Constants.BundleKey.HOME_TAB);
        int index = QOpenHomeTabImpl.getInstance().getTabIndex(this, key, mTitles);
        if (index != -1 && mCommonTabLayou.getCurrentTab() != index) {
            if(!isWorkWorldShare){
                mViewPager.setCurrentItem(index, false);
            }

        }

        //??????shortcut???????????? ??????
        if (intent.getBooleanExtra(Constants.BundleKey.IS_SHORTCUT_SCAN, false)) {
            PermissionDispatcher.requestPermissionWithCheck(TabMainActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA}, TabMainActivity.this, SCAN_REQUEST);
        } else if (intent.getBooleanExtra(Constants.BundleKey.IS_SHORTCUT_SEARCH, false)) {
            startSearchActivity();
        }
    }

    int titileClickCount;

    private void initActionBar() {
        mNewActionBar = (QtNewActionBar) findViewById(R.id.my_new_action_bar);
        setNewActionBar(mNewActionBar);
        setActionBarLeftClick(null);
        setActionBarRightIconSize(28);
        setActionBarRightSpecialIconSize(22);
//        setActionBarSingleTitle("QTalk");
        mNewActionBar.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                if (conversationFragment != null) conversationFragment.moveToTop();
            }

            @Override
            public void onSingleClick() {

            }
        }));
        mNewActionBar.getTextTitle().setOnClickListener(v -> {
            titileClickCount++;
            if (titileClickCount > 10) {
                titileClickCount = 0;
                connectionUtil.resetUnreadCount();
                toast("unread count reseted");
            }
        });
    }

    private void initAction() {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.hide();
    }

    /**
     * ?????????vewpager????????????
     */
    private void initViewPager() {
        mFragments.clear();
        conversationFragment = new ConversationFragment();
        mFragments.add(conversationFragment);


        //???????????????rn????????????????????????
        if (QtalkNavicationService.getInstance().getNavConfigResult().RNAndroidAbility.RNContactView) {
            mFragments.add(ReflectUtil.getRNContactsFragment());
        } else {
            mFragments.add(new BuddiesFragment());
        }

        if (!GlobalConfigManager.isStartalkPlat() && ("ejabhost1".equals(QtalkNavicationService.getInstance().getXmppdomain())
                || "ejabhost2".equals(QtalkNavicationService.getInstance().getXmppdomain()))) {
            mFragments.add(getDiscoverFragment());

        } else {
            mFragments.add(ReflectUtil.getRNFoundFragment());
        }

        //?????????qtalk ???????????????
        Logger.i("?????????????????????????????????" + GlobalConfigManager.isQtalkPlat());
        workWorldFragment = new WorkWorldFragment();
        if (GlobalConfigManager.isQtalkPlat() && isShowWorkWorld) {
//            mFragments.add(new RNCalendarFragment());
            workWorldFragment.setOnRefresh(time -> refreshTimestamp = time);
            mFragments.add(workWorldFragment);
        }

        //???????????????rn????????????????????????
        if (QtalkNavicationService.getInstance().getNavConfigResult().RNAndroidAbility.RNMineView) {
            mFragments.add(ReflectUtil.getRNMineFragment());
        } else {
            mFragments.add(new MineFragment());
        }


    }

    /**
     * ????????????ops????????????
     *
     * @return
     */
    private Fragment getDiscoverFragment() {
        try {
            Class<?> cls = Class.forName("com.qunar.im.camelhelp.DiscoverFragment");
            return (Fragment) cls.newInstance();
        } catch (Exception e) {

        }
        return new Fragment();
    }

    /**
     * ?????????view
     */
    private void initView() {
        feed_layout = (LinearLayout) findViewById(R.id.feed_layout);
        feed_text = (TextView) findViewById(R.id.feed_text);
        feed_progress_bar = (NumberProgressBar) findViewById(R.id.feed_progress_bar);
        mCommonTabLayou = (CommonTabLayout) findViewById(R.id.tab_common_tablayout);


        for (int i = 0; i < mTitles.length; i++) {
            if (CommonConfig.isQtalk) {
                mTabEntities.add(new TabEntity(mTitles[i], mTitleSelectIconsQtalk[i], mTitleUnSelectIconsQtalk[i]));
            } else {
                mTabEntities.add(new TabEntity(mTitles[i], mTitleSelectIcons[i], mTitleUnSelectIcons[i]));
            }

        }
        View mDecorView = getWindow().getDecorView();
        mViewPager = ViewFindUtils.find(mDecorView, R.id.tab_fragment_viewpager);
        mAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mAdapter);

    }

    /**
     * ?????????commtablayou?????????
     */
    private void initMyCommTabLayout() {
        mCommonTabLayou.setTabData(mTabEntities);
        mCommonTabLayou.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(final int position) {
//                mViewPager.setCurrentItem(position);
                mViewPager.setCurrentItem(position, false);
                saveTabClickLog(position);
            }

            @Override
            public void onTabReselect(int position) {
            }
        });

        View view = mCommonTabLayou.getTabView(0);

        view.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
                Logger.i("MoveToUnread:click");
                conversationFragment.MoveToUnread();
            }

            @Override
            public void onSingleClick() {

            }
        }));

//


        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCommonTabLayou.setCurrentTab(position);
                setTitleContentByIndex(position);


                //????????????????????????????????????
                if (GlobalConfigManager.isQtalkPlat() && isShowWorkWorld) {
                    if(position==3){



                        long i = System.currentTimeMillis();
                        if ((i - refreshTimestamp) > refreshInterval) {
                            if(workWorldFragment!=null){
                                workWorldFragment.refresh();
                            }
                            refreshTimestamp = i;
                        }
                    }
                    if(workWorldFragment!=null){
                        workWorldFragment.workworldcloseRefresh();
                    }

                }


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
//        mCommonTabLayou.setMsgMargin(0,-10,0);

        mViewPager.setOffscreenPageLimit(4);

        setTitleContentByIndex(0);

        //?????????????????????????????? ???????????????
        setActionBarTitle(getString(R.string.atom_ui_tip_disconnected));
    }

    public void startSearchActivity() {
        if (CommonConfig.isQtalk) {
            Intent i = ReflectUtil.getQTalkSearchActivityIntent(TabMainActivity.this);
            if(i == null){
                return;
            }
            startActivity(i);
        } else {
            Intent intent = new Intent(TabMainActivity.this, SearchUserActivity.class);
            startActivity(intent);
        }
        saveHomeActLog("search", "??????", "???????????????");
    }

    View.OnClickListener homePopClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(homeMenuPopWindow != null && homeMenuPopWindow.isShowing()){
                homeMenuPopWindow.dismiss();
            }
            if(v.getId() == R.id.atom_ui_home_pop_unread){
                NativeApi.openUnReadListActivity();
            }else if(v.getId() == R.id.atom_ui_home_pop_scan){
                PermissionDispatcher.requestPermissionWithCheck(TabMainActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA}, TabMainActivity.this, SCAN_REQUEST);
                saveHomeActLog("RichScan", "?????????", "????????????????????????????????????");
            }else if(v.getId() == R.id.atom_ui_home_pop_create_group){
                NativeApi.openCreateGroup();
            }else if(v.getId() == R.id.atom_ui_home_pop_readed){
                //????????????????????????at
                Map<String, List<AtInfo>> atMap = connectionUtil.getAtMessageMap();
                if (atMap != null) {
                    atMap.clear();
                }
                DispatchHelper.Async("OneKeyRead", () -> {
                    connectionUtil.setAllMsgRead();
                    mMainPresenter.getUnreadConversationMessage();
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Show_List);
                });
            }
        }
    };

    @SuppressLint("RestrictedApi")
    private void setTitleContentByIndex(int position) {
        switch (position) {
            case 0:
                setActionBarSingleTitle(mTitles[mViewPager.getCurrentItem()]);
                setActionBarRightIcon(R.string.atom_ui_new_add);
                setActionBarLeftIcon(false);
                setActionBarRightIconClick(v -> {
                    homeMenuPopWindow = new HomeMenuPopWindow(TabMainActivity.this,homePopClickListener);
                    homeMenuPopWindow.showHomePop(mNewActionBar);
                });

                setActionBarRightSpecial(0);
                setActionBarTitleDoubleClick(null);
                break;
            case 1:
            case 2:
                //??????qtalk????????? ???????????????????????????
                setActionBarTitleDoubleClick(null);
                setActionBarRightSpecial(0);
                setActionBarRightIcon(0);
                setActionBarSingleTitle(mTitles[mViewPager.getCurrentItem()]);
            case 3:
            case 4:
                setActionBarTitleDoubleClick(null);
                setActionBarRightSpecial(0);
                setActionBarSingleTitle(mTitles[mViewPager.getCurrentItem()]);
                setActionBarRightIcon(R.string.atom_ui_new_setting_six_edge);
                setActionBarRightIconClick(v -> NativeApi.openMyRnSetting());
                setActionBarRightSpecial(0);
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

    }

    /**
     * ??????pagerview?????????
     */
    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mFragments.size();
//            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);

        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    public void showMyInfo() {
        String userId = CurrentPreference.getInstance().getPreferenceUserId();
        Intent intent = new Intent(this, PersonalInfoMyActivity.class);
        intent.putExtra("jid", QtalkStringUtils.userId2Jid(userId));
        startActivity(intent);
    }

    public void showAccountInfo() {
        try {
            Class classHyMain = Class.forName("com.qunar.im.camelhelp.HyMainActivity");
            Intent i = new Intent(this, classHyMain);
            i.putExtra("module", "user-info");
            i.putExtra("data", "");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (ClassNotFoundException e) {

        }
    }

    public void showSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.atom_ui_slide_right, R.anim.atom_ui_slide_right);
    }

    public void showClockIn() {
//        Intent intent  = new Intent(this, QtalkServiceRNActivity.class);
//        startActivity(intent);
        checkLocation();
    }

    public void startClockIn() {
        Intent intent = ReflectUtil.getQtalkServiceRNActivityIntent(this);
        if(intent == null){
            return;
        }
        intent.putExtra("module", Constants.RNKey.CLOCKIN);
        startActivity(intent);
    }

    public void showTOTP() {
        Intent intent = ReflectUtil.getQtalkServiceRNActivityIntent(this);
        if(intent == null){
            return;
        }
        intent.putExtra("module", Constants.RNKey.TOTP);
        startActivity(intent);
    }

    public void gotoNote() {
        Intent intent = new Intent(this, DailyMindActivity.class);
        startActivity(intent);
    }

    /**
     * ????????????
     */
    private void checkLocation() {
        PermissionDispatcher.requestPermissionWithCheck(this, new int[]{PermissionDispatcher.REQUEST_ACCESS_COARSE_LOCATION,
                        PermissionDispatcher.REQUEST_ACCESS_FINE_LOCATION}, this,
                LOCATION_REQUIRE);
    }


    public void showHongBao() {
        if (!TextUtils.isEmpty(QtalkNavicationService.MY_HONGBAO)) {
            Uri uri = Uri.parse(QtalkNavicationService.MY_HONGBAO);
            Intent intent = new Intent(this, QunarWebActvity.class);
            intent.putExtra(Constants.BundleKey.WEB_FROM, Constants.BundleValue.HONGBAO);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    public void showHongBaoBalance() {
        if (!TextUtils.isEmpty(QtalkNavicationService.HONGBAO_BALANCE)) {
            Uri uri = Uri.parse(QtalkNavicationService.HONGBAO_BALANCE);
            Intent intent = new Intent(this, QunarWebActvity.class);
            intent.putExtra(Constants.BundleKey.WEB_FROM, Constants.BundleValue.HONGBAO);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    public void showFeedBack() {
        Intent intent = new Intent(this, BugreportActivity.class);
        startActivity(intent);
    }

    @Override
    public void startLoginView() {
        if (commonDialog != null && commonDialog.isShowing()) {
            commonDialog.dismiss();
        }
        if (!CommonConfig.loginViewHasShown) {
            if (CommonConfig.isQtalk) {
                if (!LoginType.SMSLogin.equals(QtalkNavicationService.getInstance().getLoginType()) || GlobalConfigManager.isStartalkPlat()) {
                    startActivity(new Intent(this, QTalkUserLoginActivity.class));
                } else {
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                }
            } else {
                Intent intent = new Intent(this, QChatLoginActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
//        EventBus.getDefault().unregister(handleMainEvent);
        super.onDestroy();
        if (connectionStateReceiver != null)
            unregisterReceiver(connectionStateReceiver);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                if (!TextUtils.isEmpty(data.getStringExtra("content"))) {
                    String content = data.getStringExtra("content");
                    QRRouter.handleQRCode(content, this);
                }
            }
        }
    }


    @Override
    public void responsePermission(int requestCode, boolean granted) {
        if (!granted) return;
        if (requestCode == SCAN_REQUEST) {
            scanQrCode();
        } else if (requestCode == CHECK_UPDATE) {
            UpdateManager.getUpdateManager().checkAppUpdate(this, false);
            //?????????????????????
            PullPatchService.runPullPatchService(this);
        } else if (requestCode == LOCATION_REQUIRE) {
            startClockIn();
        }
    }

    @Override
    public void setUnreadConversationMessage(final int unreadNumbers) {
        getHandler().post(() -> setTabViewUnReadCount(unreadNumbers));
    }

    @Override
    public void loginSuccess() {
        //????????????????????????ui
        runOnUiThread(() -> {
            if (!isFinishing()) {
                setErrorTitle("");
                setActionBarTitle(mTitles[mViewPager.getCurrentItem()]);
            }
        });

        //?????????????????????push
        boolean isRegister = presenter.checkUnique();
        if (CurrentPreference.getInstance().isTurnOnPsuh()) {
            if (isRegister) {
                PushServiceUtils.stopAMDService(TabMainActivity.this);
            }
            PushServiceUtils.startAMDService(TabMainActivity.this);
        } else {
            PushServiceUtils.stopAMDService(TabMainActivity.this);
        }
        //????????????
        checkUpdate();
//        DiscoverFragment.clearBridge();
        //???????????????
//        DiscoverFragment.clearBridge();

//        RNContactsFragment.clearBridge();
//        RNFoundFragment.clearBridge();
//        RNMineFragment.clearBridge();
    }

    //???????????????
    @Override
    public void synchronousing() {
        runOnUiThread(() -> {
            if (!isFinishing())
                setErrorTitle(getString(R.string.atom_ui_tip_synchronizing));
        });
    }

    @Override
    public void refreshShortcutBadger(final int count) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            if (!getFragmentManager().isDestroyed()) {//fix IllegalStateException: Can't access ViewModels from
//                RNViewModel rnViewModel = ViewModelProviders.of(this).get("RnVM", RNViewModel.class);
//                rnViewModel.getUnreadCountLD().postValue(count);
//            }
//        }
        runOnUiThread(() -> {
            //????????????????????????????????????pushReceiver???????????????
            if (!Utils.isMIUI()) {
                boolean success = ShortcutBadger.applyCount(TabMainActivity.this, count);
                Logger.i("ShortcutBadger", "Set count=" + count + ", success=" + success);
            } else {
                Notification.Builder builder = new Notification.Builder(CommonConfig.globalContext);
                Notification notification = builder.build();
                //????????????????????????
//                    int total = ConnectionUtil.getInstance().SelectUnReadCount();
                ShortcutBadger.applyNotification(CommonConfig.globalContext.getApplicationContext(), notification, count);
            }
        });


    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void showDialog(String str) {

    }

    @Override
    public void refresh() {
        if (isFinishing()) {
            return;
        }
        if (CurrentPreference.getInstance().isSwitchAccount()) {

            //??????????????????,??????????????????????????? false
            CurrentPreference.getInstance().setSwitchAccount(false);
            if (GlobalConfigManager.isQtalkPlat()) {
                mFragments.remove(2);
                if ("ejabhost1".equals(QtalkNavicationService.getInstance().getXmppdomain())
                        || "ejabhost2".equals(QtalkNavicationService.getInstance().getXmppdomain())) {
                    mFragments.add(2, getDiscoverFragment());
                } else {
                    mFragments.add(2, ReflectUtil.getRNFoundFragment());
                }
            } else {
                mFragments.remove(2);
                if ("ejabhost1".equals(QtalkNavicationService.getInstance().getXmppdomain())
                        || "ejabhost2".equals(QtalkNavicationService.getInstance().getXmppdomain())) {
                    mFragments.add(2, getDiscoverFragment());
                } else {
                    mFragments.add(2, ReflectUtil.getRNFoundFragment());
                }
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void refreshOPSUnRead(final boolean isShow) {
        getHandler().post(() -> {
            OPSShow = isShow;
            setTabViewFindRed(OPSShow);
        });
    }

    @Override
    public void refreshNoticeRed(final boolean isShow, final int count) {
        if ("ejabhost1".equals(QtalkNavicationService.getInstance().getXmppdomain())) {

            getHandler().post(() -> {
                noticeShow = isShow;
                setTabViewWorkWorldRed(noticeShow, count);
//                    setTabViewFindRed(OPSShow, noticeShow);
            });

        }
    }

    @Override
    public void startOPS() {
    }

    @Override
    public void showProgress(String str) {
        showProgressDialog(str);
    }

    @Override
    public void dimissProgress() {
        dismissProgressDialog();
    }

    @Override
    public boolean getWorkWorldPermissions() {
        return isShowWorkWorld;
    }

    private void startReleaseCircle() {
        Intent intent = new Intent(TabMainActivity.this, WorkWorldActivity.class);
        startActivity(intent);
    }


    public void handleShareAction(Intent intent) {
        if (intent == null) return;

        //?????????????????????????????????????????????
         isWorkWorldShare = intent.getBooleanExtra(WORKWORLDSHARE, false);
        if (intent.hasExtra(ShareReceiver.SHARE_EXTRA_KEY)) {
            intent.setClass(this, SearchUserActivity.class);
            intent.putExtra(Constants.BundleKey.IS_TRANS, true);
            intent.putExtra(Constants.BundleKey.IS_FROM_SHARE, true);
//            if(intent.hasExtra(Constants.BundleKey.TRANS_MSG_JSON)){//??????DownloadActivity????????????
//                intent.putExtra(Constants.BundleKey.TRANS_MSG,JsonUtils.getGson().fromJson(intent.getStringExtra(Constants.BundleKey.TRANS_MSG_JSON), IMMessage.class));
//            }
            startActivity = true;
        } else {
            String title = "";
            String content = "";
            ArrayList<String> iconPaths = new ArrayList<String>();
            ArrayList<String> videoPaths = new ArrayList<String>();
            ArrayList<String> filePaths = new ArrayList<String>();
            if (intent.getStringExtra(Intent.EXTRA_SUBJECT) != null) {
                title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            }
            if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
                content = intent.getStringExtra(Intent.EXTRA_TEXT);
            }

            if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                final String type = intent.getType();
                if (type != null) {
                    if (type.startsWith("image/")) {
                        final ArrayList<Uri> icons = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                        if (icons != null) {
                            for (Uri icon : icons) {
                                final String imagePath = FileUtils.getPath(this, icon);
                                if (!TextUtils.isEmpty(imagePath)) {
                                    iconPaths.add(imagePath);
                                }
                            }
                        }
                    } else if (type.startsWith("video/")) {
                        final ArrayList<Uri> icons = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                        if (icons != null) {
                            for (Uri icon : icons) {
                                final String imagePath = FileUtils.getPath(this, icon);
                                if (!TextUtils.isEmpty(imagePath)) {
                                    videoPaths.add(imagePath);
                                }
                            }
                        }
                    }
                }
            } else if (Intent.ACTION_SEND.equals(intent.getAction())) {//??????????????????


                if (isWorkWorldShare) {
                    intent.setClass(this, WorkWorldReleaseCircleActivity.class);
                    runOnUiThread(() -> mViewPager.setCurrentItem(3,false));

                } else {
                    intent.setClass(this, SearchUserActivity.class);
                }

                // chrome??????????????????
                final Uri screenshot_as_stream = intent.getParcelableExtra("share_screenshot_as_stream");
                if (screenshot_as_stream != null) {
                    final String imagePath = FileUtils.getPath(this, screenshot_as_stream);
                    if (imagePath != null) {
                        iconPaths.add(imagePath);
                    }
                }
                // UC???????????????????????????
                final String file = intent.getStringExtra("file");
                if (file != null) {
                    final String imagePath = new File(file).getAbsolutePath();
                    iconPaths.add(imagePath);
                }

                final String type = intent.getType();
                if (type != null) {
                    if (type.startsWith("image")) {
                        final Uri icon = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        final String imagePath = FileUtils.getPath(this, icon);
                        if (imagePath != null) {
                            iconPaths.add(imagePath);
                        }
                    } else if (type.startsWith("video/")) {
                        final Uri icon = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (icon != null) {

                            final String imagePath = FileUtils.getPath(this, icon);

                            if (!TextUtils.isEmpty(imagePath)) {


//                                if(!ImageSelectUtil.checkVideo(imagePath)){
//                                    Toast.makeText(this,"??????????????????,????????????",Toast.LENGTH_LONG).show();
//                                    return;
//                                }
                                videoPaths.add(imagePath);
                            }

                        }
                    } else if (type.equals("text/x-vcard")) {
                        title = getString(R.string.atom_ui_textbar_button_share_card);
                        content = "";
                        if (intent.getExtras().containsKey("vnd.android.cursor.item/name")) {
                            ArrayList<String> names = intent.getExtras()
                                    .getStringArrayList("vnd.android.cursor.item/name");
                            if (!ListUtil.isEmpty(names))
                                content += getString(R.string.atom_ui_tip_contact_name) + "???" + names.get(0) + "\n";
                        }
                        if (intent.getExtras().containsKey("vnd.android.cursor.item/phone_v2")) {
                            ArrayList<String> phones = intent.getExtras()
                                    .getStringArrayList("vnd.android.cursor.item/phone_v2");
                            if (!ListUtil.isEmpty(phones))
                                content += getString(R.string.atom_ui_tip_contact_mobile) + ": " + phones.get(0);
                        }
                    } else {// if (type.equalsIgnoreCase("text/plain"))
                        Uri turi = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        if (turi != null) {
                            String filepath = FileUtils.getPath(this, turi);
                            if (!TextUtils.isEmpty(filepath)) {
                                filePaths.add(filepath);
                            }
                        }
                    }
                }
                //?????????????????????content???
                String url = intent.getStringExtra("url");//????????????
                if (TextUtils.isEmpty(url)) {
                    //??????content????????????url
                    try {
                        String regex = "(\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])";
//                        String regex = "\\b(http[s]{0,1}|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                        Pattern _pattern = Pattern.compile(regex);
                        Matcher _match = _pattern.matcher(content);
                        if (_match.find()) {
                            url = _match.group();
                            content.replace(url, "");
                        }
                    } catch (Exception e) {
                        Logger.e("??????url???????????????" + e.getMessage() + "  content=" + content);
                        url = "";
                    }
                }

                if (TextUtils.isEmpty(url)) {
                    if (!TextUtils.isEmpty(title) && !content.contains(title)) {
                        content = title + "\n\r" + content;
                    }
                    intent.putExtra(ShareReceiver.SHARE_TAG, true);
                } else {
                    ExtendMessageEntity entity = new ExtendMessageEntity();
                    entity.title = title;
                    entity.linkurl = url;
                    entity.img = UrlCheckUtil.getIconUrlString(url);
                    entity.desc = content;
                    String jsonStr = JsonUtils.getGson().toJson(entity);
                    intent.putExtra(ShareReceiver.SHARE_EXTRA_KEY, jsonStr);
                }

                intent.putExtra(ShareReceiver.SHARE_TEXT, content);
                if (!ListUtil.isEmpty(iconPaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_IMG, iconPaths);
                }

                if (!ListUtil.isEmpty(videoPaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_VIDEO, videoPaths);
                }
                if (!ListUtil.isEmpty(filePaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_FILE, filePaths);
                }

                intent.putExtra(Constants.BundleKey.IS_TRANS, true);
                intent.putExtra(Constants.BundleKey.IS_FROM_SHARE, true);
                startActivity = true;
            } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {//??????????????????????????????qtalk????????????
                Uri uri = intent.getData();
                if (uri == null) {
                    return;
                }
                if (CommonConfig.schema.equalsIgnoreCase(uri.getScheme())) {
                    return;
                }
                final String type = intent.getType();
                if (TextUtils.isEmpty(type)) {
                    String filepath = FileUtils.getPath(this, uri);
                    if (!TextUtils.isEmpty(filepath)) {
                        filePaths.add(filepath);
                    }
                } else {
                    if (type.startsWith("image")) {
                        String imagePath = FileUtils.getPath(this, uri);
                        if (!TextUtils.isEmpty(imagePath)) {
                            iconPaths.add(imagePath);
                        }
                    } else if (type.startsWith("video/")) {
                        final String videoPath = FileUtils.getPath(this, uri);
                        if (!TextUtils.isEmpty(videoPath)) {
                            videoPaths.add(videoPath);
                        }
                    } else {// if (type.equalsIgnoreCase("text/plain"))
                        String filepath = FileUtils.getPath(this, uri);
                        if (!TextUtils.isEmpty(filepath)) {
                            filePaths.add(filepath);
                        }
                    }
                }
                intent.putExtra(ShareReceiver.SHARE_TAG, true);
                if (!ListUtil.isEmpty(iconPaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_IMG, iconPaths);
                }

                if (!ListUtil.isEmpty(videoPaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_VIDEO, videoPaths);
                }
                if (!ListUtil.isEmpty(filePaths)) {
                    intent.putStringArrayListExtra(ShareReceiver.SHARE_FILE, filePaths);
                }
                intent.setClass(this, SearchUserActivity.class);
                intent.putExtra(Constants.BundleKey.IS_TRANS, true);
                intent.putExtra(Constants.BundleKey.IS_FROM_SHARE, true);
                startActivity = true;
            }
        }
    }

    private int retry = 0;

    private void initActivityLifeCycle() {
        AppFrontBackHelper helper = new AppFrontBackHelper();
        helper.register(getApplication(), new AppFrontBackHelper.OnAppStatusListener() {
            @Override
            public void onFront() {
                //????????????????????????
                CurrentPreference.getInstance().setBack(false);


                if (!CommonConfig.isQtalk && TextUtils.isEmpty(DataUtils.getInstance(TabMainActivity.this).getPreferences(Constants.Preferences.qchat_qvt, ""))) {
                    IMUserDefaults.getStandardUserDefaults().newEditor(TabMainActivity.this).removeObject(Constants.Preferences.usertoken).synchronize();
                }
                boolean autoLogin = CurrentPreference.getInstance().isAutoLogin();

                if (autoLogin) {
                    Logger.i("??????????????????????????????:" + connectionUtil.isCanAutoLogin());
                    if (!connectionUtil.isCanAutoLogin()) {
                        startLoginView();
                        return;
                    } else {

                        if (!ConnectionUtil.getInstance().isLoginStatus()) {
                            retry = 0;
                            CurrentPreference.getInstance().setQvt(DataUtils.getInstance(CommonConfig.globalContext).getPreferences(Constants.Preferences.qchat_qvt, ""));
                            checkHealth();
                        }
                    }
                } else {
                    startLoginView();
                }


            }

            @Override
            public void onBack() {
                //????????????????????????
                CurrentPreference.getInstance().setBack(true);
            }
        });
    }

    private void checkHealth() {
        Logger.i("?????????????????????" + retry + "???");
        if (retry > 5) {
            Logger.i("????????????????????????5???????????????");
            return;
        }
        HttpUtil.checkHealth(new ProtocolCallback.UnitCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean aBoolean) {
                retry = 0;
                ConnectionUtil.getInstance().reConnectionForce();
            }

            @Override
            public void onFailure(String errMsg) {
                retry++;
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        checkHealth();
                    } catch (Exception e) {
                        Logger.i("?????????????????????" + e.getMessage());
                    }
                }).start();

            }
        });
    }


    private void saveTabClickLog(int position) {
        if (mTabEntities == null || mTabEntities.size() <= position) {
            return;
        }
        String eventId = "";
        String desc = "";
        switch (position) {
            case 0:
                eventId = "conversation";
                desc = "??????";
                break;
            case 1:
                eventId = "route";
                desc = "??????";
                break;
            case 2:
                eventId = "address list";
                desc = "?????????";
                break;
            case 3:
                eventId = "discovery";
                desc = "??????";
                break;
            case 4:
                eventId = "mine";
                desc = "??????";
                break;
        }
        saveHomeActLog(eventId, desc, "??????tab");
    }

    private void saveHomeActLog(String eventId, String desc, String currentPage) {
        LogInfo logInfo = QLog.build(LogConstans.LogType.ACT, LogConstans.LogSubType.CLICK).eventId(eventId).describtion(desc).currentPage(currentPage);
        LogService.getInstance().saveLog(logInfo);
    }

    @Override
    public void showFeedBackProgressView(final String[] args,final boolean isShowNotify,final boolean uploadDb) {
        if(!isFinishing()){
            if(isShowNotify && feed_layout != null && feed_progress_bar != null){
                feed_layout.setVisibility(View.VISIBLE);
                feed_progress_bar.setProgress(0);
            }
        }
        DispatchHelper.Async("uploadLog", true, () -> {
            FeedBackServcie feedBackServcie = new FeedBackServcie();
            feedBackServcie.setCallBack(TabMainActivity.this);
            feedBackServcie.setNotify(isShowNotify);
            feedBackServcie.setUploadDb(uploadDb);
            feedBackServcie.setVoids(args);
            feedBackServcie.handleLogs();
        });
    }

    @Override
    public void showFeedProgress(final long current, final long total, final FeedBackServcie.FeedType feedType) {
        runOnUiThread(() -> {
            int progress = (int) (current * 100 / total);
            feed_progress_bar.setProgress(progress);
            switch (feedType){
                case ZIP:
                    feed_text.setText("?????????...");
                    break;
                case UPLOAD:
                    feed_text.setText("?????????...");
                    feed_progress_bar.setProgress(progress);
                    if(progress == 100){
                        if(feed_layout != null){
                            feed_layout.setVisibility(View.GONE);
                        }
                    }
                    break;
            }
        });
    }

    @Override
    public void restartApplication() {
//        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//        Toast.makeText(this,"?????????????????????,3????????????",Toast.LENGTH_LONG).show();
        if (commonDialog != null && commonDialog.isShowing()) {
            commonDialog.dismiss();
        }

        commonDialog.setTitle("??????");
        commonDialog.setMessage("?????????????????????,????????????,???????????????!");
        commonDialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                commonDialog.dismiss();
                android.os.Process.killProcess(android.os.Process.myPid());  //??????PID
                System.exit(0);   //??????java???c#?????????????????????????????????0??????????????????
            }
        });
        commonDialog.show();


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }).start();


//        android.os.Process.killProcess(android.os.Process.myPid());  //??????PID
//        System.exit(0);   //??????java???c#?????????????????????????????????0??????????????????

        //??????????????????
//        final Intent intent =getPackageManager().getLaunchIntentForPackage(getPackageName());
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//        android.os.Process.killProcess(android.os.Process.myPid());

    }

}
