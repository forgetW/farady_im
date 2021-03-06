package com.qunar.im.core.manager;

import android.content.Context;
import android.text.TextUtils;

import com.orhanobut.logger.Logger;
import com.qunar.im.base.jsonbean.SetWorkWorldRemindResponse;
import com.qunar.im.base.module.CityLocal;
import com.qunar.im.base.module.MedalListResponse;
import com.qunar.im.base.module.MedalUserStatusResponse;
import com.qunar.im.base.module.VideoSetting;
import com.qunar.im.base.module.WorkWorldNoticeHistoryResponse;
import com.qunar.im.base.module.WorkWorldResponse;
import com.qunar.im.base.structs.WorkWorldItemState;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.log.LogConstans;
import com.qunar.im.log.LogService;
import com.qunar.im.log.QLog;
import com.qunar.im.utils.ConnectionUtil;
import com.qunar.im.utils.HttpUtil;
import com.qunar.im.base.jsonbean.BaseJsonResult;
import com.qunar.im.base.jsonbean.DepartmentResult;
import com.qunar.im.base.jsonbean.HotlinesResult;
import com.qunar.im.base.jsonbean.IncrementUsersResult;
import com.qunar.im.base.jsonbean.MessageStateSendJsonBean;
import com.qunar.im.base.jsonbean.NewRemoteConfig;
import com.qunar.im.base.jsonbean.OpsUnreadResult;
import com.qunar.im.base.jsonbean.PushSettingResponseBean;
import com.qunar.im.base.jsonbean.QuickReplyResult;
import com.qunar.im.base.module.AreaLocal;
import com.qunar.im.base.module.CalendarTrip;
import com.qunar.im.base.module.MucListResponse;
import com.qunar.im.base.protocol.Protocol;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.base.structs.MessageStatus;
import com.qunar.im.base.structs.PushSettinsStatus;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.DateTimeUtils;
import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.core.enums.LoginStatus;
import com.qunar.im.core.services.ClearLogService;
import com.qunar.im.core.services.QtalkHttpService;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.entity.XMPPJID;
import com.qunar.im.utils.CalendarSynchronousUtil;
import com.qunar.im.utils.MD5;
import com.qunar.im.utils.PbAssemblyUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.Map;
import java.lang.reflect.Method;

/**
 * Created by may on 2017/7/12.
 */
//?????????????????????
public class LoginComplateManager {

    private static final String TAG = "LoginComplateManager";

    private static boolean isBackgroundLogin = false;

    public static void loginComplate() {
        Logger.i("????????????,??????????????????");
//        checkNetworkStatus();

//        checkMessageState();
//        updateLastMsgTime();
//        IMNotificaitonCenter.getInstance().postMainThreadNotificationName("LoginStatusChanged", LoginStatus.Updating);
        if (!isBackgroundLogin) {
            Logger.i("?????????????????????UI:?????????...");
            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_EVENT, LoginStatus.Updating);
            // TODO: 2017/11/15 ??????????????????????????????log??????
//            IMDatabaseManager.getInstance().updateMessageState(MessageState.Waiting, MessageState.Failed);
            //??????????????????Card


            long a5s = System.currentTimeMillis();
            updateOfflineMessages();
            long a5e = System.currentTimeMillis();
            long time5 = a5e - a5s;
            Logger.i("time5:" + time5);
            try {
                //????????????
                LogService.getInstance()
                        .saveLog(QLog.build(LogConstans.LogType.COD, LogConstans.LogSubType.NATIVE)
                                .describtion("??????????????????")
                                .costTime(time5)
                                .method("updateOfflineMessages"));
            } catch (Exception e) {

            }
            //??????checkconfig
//            if(true){
//                return;
//            }
            //???????????????
            Logger.i("????????????,?????????????????????UI:?????????");
            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_EVENT, LoginStatus.Login);
            checkWorkWorldPermissions();

