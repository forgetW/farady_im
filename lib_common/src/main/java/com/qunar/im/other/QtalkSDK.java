package com.qunar.im.other;

import android.content.Context;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.DiskLogStrategy;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import com.qunar.im.base.common.QChatRSA;
import com.qunar.im.base.common.QunarIMApp;
import com.qunar.im.base.module.WorkWorldNoticeTimeData;
import com.qunar.im.base.structs.PushSettinsStatus;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.base.util.LogUtil;
import com.qunar.im.base.util.MemoryCache;
import com.qunar.im.base.util.PhoneInfoUtils;
import com.qunar.im.base.util.graphics.MyDiskCache;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.core.manager.IMCoreManager;
import com.qunar.im.core.manager.IMDatabaseManager;
import com.qunar.im.core.manager.IMLogicManager;
import com.qunar.im.core.manager.IMNotificaitonCenter;
import com.qunar.im.core.services.QtalkHttpService;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.dispatch.DispatchHelper;
import com.qunar.im.protobuf.dispatch.DispatcherQueue;
import com.qunar.im.protobuf.utils.StringUtils;
import com.qunar.im.utils.ConnectionUtil;
import com.qunar.im.utils.DeviceInfoManager;
import com.qunar.im.utils.MD5;
import com.qunar.im.utils.PbAssemblyUtil;
import com.qunar.im.utils.PubKeyUtil;
import com.qunar.im.utils.QtalkDiskLogStrategy;
import com.qunar.im.utils.QtalkStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QtalkSDK {

    private static volatile QtalkSDK instance = new QtalkSDK();

    public static QtalkSDK getInstance() {
        return instance;
    }

//    public static void setInstance(QtalkSDK instance) {
//        QtalkSDK.instance = instance;
//    }

    private IMCoreManager coreManager;
    private IMLogicManager logicManager;
    public static final String CONNECTING_DISPATCHER_NAME = "connecting";

    private QtalkSDK() {
//        GlobalConfigManager.setGlobalContext(context);
        coreManager = IMCoreManager.BuildDefaultInstance(CommonConfig.globalContext);

        initialize();
    }

    public void initialize() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                // (Optional) Changes the log strategy to print out. Default LogCat
                .tag("My custom tag")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return CommonConfig.isDebug;
            }
        });
        //???????????????????????????
        String folder = MyDiskCache.CACHE_LOG_DIR;//save path
        final int MAX_BYTES = 10 * 1024 * 1024;//10M  per file
        HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
        ht.start();
        Logger.addLogAdapter(new DiskLogAdapter(CsvFormatStrategy.newBuilder()
                .logStrategy(new DiskLogStrategy(new QtalkDiskLogStrategy.WriteHandler(ht.getLooper(), folder, MAX_BYTES))).tag((CommonConfig.isQtalk ? "qtalk" : "qchat")).build()));
        //save log end
        logicManager = IMLogicManager.getInstance();
        String userName = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.lastuserid);
        if (TextUtils.isEmpty(userName)) {

            userName = "test";
        }
        IMDatabaseManager.getInstance().initialize(userName, CommonConfig.globalContext);
        //??????????????????????????? ??? ??????????????????????????????
        CurrentPreference.getInstance().setTurnOnMsgSound(ConnectionUtil.getInstance().getPushStateBy(PushSettinsStatus.SOUND_INAPP));
        CurrentPreference.getInstance().setTurnOnMsgShock(ConnectionUtil.getInstance().getPushStateBy(PushSettinsStatus.VIBRATE_INAPP));

    }

    //????????????
    public void addEvent(IMNotificaitonCenter.NotificationCenterDelegate object, String key) {
        IMNotificaitonCenter.getInstance().addObserver(object, key);
    }

    //??????????????????
    public void removeEvent(IMNotificaitonCenter.NotificationCenterDelegate object, String key) {
        IMNotificaitonCenter.getInstance().removeObserver(object, key);
    }


    /**
     * ????????????
     *
     * @param userName
     */
    public void logout(String userName) {
        Logger.i("????????????");
        //??????push imei
        try {
            Class clazzAd = Class.forName("com.qunar.im.thirdpush.QTPushConfiguration");
            Object adPresenter = clazzAd.newInstance();
            Method adMethod = clazzAd.getMethod("unRegistPush", Context.class);
            adMethod.invoke(adPresenter, CommonConfig.globalContext);
        } catch (Exception e) {
            Logger.i("??????push ?????????" + e.getMessage());
        }
        //??????push end
        MemoryCache.emptyCache();
        try{
            Class clazzEmoji = Class.forName("com.qunar.im.ui.util.EmotionUtils");
            Method method = clazzEmoji.getMethod("clearEmoticonCache");
            method.invoke(null,new Object[]{});
        }catch (Exception e){
            Logger.i("??????emoji?????? ?????????" + e.getMessage());
        }

//        Protocol.unregistPushinfo(PhoneInfoUtils.getUniqueID(), QTPushConfiguration.getPlatName(), true);
//        Protocol.deleteSelfImei(PhoneInfoUtils.getUniqueID(), true);
        //??????push
//        QTPushConfiguration.unRegistPush(CommonConfig.globalContext);

        //??????????????????
        IMLogicManager.getInstance().clearCache();
        CookieSyncManager.createInstance(QunarIMApp.getContext());
        CookieManager cookieManager = CookieManager.getInstance();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.removeSessionCookies(null);
        }else {
            cookieManager.removeAllCookie();
        }
        CookieSyncManager.getInstance().sync();

        //??????token
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .removeObject(Constants.Preferences.usertoken)
                .synchronize();
        IMLogicManager.getInstance().setLoginStatus(false);
        IMLogicManager.getInstance().logout(userName);
        //
        clearSmscode();
    }

    public void sendMessage(final ProtoMessageOuterClass.ProtoMessage protoMessage) {
        DispatchHelper.Async("sendMessage",false, new Runnable() {
            @Override
            public void run() {

                coreManager.sendMessage(protoMessage);
            }
        });
    }

    /**
     * ???????????????
     * @param protoMessage
     */
    public void sendMessageSync(ProtoMessageOuterClass.ProtoMessage protoMessage){
        coreManager.sendMessage(protoMessage);
    }

    /**
     * ????????????
     */
    public void sendHeartMessage() {
        IMLogicManager.getInstance().sendHeartMessage(PbAssemblyUtil.getHeartBeatMessage());
    }

    //?????????????????????????????????
    public boolean needTalkSmscode() {
        String token = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.usertoken);
        String userName = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.lastuserid);
        Logger.i("??????????????????????????????:"+token+"---"+userName);
        return !StringUtils.isEmpty(token) && !StringUtils.isEmpty(userName);
    }

    //?????????????????????
    public void clearSmscode() {
        //token??????sp
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .removeObject(Constants.Preferences.usertoken);

        //username??????sp
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .removeObject(Constants.Preferences.lastuserid);
    }

    Runnable loginRunnable = new Runnable() {
        @Override
        public void run(){
            try {
                Logger.i("????????????????????????,????????????,??????app??????:" + QunarIMApp.getQunarIMApp().getVersion() +
                        "?????????????????????" + DataUtils.getInstance(CommonConfig.globalContext).getPreferences(Constants.Preferences.PATCH_TIMESTAMP + "_" + QunarIMApp.getQunarIMApp().getVersionName(), "0"));

                if (!needTalkSmscode()) {
                    Logger.i("????????????????????????,????????????");
                    //?????????????????????null,????????????
                    return;
                }
                final String userName = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.lastuserid);
                //?????? ??????token????????????, ????????????base64????????? ????????? /0xxx/0?????????
                final String token = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.usertoken);
                CurrentPreference.getInstance().setToken(token);
                CurrentPreference.getInstance().setUserid(userName);

                IMDatabaseManager.getInstance().initialize(userName, CommonConfig.globalContext);
                IMDatabaseManager.getInstance().insertUserIdToCacheData(QtalkStringUtils.userId2Jid(userName));
                //?????????????????????,????????????????????????,??????????????????,?????????
                String navurl = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(QtalkNavicationService.NAV_CONFIG_CURRENT_URL, "");
                String str = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                        CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastMessageTime");

                //????????????????????????
                String wwuuid = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                        CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastwwuuid");

                //????????????????????????
                String wwtime = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                        CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastwwtime");

                if (TextUtils.isEmpty(str)) {
                    Logger.i("?????????????????????????????????" + str);
                    long lastMessageTime = IMDatabaseManager.getInstance().getLastestMessageTime();
                    if (lastMessageTime <= 0) {
                        lastMessageTime = System.currentTimeMillis() - 3600 * 48 * 1000;
                        Logger.i("?????????????????????????????????,??????????????????????????????");
                    }
                    IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                            .putObject(CurrentPreference.getInstance().getUserid()
                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                    + CommonConfig.isDebug
                                    + MD5.hex(navurl)
                                    + "lastMessageTime", lastMessageTime + "")
                            .synchronize();
                    Logger.i("??????????????????????????????????????????" + lastMessageTime);
                }


                if (TextUtils.isEmpty(wwtime)|| TextUtils.isEmpty(wwuuid)) {
                    Logger.i("??????????????????????????????????????????" + wwtime);

                    WorkWorldNoticeTimeData data =IMDatabaseManager.getInstance().getLastestWorkWorldTime();
//
//                        }
                    IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                            .putObject(CurrentPreference.getInstance().getUserid()
                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                    + CommonConfig.isDebug
                                    + MD5.hex(navurl)
                                    + "lastwwuuid", data.getUuid() + "")
                            .synchronize();

                    IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                            .putObject(CurrentPreference.getInstance().getUserid()
                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                    + CommonConfig.isDebug
                                    + MD5.hex(navurl)
                                    + "lastwwtime", data.getCreateTime() + "")
                            .synchronize();
                    Logger.i("???????????????????????????????????????????????????" + data.getUuid()+","+data.getCreateTime());
                }

                //??????????????????readmark?????????
                String rmt = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                        CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastGroupReadMarkTime");
                if(TextUtils.isEmpty(rmt)){
                    String localGroupRMTime = IMDatabaseManager.getInstance().getLatestGroupRMTime();
                    IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                            .putObject(CurrentPreference.getInstance().getUserid()
                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                    + CommonConfig.isDebug
                                    + MD5.hex(navurl)
                                    + "lastGroupReadMarkTime", localGroupRMTime)
                            .synchronize();
                }
                coreManager.login(userName, token);
            } catch (IOException e) {
                Logger.e(e, "login failed - IOException");
            }
        }
    };

    public void login(boolean isInterrupt) {
        try {
            DispatcherQueue connectingDispatcherQueue = DispatchHelper.getInstance().takeDispatcher(CONNECTING_DISPATCHER_NAME,false);
            if(connectingDispatcherQueue!=null){
                /**
                 * ????????????????????????
                 */
                connectingDispatcherQueue.removeCallbacks(loginRunnable);

                if(isInterrupt && !IMLogicManager.getInstance().isForceConnect()){
                    IMLogicManager.getInstance().setForceConnect();
                    IMLogicManager.getInstance().wakeup();
                }
                Logger.i("login-start" + isInterrupt);
            }

        } catch (Exception e) {
            Logger.i("??????????????????????????????");
        } finally {
            /**
             * ????????????????????????????????????
             */
            DispatchHelper.Async(CONNECTING_DISPATCHER_NAME, false,loginRunnable);
            Logger.i("login-start");
        }

    }

    public boolean isConnected() {
        return logicManager.isConnected();
    }

    public boolean isLoginStatus() {
        return logicManager.isLoginStatus();
    }