            long a44a = System.currentTimeMillis();
            getWordWorldNoticeHistory();
            getWorkWorldRemind();
            long a44b = System.currentTimeMillis();
            Logger.i("time_work:" + (a44a - a44b));

            long updateStart = System.currentTimeMillis();
            updateMessageStateNoticeServer();
            Logger.i("????????????????????????:" + (System.currentTimeMillis() - updateStart));
            updateMyPushSetting();



            updateMedalList();

//            long a13s = System.currentTimeMillis();
//            updateUserMucPushConfig();
//            long a13e = System.currentTimeMillis();
//            Logger.i("time12" + (a13s - a13e));

            //?????????????????????
            long a2s = System.currentTimeMillis();
            updateMyCard();
            if (!CommonConfig.isQtalk && CurrentPreference.getInstance().isMerchants()) {//qchat ????????? ??????????????? ???????????????
                notifyOnLine();
            }
            long a2e = System.currentTimeMillis();
            Logger.i("time2:" + (a2e - a2s));
            //??????????????????
            long a3s = System.currentTimeMillis();
            long time = updateMucList();
            long a3e = System.currentTimeMillis();
            Logger.i("time3:" + (a3e - a3s));
            //TODO ??????????????????????????????????????? ????????????????????????????????????


            long a4s = System.currentTimeMillis();
            updateMucInfoList(time);
            long a4e = System.currentTimeMillis();
            Logger.i("time4:" + (a4e - a4s));

            //??????ops??????
            updateMyOPSMessage();

            long a6s = System.currentTimeMillis();
            updateMessageState();
            long a6e = System.currentTimeMillis();
            //????????????????????????
            Logger.i("time6:" + (a6e - a6s));

            getVideoSetting();

            long a7s = System.currentTimeMillis();
            get_virtual_user_role();
            long a7e = System.currentTimeMillis();
            Logger.i("time7:" + (a7e - a7s));


            long a8s = System.currentTimeMillis();
            updateUserServiceConfig(false);
            long a8e = System.currentTimeMillis();
            Logger.i("time8:" + (a8e - a8s));

            long a9s = System.currentTimeMillis();
            HttpUtil.getMyCapability(true);
            long a9e = System.currentTimeMillis();
            Logger.i("time9:" + (a9e - a9s));

            if (!CommonConfig.isQtalk) {
                updateQuickReply(false);
            }

            long a10s = System.currentTimeMillis();
            if (CommonConfig.isQtalk) {
                Logger.i("qtalk?????????,???????????????");
                try {
                    Class<?> clazz = Class.forName("com.qunar.im.ui.services.PullPasswordBoxService");
                    Method method = clazz.getMethod("runPullPasswordBoxService", Context.class);
                    method.invoke(null, CommonConfig.globalContext);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            long a10e = System.currentTimeMillis();
            Logger.i("time10:" + (a10e - a10s));


            //????????????????????????????????????????????????
            //????????????????????????,?????????????????????????????????
            long a1s = System.currentTimeMillis();
            Logger.i("??????????????????????????????");
            if (CommonConfig.isQtalk) {
                processBuddy();
            } else {
                getQchatDepInfo();
            }
            long a1e = System.currentTimeMillis();
            Logger.i("????????????time:" + (a1e - a1s));

            //?????????????????????????????????
//            if (CommonConfig.isQtalk) {
//                RobotListPresenter presenter = new RobotListPresenter();
//                presenter.loadRobotIdList4mNet();
//            }

            long a11s = System.currentTimeMillis();
            ConnectionUtil.getInstance().setNotificationConfig();
            long a11e = System.currentTimeMillis();

            clearLogFile();

//            long a12s = System.currentTimeMillis();
//            setConfigProfile();
//            long a12e = System.currentTimeMillis();

            updateTripList();

            updateTripArae();

            updateTripCity();
            return;
        }
    }

    public static void updateMedalList() {


        int version = IMDatabaseManager.getInstance().selectMedalListVersion();
        HttpUtil.getMedal(version, new ProtocolCallback.UnitCallback<MedalListResponse>() {
            @Override
            public void onCompleted(MedalListResponse medalListResponse) {
                new String();
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });

        int statusVersion = IMDatabaseManager.getInstance().selectUserMedalStatusVersion();
        HttpUtil.getUserMedalStatus(statusVersion, new ProtocolCallback.UnitCallback<MedalUserStatusResponse>() {
            @Override
            public void onCompleted(MedalUserStatusResponse medalUserStatusResponse) {
                new String();
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    /**
     * ??????????????????????????????
     */
    private static void getVideoSetting() {
        HttpUtil.videoSetting(new ProtocolCallback.UnitCallback<VideoSetting>() {
            @Override
            public void onCompleted(VideoSetting videoSetting) {
                Logger.i("??????????????????????????????:" + JsonUtils.getGson().toJson(videoSetting));
            }

            @Override
            public void onFailure(String errMsg) {
                Logger.i("??????????????????????????????");
            }
        });
    }


    /**
     * ??????????????????
     */
    private static void updateTripCity() {
        HttpUtil.getCity(new ProtocolCallback.UnitCallback<CityLocal>() {
            @Override
            public void onCompleted(CityLocal areaLocal) {
                IMDatabaseManager.getInstance().InsertCity(areaLocal);
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    /**
     * ??????????????????
     */
    public static void updateTripArae() {
        HttpUtil.getArea(new ProtocolCallback.UnitCallback<AreaLocal>() {
            @Override
            public void onCompleted(AreaLocal areaLocal) {
                IMDatabaseManager.getInstance().InsertArea(areaLocal);
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    /**
     * ??????????????????
     */
    public static void updateTripList() {

        long version = IMDatabaseManager.getInstance().selectUserTripVersion();
//        version = 0;
        HttpUtil.getUserTripList(version, new ProtocolCallback.UnitCallback<CalendarTrip>() {
            @Override
            public void onCompleted(CalendarTrip calendarTrip) {
                IMDatabaseManager.getInstance().InsertTrip(calendarTrip);
                IMDatabaseManager.getInstance().insertUserTripVersion(Long.parseLong(calendarTrip.getData().getUpdateTime()));
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.UPDATE_TRIP);
                CalendarSynchronousUtil.bulkTrip(calendarTrip);

            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    /**
     * ?????????????????????????????????
     */
    public static void updateMessageStateNoticeServer() {
        //???????????????????????????????????????????????????????????????????????????0???
        List<MessageStateSendJsonBean> list = IMDatabaseManager.getInstance().getMessageStateSendNotXmppIdJson(CurrentPreference.getInstance().getPreferenceUserId(), "1");
        if (list == null || list.size() < 1) {
            return;
        }
        Logger.i("json??????????????????0????????? ????????? " + JsonUtils.getGson().toJson(list));
        Logger.i("???????????????????????????????????????,???????????????????????????????????????");
        for (int i = 0; i < list.size(); i++) {
            ProtoMessageOuterClass.ProtoMessage receive = PbAssemblyUtil.getBeenNewReadStateMessage(MessageStatus.STATUS_SINGLE_DELIVERED + "", list.get(i).getJsonArray(), list.get(i).getUserid(), null);
            IMLogicManager.getInstance().sendMessage(receive);
            IMDatabaseManager.getInstance().updateMessageStateByJsonArray(list.get(i).getJsonArray());

        }
    }

    public static void updateQuickReply(boolean isForce) {
        int gversion = 0;
        int cversion = 0;
        if (isForce) {
            gversion = 0;
            cversion = 0;
            IMDatabaseManager.getInstance().deleteQuickReply();
        } else {
            gversion = IMDatabaseManager.getInstance().selectQuickReplyGroupMaxVersion();
            cversion = IMDatabaseManager.getInstance().selectQuickReplyContentMaxVersion();
        }
        HttpUtil.getQuickReplies(gversion, cversion, new ProtocolCallback.UnitCallback<QuickReplyResult>() {
            @Override
            public void onCompleted(QuickReplyResult quickReplyResult) {
                if (quickReplyResult != null && quickReplyResult.data != null) {
                    ConnectionUtil.getInstance().refreshTheQuickReply(quickReplyResult.data);
                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }


    private static void updateMyOPSMessage() {
        HttpUtil.getUnreadCountFromOps(new ProtocolCallback.UnitCallback<OpsUnreadResult>() {
            @Override
            public void onFailure(String errMsg) {
            }

            @Override
            public void onCompleted(OpsUnreadResult opsUnreadResult) {
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.refreshOPSUnRead, opsUnreadResult.getData().isHasUnread());
            }
        });
    }

    private static void updateMyPushSetting() {
        HttpUtil.getPushMsgSettings(new ProtocolCallback.UnitCallback<PushSettingResponseBean>() {
            @Override
            public void onCompleted(PushSettingResponseBean pushSettingResponseBean) {
                if (pushSettingResponseBean.isRet()) {
                    IMDatabaseManager.getInstance().updatePushSettingAllState(pushSettingResponseBean.getData().getPush_flag());
                    CurrentPreference.getInstance().setTurnOnMsgSound(ConnectionUtil.getInstance().getPushStateBy(PushSettinsStatus.SOUND_INAPP));
                    CurrentPreference.getInstance().setTurnOnMsgShock(ConnectionUtil.getInstance().getPushStateBy(PushSettinsStatus.VIBRATE_INAPP));

                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    private static void updateMucInfoList(long time) {
        Logger.i("?????????????????????");
        try {
            IMUserCardManager.getInstance().updateMucCardSync(time);
        } catch (Exception e) {
            Logger.e("updateMucInfoList error:" + e.getLocalizedMessage());
        }
//        if(time == 0){//????????????????????? ??????????????????
//            try {
//                IMUserCardManager.getInstance().updateMucCardSync(IMDatabaseManager.getInstance().SelectIMGroupId());
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * ??????????????????
     */
    public static void updateUserServiceConfig(boolean isForce) {
//        if(true){
//            return;
//        }
        int version = 0;
        //???????????????????????? ??????????????????????????? ????????? ???????????? ?????????,?????? ??????????????????
        if (isForce) {
            version = 0;
            IMDatabaseManager.getInstance().deleteUserConfig();
        } else {
            version = IMDatabaseManager.getInstance().selectUserConfigVersion();
        }
//        int version = 0;
        HttpUtil.getUserConfig(version, new ProtocolCallback.UnitCallback<NewRemoteConfig>() {
            @Override
            public void onCompleted(NewRemoteConfig newRemoteConfigs) {
                if (newRemoteConfigs.getData().getClientConfigInfos().size() > 0) {
                    ConnectionUtil.getInstance().refreshTheConfig(newRemoteConfigs);
                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });

    }

    /**
     * ??????????????????????????????????????????,?????????????????????????????????????????????,?????????????????????
     */
    private static long updateMucList() {

        long time = -1;
        try {
            Logger.i("????????????????????????");
            XMPPJID mySelf = IMLogicManager.getInstance().getMyself();
            time = IMDatabaseManager.getInstance().getGroupLastUpdateTime();
            String destUrl = String.format("%s/muc/get_increment_mucs.qunar?u=%s&k=%s",
                    QtalkNavicationService.getInstance().getHttpUrl(),
                    mySelf.getUser(),
                    IMLogicManager.getInstance().getRemoteLoginKey());
            JSONObject inputBody = new JSONObject();
            inputBody.put("u", mySelf.getUser());
            inputBody.put("d", mySelf.getDomain());
            inputBody.put("t", time);
            Logger.i("??????????????????:" + destUrl + "????????????:" + inputBody);
            JSONObject response = QtalkHttpService.postJson(destUrl, inputBody);
            if (response == null) {
                return time;
            }
            BaseJsonResult baseResponse = JsonUtils.getGson().fromJson(response.toString(), BaseJsonResult.class);
            if (!baseResponse.ret) {
                return time;
            }
            MucListResponse mucListResponse = JsonUtils.getGson().fromJson(response.toString(), MucListResponse.class);

            Logger.i("????????????????????????:" + JsonUtils.getGson().toJson(mucListResponse));
//            if (!mucListResponse.getRet()) {
//                return;
//            }
            if (!(mucListResponse.ret && mucListResponse.getData() != null && mucListResponse.getData().size() > 0)) {
                return time;
            }
            List<MucListResponse.Data> list = mucListResponse.getData();
            List<MucListResponse.Data> okList = new ArrayList<>();
            List<MucListResponse.Data> noList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                MucListResponse.Data data = list.get(i);
                if (data.getF().equals("1")) {
                    okList.add(data);
                } else {
                    noList.add(data);
                }
            }
            Logger.i("???????????????????????????");
            IMDatabaseManager.getInstance().updateMucList(okList, noList);

        } catch (Exception e) {

        } finally {
            return time;
        }
    }


    /**
     * ??????????????????????????????????????????????????????
     */
    private static void updateMessageState() {
        Logger.i("??????????????????????????????????????????");
        IMDatabaseManager.getInstance().updateMessageStateFailed();

    }

    /**
     * ??????????????????
     */
    private static void updateOfflineMessages() {
        try {
            IMMessageManager.getInstance().updateOfflineMessage();
        } catch (IOException e) {
            Logger.e(e, "updateOfflineMessages crashed for io");
        } catch (JSONException e) {
            Logger.e(e, "updateOfflineMessages crashed for json");
        }
    }

    private static void updateMyCard() {
        String userId = IMLogicManager.getInstance().getMyself().bareJID().fullname();

        try {
            IMUserCardManager.getInstance().updateUserCard(userId, true);
            //?????????????????????????????????,?????????????????? ????????????body??????????????????
            String myNickName = IMDatabaseManager.getInstance().selectUserByJID(userId).optString("Name");
            CurrentPreference.getInstance().setUserName(myNickName);
            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.SHOW_MY_INFO, "");
        } catch (JSONException e) {
            Logger.e(e, "updateMyCard failed.");
        }
    }

    /**
     * qchat ????????????????????????
     */
    private static void notifyOnLine() {
        HttpUtil.notifyOnline();
    }

    /**
     * ???????????????????????????????????? ????????????????????????,?????????????????????????????????
     */
    public static void processBuddy() {

//        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Update_Buddy);//??????????????????
        int version = IMDatabaseManager.getInstance().getLastIncrementUsersVersion();

        HttpUtil.getIncrementUsers(version, new ProtocolCallback.UnitCallback<IncrementUsersResult>() {
            @Override
            public void onCompleted(IncrementUsersResult incrementUsersResult) {
                IMDatabaseManager.getInstance().InsertUserCardInIncrementUser(incrementUsersResult);
                try {
                    //??????????????????????????????, ????????????????????????????????????
                    String userId = IMLogicManager.getInstance().getMyself().bareJID().fullname();
                    String myNickName = IMDatabaseManager.getInstance().selectUserByJID(userId).getString("Name");
                    CurrentPreference.getInstance().setUserName(myNickName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(String errMsg) {

            }
        });

    }

    public static void getQchatDepInfo() {
//        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Update_Buddy);//??????????????????
        String time = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, CurrentPreference.getInstance().getUserid()
                + QtalkNavicationService.getInstance().getXmppdomain()
                + Constants.Preferences.buddytime);
//        long tiem = IMUserDefaults.getStandardUserDefaults().get
        if (!TextUtils.isEmpty(time)) {
            long lastTime = Long.parseLong(time);
            long newTime = System.currentTimeMillis();
            if ((newTime - lastTime) > 24 * 60 * 60 * 1000) {
//                IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
//                        .putObject(CurrentPreference.getInstance().getUserid()
//                                + QtalkNavicationService.getInstance().getXmppdomain()
//                                + "buddyTime", String.valueOf(newTime))
//                        .synchronize();
            } else {
                //// TODO: 2017/10/26 ?????????????????????????????????????????????boddy
                return;
            }
        }

        Logger.i("???????????????QCHAT????????????????????????");
        //?????????????????????????????? ?????????
//        int version = 0;// IMDatabaseManager.getInstance().getIncrementUsersCount();
//        String qchatorg = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(CurrentPreference.getInstance().getUserid()
//                + QtalkNavicationService.getInstance().getXmppdomain()
//                + Constants.Preferences.qchat_org, "");
//        if (!TextUtils.isEmpty(qchatorg)) {
//            return;
//        }
        Protocol.getQchatDeptInfo(new ProtocolCallback.UnitCallback<DepartmentResult>() {
            @Override
            public void onCompleted(DepartmentResult departmentResult) {
                if (departmentResult != null) {
                    //qchat??????????????????
                    DataUtils.getInstance(CommonConfig.globalContext).putPreferences(CurrentPreference.getInstance().getUserid()
                            + QtalkNavicationService.getInstance().getXmppdomain()
                            + Constants.Preferences.qchat_org, JsonUtils.getGson().toJson(departmentResult));

                    String time = String.valueOf(System.currentTimeMillis());

                    IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                            .putObject(CurrentPreference.getInstance().getUserid()
                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                    + Constants.Preferences.buddytime, time)
                            .synchronize();
                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

    /**
     * ???????????????
     */
    private static void clearLogFile() {
        //???????????????????????????7???????????????
        long lastClearTime = DataUtils.getInstance(CommonConfig.globalContext).getPreferences("lastClearTime", 0L);
        Logger.i(TAG + " clearLogFile lastClearTime = " + DateTimeUtils.getTime(lastClearTime, true, true) + "  ????????????" + DateTimeUtils.getTime(System.currentTimeMillis(), false, true));
        if (lastClearTime > 0) {
            if (System.currentTimeMillis() - lastClearTime > 7 * 24 * 60 * 60 * 1000) {
                ClearLogService.runClearLogService(CommonConfig.globalContext);
                DataUtils.getInstance(CommonConfig.globalContext).putPreferences("lastClearTime", System.currentTimeMillis());
            }
        } else {
            DataUtils.getInstance(CommonConfig.globalContext).putPreferences("lastClearTime", System.currentTimeMillis());
        }
    }

    public static void get_virtual_user_role() {
        Logger.i("??????????????????????????????");
        HttpUtil.getHotlineList(new ProtocolCallback.UnitCallback<HotlinesResult.DataBean>() {
            @Override
            public void onCompleted(HotlinesResult.DataBean hotlines) {
                if(hotlines != null){
                    ConnectionUtil.getInstance().cacheHotlines(hotlines.allhotlines);
                    CurrentPreference.getInstance().setHotLineList(hotlines.allhotlines);
                    CurrentPreference.getInstance().setMyHotlines(hotlines.myhotlines);
                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });

    }

    //??????????????????
//    private static void setConfigProfile() {
//        CurrentPreference.ProFile f = IMDatabaseManager.getInstance().getProFile();
//        CurrentPreference.Preference preference = IMDatabaseManager.getInstance().getPreference();
//        if (f != null)
//            CurrentPreference.getInstance().setProFile(f);
//        if (preference != null){
//            CurrentPreference.getInstance().setPreference(preference);
//        }
//    }


    /**
     * ??????????????????????????????
     */
    private static void checkWorkWorldPermissions() {

        HttpUtil.checkWorkWorldPermissionsV2(new ProtocolCallback.UnitCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean aBoolean) {
                boolean workworldState = IMDatabaseManager.getInstance().SelectWorkWorldPremissions();
                IMDatabaseManager.getInstance().InsertWorkWorldPremissions(aBoolean);
                if (aBoolean == workworldState) {
                    //??????????????????????????????????????????????????????
                } else {
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.RESTART);
                }


            }

            @Override
            public void onFailure(String errMsg) {

            }
        });


    }


    public static void getWordWorldNoticeHistory() {
        final String navurl = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(QtalkNavicationService.NAV_CONFIG_CURRENT_URL, "");

        boolean show = IMUserDefaults.getStandardUserDefaults().getBooleanValue(CommonConfig.globalContext,
                CurrentPreference.getInstance().getUserid()
                        + QtalkNavicationService.getInstance().getXmppdomain()
                        + CommonConfig.isDebug
                        + MD5.hex(navurl)
                        + "WORKWORLDSHOWUNREAD", false);
        if (show) {

            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE);
        } else {


            HttpUtil.refreshWorkWorldV2(1, 0, WorkWorldItemState.normal, "", "", 0, false, new ProtocolCallback.UnitCallback<WorkWorldResponse>() {
                @Override
                public void onCompleted(WorkWorldResponse workWorldResponse) {
                    if (workWorldResponse != null && workWorldResponse.getData().getNewPost() != null && workWorldResponse.getData().getNewPost().size() > 0) {
                        boolean isHave = IMDatabaseManager.getInstance().selectHistoryWorkWorldItemIsHave(workWorldResponse.getData().getNewPost().get(0));


                        if (!isHave) {

                            IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                                    .putObject(CurrentPreference.getInstance().getUserid()
                                            + QtalkNavicationService.getInstance().getXmppdomain()
                                            + CommonConfig.isDebug
                                            + MD5.hex(navurl)
                                            + "WORKWORLDSHOWUNREAD", true)
                                    .synchronize();

                            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE);
//
                        }
                    }
                }

                @Override
                public void onFailure(String errMsg) {
                    Logger.i("??????????????????:" + errMsg);
//                mView.workworldcloseRefresh();
                }
            });
        }

        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE);
        //// TODO: 2017/9/4 ?????????????????????????????????????????????????????????,???????????????????????????


//        long start = System.currentTimeMillis();

        String timeId = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                CurrentPreference.getInstance().getUserid()
                        + QtalkNavicationService.getInstance().getXmppdomain()
                        + CommonConfig.isDebug
                        + MD5.hex(navurl)
                        + "lastwwuuid");
//        long lastMessageTime = IMDatabaseManager.getInstance().getLastestMessageTime();
        String timeStr = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                CurrentPreference.getInstance().getUserid()
                        + QtalkNavicationService.getInstance().getXmppdomain()
                        + CommonConfig.isDebug
                        + MD5.hex(navurl)
                        + "lastwwtime");

        HttpUtil.getWorkWorldHistory(timeId, timeStr, new ProtocolCallback.UnitCallback<WorkWorldNoticeHistoryResponse>() {
            @Override
            public void onCompleted(WorkWorldNoticeHistoryResponse workWorldNoticeHistoryResponse) {

                IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                        .removeObject(CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastwwuuid")
                        .synchronize();

                IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                        .removeObject(CurrentPreference.getInstance().getUserid()
                                + QtalkNavicationService.getInstance().getXmppdomain()
                                + CommonConfig.isDebug
                                + MD5.hex(navurl)
                                + "lastwwtime")
                        .synchronize();


//                if(workWorldNoticeHistoryResponse.getData().getMsgList().size()>0){
                //??????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE);
//                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });


    }


    public static void getWorkWorldRemind() {
        HttpUtil.getWorkWorldRemind(new ProtocolCallback.UnitCallback<SetWorkWorldRemindResponse>() {
            @Override
            public void onCompleted(SetWorkWorldRemindResponse setWorkWorldRemindResponse) {

            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }

}