//    public void setLoginStatus(boolean b) {
//        IMLogicManager.getInstance().setLoginStatus(b);
//    }

    public void newLogin(String userName, String password){
        Logger.i("???????????????,??????:" + userName + ",??????:" + password);
        try{
            JSONObject nauth = new JSONObject();
            JSONObject data = new JSONObject();
            data.put("p",password);
            data.put("u",userName);
            data.put("mk",PhoneInfoUtils.getUniqueID());
            nauth.put("nauth",data);
            password = nauth.toString();
            Logger.i("?????????????????????,??????????????? password = " + password);
            IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                    .putObject(Constants.Preferences.usertoken, password)
                    .synchronize();
            CurrentPreference.getInstance().setToken(password);
            //username??????sp
            IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                    .putObject(Constants.Preferences.lastuserid, userName)
                    .synchronize();

            login(false);
        }catch (Exception e){

        }
    }

    public void publicLogin(String userName, String password) {
        Logger.i("???????????????????????????,??????:" + userName + ",??????:" + password);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String timeStr = simpleDateFormat.format(date);

        password = "{\"d\":\"" + timeStr + "\", \"p\":\"" + password + "\", \"u\":\"" + userName + "\", \"a\":\"testapp\"}";
        Logger.i("???????????????????????????,??????????????? password = " + password);
        try {
            password = QChatRSA.QTalkEncodePassword(password);
            Logger.i("???????????????????????????,??????????????? password = " + password);
        } catch (Exception e) {
            LogUtil.e(e + "");
            PubKeyUtil.deletePUBKEY(QtalkNavicationService.getInstance().getXmppdomain());
            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_FAILED, 0);
            return;
        }
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .putObject(Constants.Preferences.usertoken, password)
                .synchronize();
        CurrentPreference.getInstance().setToken(password);
        //username??????sp
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .putObject(Constants.Preferences.lastuserid, userName)
                .synchronize();

        login(false);
    }

    //???????????? ??????????????????token,???username??????sp???
    public void login(final String userName, final String password) {
        final boolean[] succeeded = {false};
        final String[] errorMessage = new String[1];

        if(TestAccount.isTestAccount(userName)){
            saveToken(succeeded,userName,TestAccount.getTestAccountToken(userName));
        }else {
            DispatchHelper.sync("takePassword", new Runnable() {
                @Override
                public void run() {
                    String tokenReqeustUrl = QtalkNavicationService.getInstance().getTokenSmsUrl();

                    JSONObject result =
                            QtalkHttpService.buildFormRequest(tokenReqeustUrl)
                                    .addParam("rtx_id", userName)
                                    .addParam("verify_code", password)
                                    .post();

                    if (result != null) {
                        int statusId = -100;
                        try {
                            statusId = result.getInt("status_id");
                        } catch (JSONException e) {
                            Logger.e(e, "json parse failed");
                        }
                        if (statusId == 0) {
                            String token = null;
                            try {
                                token = result.getJSONObject("data").getString("token");

                            } catch (JSONException e) {
                                Logger.e(e, "json parse failed");
                            }
                            if (StringUtils.isNotEmpty(token)) {
                                saveToken(succeeded,userName,token);
                            }
                        } else {
                            try {
                                errorMessage[0] = result.getString("msg");
                            } catch (JSONException e) {
                                Logger.e(e, "json parse failed");
                            }
                        }
                    }
                }
            });
        }
        if (succeeded[0] == false) {
            //???????????????????????????0
            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_FAILED, 0);
        } else {
            login(false);
        }
    }

    private void saveToken(boolean[] succeeded,String userName,String token){
        //token??????sp
        String pwd = String.format("%s@%s", DeviceInfoManager.getInstance().getDeviceId(CommonConfig.globalContext), token);
        if(TestAccount.isTestAccount(userName)){
            pwd = token;
        }
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .putObject(Constants.Preferences.usertoken, pwd)
                .synchronize();
        CurrentPreference.getInstance().setToken(token);
        //username??????sp
        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                .putObject(Constants.Preferences.lastuserid, userName)
                .synchronize();
        succeeded[0] = true;
    }

    public void takeSmsCode(final String userName, final IQTalkLoginDelegate delegate) {
        DispatchHelper.Async("takeSmsCode", new Runnable() {
            @Override
            public void run() {
                String tokenReqeustUrl = QtalkNavicationService.getInstance().getVerifySmsUrl();
                Logger.i("??????????????? ??? url = " + tokenReqeustUrl + "  username = " + userName);
                JSONObject result =
                        QtalkHttpService.buildFormRequest(tokenReqeustUrl)
                                .addParam("rtx_id", userName)
                                .post();

                if (delegate != null) {
                    int resultCode = -1;
                    if (result == null) {
                        delegate.onSmsCodeReceived(resultCode, "????????????????????????????????????");
                        return;
                    }
                    String errMessage = null;

                    try {
                        if (result != null) {
                            resultCode = result.getInt("status_id");
                        }

                        if (resultCode != 0) {
                            errMessage = result.getString("msg");
                        }

                    } catch (JSONException e) {
                        Logger.e(e, "parse json failed");
                    }
                    delegate.onSmsCodeReceived(resultCode, errMessage);
                }
            }
        });
    }

    /**
     * ?????????????????????
     */
    public void initNavConfig(final boolean isForce) {
        Logger.i("???????????????:" + isForce);
        DispatchHelper.sync("updateNav", new Runnable() {
            @Override
            public void run() {
                //????????????
                QtalkNavicationService.getInstance().updateNavicationConfig(isForce);
            }
        });
    }

}
