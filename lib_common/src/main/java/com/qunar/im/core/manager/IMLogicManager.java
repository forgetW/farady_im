package com.qunar.im.core.manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LruCache;

import com.google.protobuf.InvalidProtocolBufferException;
import com.orhanobut.logger.Logger;
import com.qunar.im.base.common.BackgroundExecutor;
import com.qunar.im.base.common.ConversitionType;
import com.qunar.im.base.common.QunarIMApp;
import com.qunar.im.base.jsonbean.CalendarVersion;
import com.qunar.im.base.jsonbean.NavConfigResult;
import com.qunar.im.base.jsonbean.NoticeBean;
import com.qunar.im.base.jsonbean.OpsUnreadResult;
import com.qunar.im.base.jsonbean.VersionBean;
import com.qunar.im.base.jsonbean.WebRtcJson;
import com.qunar.im.base.module.GroupMember;
import com.qunar.im.base.module.IMGroup;
import com.qunar.im.base.module.IMMessage;
import com.qunar.im.base.module.MedalUserStatusResponse;
import com.qunar.im.base.module.NavigationNotice;
import com.qunar.im.base.module.Nick;
import com.qunar.im.base.module.RevokeInfo;
import com.qunar.im.base.module.UserHaveMedalStatus;
import com.qunar.im.base.module.WorkWorldItem;
import com.qunar.im.base.module.WorkWorldNoticeItem;
import com.qunar.im.base.structs.MessageStatus;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.EventBusEvent;
import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.core.utils.GlobalConfigManager;
import com.qunar.im.other.QtalkSDK;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.protobuf.Interfaces.IGroupEventReceivedDelegate;
import com.qunar.im.protobuf.Interfaces.IIMEventReceivedDelegate;
import com.qunar.im.protobuf.Interfaces.IMessageReceivedDelegate;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.dispatch.DispatchHelper;
import com.qunar.im.protobuf.dispatch.DispatcherQueue;
import com.qunar.im.protobuf.entity.XMPPJID;
import com.qunar.im.protobuf.stream.ProtobufSocket;
import com.qunar.im.protobuf.utils.StringUtils;
import com.qunar.im.utils.ConnectionUtil;
import com.qunar.im.utils.HttpUtil;
import com.qunar.im.utils.MD5;
import com.qunar.im.utils.PbAssemblyUtil;
import com.qunar.im.utils.PbParseUtil;
import com.qunar.im.utils.QtalkStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

/**
 * ???????????????reConnectionForce?????? ???????????????&??????
 * Created by may on 2017/6/29.
 */

public class IMLogicManager implements IMessageReceivedDelegate, IGroupEventReceivedDelegate, IIMEventReceivedDelegate {
    private static IMLogicManager instance = new IMLogicManager();
    //    private String domain;
//    private String hostName;
    private String resource;
    //    private int port;
    private String remoteLoginKey;
    private IMProtocol _protocolType;
    private XMPPJID myself;

    private LruCache<String, JSONObject> userCache;

    private LruCache<String, Nick> nickCache;

    private LruCache<String,List<UserHaveMedalStatus>> userMedalCache;

    private static String defaultMucImage = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + "/file/v2/download/perm/2227ff2e304cb44a1980e9c1a3d78164.png";
    private static String defaultUserImage = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + "/file/v2/download/perm/3ca05f2d92f6c0034ac9aee14d341fc7.png";

//    private List<String>

    private ProtobufSocket _pbSocket;

    private DispatcherQueue _loginComplateQueue;

    public static String getDefaultMucImage() {
        return defaultMucImage;
    }

    public static void setDefaultMucImage(String defaultMucImage) {
        IMLogicManager.defaultMucImage = defaultMucImage;
    }

    public static String getDefaultUserImage() {
        return defaultUserImage;
    }

    public static void setDefaultUserImage(String defaultUserImage) {
        IMLogicManager.defaultUserImage = defaultUserImage;
    }

    protected IMLogicManager() {
        _protocolType = IMProtocol.PROTOCOL_PROTOBUF;
//        remoteLoginKey = null;
        try {
            if (userCache == null) {
                userCache = new LruCache<>(100);
            }
            if (nickCache == null) {
                nickCache = new LruCache<>(500);
            }
            if(userMedalCache==null){
                userMedalCache = new LruCache<>(500);
            }
//            userCache = new LruCache<>(100);
//            nickCache = new LruCache<>(200);
//            _loginComplateQueue = DispatchHelper.getInstance().takeDispatcher("loginComplateQueue");
        } catch (Exception e) {
        }

        _pbSocket = new ProtobufSocket();
        _pbSocket.addMessageDelegate(this);
        _pbSocket.addGroupEventDelegate(this);
        _pbSocket.addSocketEventDelegate(this);
    }

    public static IMLogicManager getInstance() {
        return instance;
    }

    public void clearCache() {
        if (userCache != null && userCache.size() > 0) {
            userCache.evictAll();
        }
        if (nickCache != null && nickCache.size() > 0) {
            nickCache.evictAll();
        }
    }

    //??????userinfo
    public JSONObject getUserInfoByUserId(XMPPJID myId) {
        if (myId == null) return null;
        String userId = myId.bareJID().fullname();

        JSONObject result = userCache.get(userId);
        if (result == null) {
            result = IMDatabaseManager.getInstance().selectUserByJID(userId);
            if (result != null) {
                userCache.put(userId, result);
            }
        }
        return result;
    }

    public void setNickToCache(Nick nick) {
        nickCache.put(nick.getXmppId(), nick);
    }

    public void getCollectionUserInfoByUserId(XMPPJID targe, final boolean enforce, boolean toDB, final NickCallBack nickCallBack) {
        if (targe == null) return;
        //????????????id
        final String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return;
        }
        Nick targeResult = null;
        if (!enforce) {
            targeResult = nickCache.get(targeId);
            //????????????id????????????
            if (toDB || targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getDescInfo()) || TextUtils.isEmpty(targeResult.getName())) {
                //?????????????????????
                targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionUserByJID(targeId).toString(), Nick.class);
                if (targeResult != null && !TextUtils.isEmpty(targeResult.getXmppId())) {
                    nickCache.put(targeId, targeResult);
                    nickCallBack.onNickCallBack(targeResult);
                    return;
                }
                //???????????????????????????
                if (targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    //???????????????
                    try {
                        IMUserCardManager.getInstance().updateCollectionUserCard(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {
                            @Override
                            public void onComplate(String stat) {
                                Nick nick = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionUserByJID(targeId).toString(), Nick.class);
                                if (stat.equals("success")) {

                                    if (nick != null && !TextUtils.isEmpty(nick.getXmppId())) {
                                        if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                            nick.setHeaderSrc(defaultUserImage);
                                        }
                                        if (TextUtils.isEmpty(nick.getXmppId())) {
                                            nick.setXmppId(targeId);
                                        }
                                        if (TextUtils.isEmpty(nick.getDescInfo())) {
                                            nick.setDescInfo("???");
                                        }
                                        if (TextUtils.isEmpty(nick.getName())) {
                                            nick.setName(targeId);
                                        }
                                        nickCache.put(targeId, nick);
                                        final Nick finalNick = nick;
                                        QunarIMApp.mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                nickCallBack.onNickCallBack(finalNick);
                                            }
                                        });
                                    }
                                } else {
//                                    Nick n = new Gson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);
                                    if (nick == null) {
                                        nick = new Nick();
                                        nick.setXmppId(targeId);
                                        nick.setHeaderSrc(defaultUserImage);
                                        nick.setDescInfo("???");
                                        nick.setName(targeId);
                                    } else {
                                        if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                            nick.setHeaderSrc(defaultUserImage);
                                        }
                                        if (TextUtils.isEmpty(nick.getXmppId())) {
                                            nick.setXmppId(targeId);
                                        }
                                        if (TextUtils.isEmpty(nick.getDescInfo())) {
                                            nick.setDescInfo("???");
                                        }
                                        if (TextUtils.isEmpty(nick.getName())) {
                                            nick.setName(targeId);
                                        }
                                    }
                                    nickCache.put(targeId, nick);
                                    final Nick finalNick = nick;
                                    QunarIMApp.mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            nickCallBack.onNickCallBack(finalNick);
                                        }
                                    });

                                }
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                nickCallBack.onNickCallBack(targeResult);
            }
        } else {
            try {
                IMUserCardManager.getInstance().updateCollectionUserCard(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {

                    @Override
                    public void onComplate(String stat) {
                        Nick nick = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionUserByJID(targeId).toString(), Nick.class);
                        if (stat.equals("success")) {

                            if (nick != null && !TextUtils.isEmpty(nick.getXmppId())) {
                                if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                    nick.setHeaderSrc(defaultUserImage);
                                }
                                if (TextUtils.isEmpty(nick.getXmppId())) {
                                    nick.setXmppId(targeId);
                                }
                                if (TextUtils.isEmpty(nick.getDescInfo())) {
                                    nick.setDescInfo("???");
                                }
                                if (TextUtils.isEmpty(nick.getName())) {
                                    nick.setName(targeId);
                                }
                                nickCache.put(targeId, nick);
                                final Nick finalNick = nick;
                                QunarIMApp.mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        nickCallBack.onNickCallBack(finalNick);
                                    }
                                });
                            }
                        } else {
//                            Nick n = new Gson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);
                            if (nick == null) {
                                nick = new Nick();
                                nick.setXmppId(targeId);
                                nick.setHeaderSrc(defaultUserImage);
                                nick.setDescInfo("???");
                                nick.setName(targeId);
                            } else {
                                if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                    nick.setHeaderSrc(defaultUserImage);
                                }
                                if (TextUtils.isEmpty(nick.getXmppId())) {
                                    nick.setXmppId(targeId);
                                }
                                if (TextUtils.isEmpty(nick.getDescInfo())) {
                                    nick.setDescInfo("???");
                                }
                                if (TextUtils.isEmpty(nick.getName())) {
                                    nick.setName(targeId);
                                }
                            }
                            nickCache.put(targeId, nick);
                            final Nick finalNick = nick;
                            QunarIMApp.mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    nickCallBack.onNickCallBack(finalNick);
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<UserHaveMedalStatus> getUserMedalList(String xmppid) {
        List<UserHaveMedalStatus> list = null;
        list = userMedalCache.get(xmppid);
        if(list == null){
            list = IMDatabaseManager.getInstance().selectUserWearMedalStatusByUserid(QtalkStringUtils.parseId(xmppid),QtalkStringUtils.parseDomain(xmppid));
            if(list!=null&&list.size()>0){
                userMedalCache.put(xmppid,list);
            }else{
                list = null;
            }
        }
        return list;
    }

    public void deleteMedalCache(MedalUserStatusResponse medalListResponse) {
        if(userMedalCache==null){
            return;
        }
        for (int i = 0; i < medalListResponse.getData().getUserMedals().size(); i++) {
            userMedalCache.remove(medalListResponse.getData().getUserMedals().get(i).getUserId()
                    +"@"+medalListResponse.getData().getUserMedals().get(i).getHost());
        }
    }


    //??????????????????
    public interface NickCallBack {
        void onNickCallBack(Nick nick);
    }

    //????????? enforce ???false???, toDB??????????????????????????????????????????????????????,???????????????????????????????????????????????????????????????
    public void getMucInfoByGroupId(XMPPJID targe, XMPPJID myself, final boolean enforce, boolean toDB, final NickCallBack nickCallBack) {
        if (targe == null) return;

        final String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return;
        }

        Nick targeResult;
        if (!enforce) {
            targeResult = nickCache.get(targeId);
            //????????????id????????????
            if (toDB || (targeResult == null || TextUtils.isEmpty(targeResult.getGroupId()) || TextUtils.isEmpty(targeResult.getHeaderSrc()))) {
                //?????????????????????
                targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectMucByGroupId(targeId).toString(), Nick.class);
                if (targeResult != null && !TextUtils.isEmpty(targeResult.getGroupId()) && !TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    nickCache.put(targeId, targeResult);
                    nickCallBack.onNickCallBack(targeResult);
                    return;
                }
                //???????????????????????????
                if (targeResult == null || TextUtils.isEmpty(targeResult.getGroupId()) || TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    //???????????????
                    updateGroupInfoFromNet(targeId, enforce, nickCallBack);
                }

            } else {
                nickCallBack.onNickCallBack(targeResult);
            }
        } else {
            updateGroupInfoFromNet(targeId, enforce, nickCallBack);
        }

    }

    public void updateGroupInfoFromNet(final String targeId, final boolean enforce, final NickCallBack nickCallBack) {
        DispatchHelper.Async("getGroupInfo", false, new Runnable() {
            @Override
            public void run() {
                try {
                    IMUserCardManager.getInstance().updateMucCardSync(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {
                        @Override
                        public void onComplate(String stat) {
                            Nick n;
                            if (stat.equals("success")) {
                                n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectMucByGroupId(targeId).toString(), Nick.class);
                                if (n != null && !TextUtils.isEmpty(n.getGroupId())) {
                                    if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                        n.setHeaderSrc(defaultMucImage);
                                    }
                                    if (TextUtils.isEmpty(n.getGroupId())) {
                                        n.setGroupId(targeId);
                                    }
                                    if (TextUtils.isEmpty(n.getDescInfo())) {
                                        n.setDescInfo("???");
                                    }
                                    if (TextUtils.isEmpty(n.getName())) {
                                        n.setName(targeId);
                                    }
                                }
                            } else {
                                n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectMucByGroupId(targeId).toString(), Nick.class);
                                if (n == null) {
                                    n = new Nick();
                                    n.setGroupId(targeId);
                                    n.setHeaderSrc(defaultMucImage);
                                    n.setDescInfo("???");
                                    n.setName(targeId);
                                } else {
                                    if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                        n.setHeaderSrc(defaultMucImage);
                                    }
                                    if (TextUtils.isEmpty(n.getGroupId())) {
                                        n.setGroupId(targeId);
                                    }
                                    if (TextUtils.isEmpty(n.getDescInfo())) {
                                        n.setDescInfo("???");
                                    }
                                    if (TextUtils.isEmpty(n.getName())) {
                                        n.setName(targeId);
                                    }
                                }

                            }
                            nickCache.put(targeId, n);
                            final Nick finalNick = n;
                            QunarIMApp.mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    nickCallBack.onNickCallBack(finalNick);
                                }
                            });
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getCollectionMucInfoByGroupId(XMPPJID targe, boolean enforce, boolean toDB, final NickCallBack nickCallBack) {
        if (targe == null) return;

        final String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return;
        }

        Nick targeResult = null;
        if (!enforce) {


            targeResult = nickCache.get(targeId);
            //????????????id????????????
            if (toDB || (targeResult == null || TextUtils.isEmpty(targeResult.getGroupId()) || TextUtils.isEmpty(targeResult.getHeaderSrc()))) {
                //?????????????????????
                targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionMucByGroupId(targeId).toString(), Nick.class);
                if (targeResult != null && !TextUtils.isEmpty(targeResult.getGroupId()) && !TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    nickCache.put(targeId, targeResult);
                    nickCallBack.onNickCallBack(targeResult);
                    return;
                }
                //???????????????????????????
                if (targeResult == null || TextUtils.isEmpty(targeResult.getGroupId()) || TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    //???????????????
                    try {

                        IMUserCardManager.getInstance().updateCollectionMucCard(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {
                            @Override
                            public void onComplate(String stat) {
                                Nick n;
                                if (stat.equals("success")) {
                                    n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionMucByGroupId(targeId).toString(), Nick.class);
                                    if (n != null && !TextUtils.isEmpty(n.getGroupId())) {
                                        if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                            n.setHeaderSrc(defaultMucImage);
                                        }
                                        if (TextUtils.isEmpty(n.getGroupId())) {
                                            n.setGroupId(targeId);
                                        }
                                        if (TextUtils.isEmpty(n.getDescInfo())) {
                                            n.setDescInfo("???");
                                        }
                                        if (TextUtils.isEmpty(n.getName())) {
                                            n.setName(targeId);
                                        }
                                    }
//                                    targeResult[0] = new Gson().fromJson(IMDatabaseManager.getInstance().selectMucByGroupId(targeId).toString(),Nick.class);
//                                    if (targeResult[0] != null &&  !TextUtils.isEmpty(targeResult[0].getGroupId())) {
//                                        nickCache.put(targeId, targeResult[0]);
//                                        nickCallBack.onNickCallBack(targeResult[0]);
//                                    }
                                } else {
//                                    Nick n = new Nick();
//                                    n.setXmppId(targeId);
//                                    n.setHeaderSrc(defaultMucImage);
//                                    nickCache.put(targeId, n);
//                                    nickCallBack.onNickCallBack(n);


                                    n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionMucByGroupId(targeId).toString(), Nick.class);
                                    if (n == null) {
                                        n = new Nick();
                                        n.setGroupId(targeId);
                                        n.setHeaderSrc(defaultMucImage);
                                        n.setDescInfo("???");
                                        n.setName(targeId);
                                    } else {
                                        if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                            n.setHeaderSrc(defaultMucImage);
                                        }
                                        if (TextUtils.isEmpty(n.getGroupId())) {
                                            n.setGroupId(targeId);
                                        }
                                        if (TextUtils.isEmpty(n.getDescInfo())) {
                                            n.setDescInfo("???");
                                        }
                                        if (TextUtils.isEmpty(n.getName())) {
                                            n.setName(targeId);
                                        }
                                    }

                                }
                                nickCache.put(targeId, n);
                                final Nick finalNick = n;
                                QunarIMApp.mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        nickCallBack.onNickCallBack(finalNick);
                                    }
                                });
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                nickCallBack.onNickCallBack(targeResult);
            }
        } else {
            try {
                IMUserCardManager.getInstance().updateCollectionMucCard(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {
                    @Override
                    public void onComplate(String stat) {
                        if (stat.equals("success")) {
                            Nick n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionMucByGroupId(targeId).toString(), Nick.class);
                            if (n != null && !TextUtils.isEmpty(n.getGroupId())) {
                                if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                    n.setHeaderSrc(defaultMucImage);
                                }
                                if (TextUtils.isEmpty(n.getGroupId())) {
                                    n.setGroupId(targeId);
                                }
                                if (TextUtils.isEmpty(n.getDescInfo())) {
                                    n.setDescInfo("???");
                                }
                                if (TextUtils.isEmpty(n.getName())) {
                                    n.setName(targeId);
                                }
                                nickCache.put(targeId, n);
                                final Nick finalNick = n;
                                QunarIMApp.mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        nickCallBack.onNickCallBack(finalNick);
                                    }
                                });
                            }
                        } else {
                            Nick n = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectCollectionMucByGroupId(targeId).toString(), Nick.class);
                            if (n == null) {
                                n = new Nick();
                                n.setGroupId(targeId);
                                n.setHeaderSrc(defaultMucImage);
                                n.setDescInfo("???");
                                n.setName(targeId);
                            } else {
                                if (TextUtils.isEmpty(n.getHeaderSrc())) {
                                    n.setHeaderSrc(defaultMucImage);
                                }
                                if (TextUtils.isEmpty(n.getGroupId())) {
                                    n.setGroupId(targeId);
                                }
                                if (TextUtils.isEmpty(n.getDescInfo())) {
                                    n.setDescInfo("???");
                                }
                                if (TextUtils.isEmpty(n.getName())) {
                                    n.setName(targeId);
                                }
                            }

                            final Nick finalNick = n;
                            QunarIMApp.mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    nickCallBack.onNickCallBack(finalNick);
                                }
                            });
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    //?????????????????????UserInfo ??????jid ??????jid ??????????????????
    public void getUserInfoByUserId(XMPPJID targe, XMPPJID myself, final boolean enforce, boolean toDB, final NickCallBack nickCallBack) {
        if (targe == null) {
            Nick nick = new Nick();
            nick.setXmppId("");
            nick.setHeaderSrc(defaultUserImage);
            nick.setDescInfo("???");
            nick.setName("");
            nick.setMark("");
            nick.setMood("");
            nickCallBack.onNickCallBack(nick);
            return;
        }
        //????????????id
        String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return;
        }
        //??????hotline????????????.?????????????????????????????? add by hubo at 2018/8/29
//        targeId = ConnectionUtil.getInstance().getHotlineJid(targeId);
        if (!enforce) {
            Nick targeResult = nickCache.get(targeId);
            //????????????id????????????
            if (toDB || targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getDescInfo()) || TextUtils.isEmpty(targeResult.getName())) {
                targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);

                if (targeResult != null && !TextUtils.isEmpty(targeResult.getXmppId()) && !TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    targeResult.setMark(ConnectionUtil.getInstance().getMarkupNameById(targeId));
                    nickCache.put(targeId, targeResult);
                    nickCallBack.onNickCallBack(targeResult);
                    return;
                }
                //???????????????????????????
                if (targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                    //???????????????
                    updateUserInfoFromNet(targeId, enforce, nickCallBack);
                }

            } else {
                nickCallBack.onNickCallBack(targeResult);
            }
        } else {
            //???????????????
            updateUserInfoFromNet(targeId, enforce, nickCallBack);
        }
    }

    //?????????????????????UserInfo ????????????
    public void getUserInfoByUserId(XMPPJID targe,final NickCallBack nickCallBack) {
        if (targe == null) {
            Nick nick = new Nick();
            nick.setXmppId("");
            nick.setHeaderSrc(defaultUserImage);
            nick.setDescInfo("???");
            nick.setName("");
            nick.setMark("");
            nick.setMood("");
            nickCallBack.onNickCallBack(nick);
            return;
        }
        //????????????id
        String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return;
        }
        Nick targeResult = nickCache.get(targeId);
        //????????????id????????????
        if (targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getDescInfo()) || TextUtils.isEmpty(targeResult.getName())) {
            targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);
            if (targeResult != null && !TextUtils.isEmpty(targeResult.getXmppId()) && !TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                targeResult.setMark(ConnectionUtil.getInstance().getMarkupNameById(targeId));
                nickCache.put(targeId, targeResult);
                nickCallBack.onNickCallBack(targeResult);
                return;
            }else {
                nickCallBack.onNickCallBack(targeResult);
            }

        } else {
            nickCallBack.onNickCallBack(targeResult);
        }
    }

    public void updateUserInfoFromNet(final String targeId, final boolean enforce, final NickCallBack nickCallBack) {
        DispatchHelper.Async("getUserInfo", false, new Runnable() {
            @Override
            public void run() {
                //?????????????????????
                try {
                    IMUserCardManager.getInstance().updateUserCardSync(targeId, enforce, new IMUserCardManager.InsertDataBaseCallBack() {
                        @Override
                        public void onComplate(String stat) {
                            Nick nick = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);
                            if (stat.equals("success")) {

                                if (nick != null && !TextUtils.isEmpty(nick.getXmppId())) {
                                    if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                        nick.setHeaderSrc(defaultUserImage);
                                    }
                                    if (TextUtils.isEmpty(nick.getXmppId())) {
                                        nick.setXmppId(targeId);
                                    }
                                    if (TextUtils.isEmpty(nick.getDescInfo())) {
                                        nick.setDescInfo("???");
                                    }
                                    if (TextUtils.isEmpty(nick.getName())) {
                                        nick.setName(targeId);
                                    }
                                    nick.setMark(ConnectionUtil.getInstance().getMarkupNameById(targeId));
                                    nickCache.put(targeId, nick);
                                    final Nick finalNick = nick;
                                    QunarIMApp.mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            nickCallBack.onNickCallBack(finalNick);
                                        }
                                    });
                                }
                            } else {
                                if (nick == null) {
                                    nick = new Nick();
                                    nick.setXmppId(targeId);
                                    nick.setHeaderSrc(defaultUserImage);
                                    nick.setDescInfo("???");
                                    nick.setName(targeId);
                                    nick.setMark(ConnectionUtil.getInstance().getMarkupNameById(targeId));
                                } else {
                                    if (TextUtils.isEmpty(nick.getHeaderSrc())) {
                                        nick.setHeaderSrc(defaultUserImage);
                                    }
                                    if (TextUtils.isEmpty(nick.getXmppId())) {
                                        nick.setXmppId(targeId);
                                    }
                                    if (TextUtils.isEmpty(nick.getDescInfo())) {
                                        nick.setDescInfo("???");
                                    }
                                    if (TextUtils.isEmpty(nick.getName())) {
                                        nick.setName(targeId);
                                    }
                                    nick.setMark(ConnectionUtil.getInstance().getMarkupNameById(targeId));
                                }
                                nickCache.put(targeId, nick);
                                final Nick finalNick = nick;
                                QunarIMApp.mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        nickCallBack.onNickCallBack(finalNick);
                                    }
                                });

                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public Nick getNickkk(String jid) {
        if (TextUtils.isEmpty(jid)) {
            return null;
        }
        Nick targeResult = null;
        targeResult = nickCache.get(jid);
        if (targeResult != null) {
            if (TextUtils.isEmpty(targeResult.getHeaderSrc())) {
                targeResult.setHeaderSrc(defaultUserImage);
                nickCache.put(jid, targeResult);
            }
            if (TextUtils.isEmpty(targeResult.getXmppId())) {
                targeResult.setXmppId(jid);
                nickCache.put(jid, targeResult);
            }
            if (TextUtils.isEmpty(targeResult.getDescInfo())) {
                targeResult.setDescInfo("???");
                nickCache.put(jid, targeResult);
            }
            if (TextUtils.isEmpty(targeResult.getName())) {
                targeResult.setName(QtalkStringUtils.parseId(jid));
                nickCache.put(jid, targeResult);
            }
            return targeResult;
        } else {
            return null;
        }
    }

    //?????????????????????UserInfo ??????jid ??????jid ??????????????????
    public Nick getNickById(XMPPJID targe) {
        if (targe == null) return new Nick();
        //????????????id
        final String targeId = targe.fullname();
//        Logger.i("targeId:" + targeId);
        //????????????id???????????????????????????
        if (TextUtils.isEmpty(targeId)) {
            return new Nick();
        }
        Nick targeResult = null;
        targeResult = nickCache.get(targeId);
        //????????????id????????????
        if (targeResult == null || TextUtils.isEmpty(targeResult.getXmppId()) || TextUtils.isEmpty(targeResult.getName())) {
            //?????????????????????
            targeResult = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(targeId).toString(), Nick.class);
            if (targeResult != null && !TextUtils.isEmpty(targeResult.getXmppId())) {
//                nickCache.put(targeId, targeResult);
//                return targeResult;
            } else {
                targeResult = new Nick();
                targeResult.setXmppId(targeId);
                targeResult.setHeaderSrc(defaultUserImage);
            }
        }

        if (TextUtils.isEmpty(targeResult.getHeaderSrc())) {
            targeResult.setHeaderSrc(defaultUserImage);
        }
        if (TextUtils.isEmpty(targeResult.getXmppId())) {
            targeResult.setXmppId(targeId);
        }
        if (TextUtils.isEmpty(targeResult.getDescInfo())) {
            targeResult.setDescInfo("???");
        }
        if (TextUtils.isEmpty(targeResult.getName())) {
            targeResult.setName(QtalkStringUtils.parseId(targeId));
        }
        nickCache.put(targeId, targeResult);

        return targeResult;

    }

    public void mandatorySendMessage(ProtoMessageOuterClass.ProtoMessage protoMessage) {
        try {
            _pbSocket.sendProtoMessage(protoMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isAuthenticated() {
        return _pbSocket.isAuthenticated();
    }

    public void sendMessage(ProtoMessageOuterClass.ProtoMessage protoMessage) {
        //?????????????????????
        if (_pbSocket.isAuthenticated()) {
            try {
                //2 6 7??????????????? ?????? ????????????,????????????????????????????????????
                //????????????????????????????????????,????????????????????????

                Logger.i("????????????,????????????");
                _pbSocket.sendProtoMessage(protoMessage);
            } catch (Exception e) {
                Logger.i("???????????????,????????????");
                e.printStackTrace();
                //?????????????????????????????????,????????????????????????
//                reConnection();
                //??????????????????????????????????????????????????????
                IMDatabaseManager.getInstance().UpdateChatStateMessage(protoMessage, MessageStatus.LOCAL_STATUS_FAILED);

                try {
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Connect_Interrupt, "");
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, ProtoMessageOuterClass.XmppMessage.parseFrom(protoMessage.getMessage()).getMessageId());
//                    reConnection();
                    //                    ????????????????????????id
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(ProtoMessageOuterClass.XmppMessage.parseFrom(message.getMessage()).getMessageId(), message);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }

            }
        } else {
            //??????????????????
            Logger.i("??????????????????:" + _pbSocket.isAuthenticated());
            //????????????????????????
            // reConnection();
            //??????????????????????????????????????????????????????
            IMDatabaseManager.getInstance().UpdateChatStateMessage(protoMessage, MessageStatus.LOCAL_STATUS_FAILED);

            try {
//                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Connect_Interrupt, "");
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_Failed, ProtoMessageOuterClass.XmppMessage.parseFrom(protoMessage.getMessage()).getMessageId());
//                reConnection();
                //                    ????????????????????????id
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(ProtoMessageOuterClass.XmppMessage.parseFrom(message.getMessage()).getMessageId(), message);
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }

    /**
     * ??????
     */
    public void reConnection() {
        if (_pbSocket != null) {
            Logger.i("????????? " + _pbSocket.isReconnecting() + "??????????????????" + _pbSocket.isConnecting());
            connectedHandler.sendEmptyMessageDelayed(WHAT, 3 * 1000);
        }
    }

    /**
     * ????????????
     */
    public synchronized void reConnectionForce() {
        if (_pbSocket != null && _pbSocket.isConnected()) {
            return;
        }
        QtalkSDK.getInstance().login(true);
    }

    public void shutdown() {
        if (_pbSocket != null) {
            Logger.i("TID:" + android.os.Process.myTid() + "????????????");
            _pbSocket.shutdown();
        }

    }

    //??????????????????
    public ProtoMessageOuterClass.IQMessage get_virtual_user_real(String jid) {
        if (_pbSocket.isAuthenticated()) {
            ProtoMessageOuterClass.IQMessage iqMessage = ProtoMessageOuterClass.IQMessage.newBuilder()
                    .setDefinedKey(ProtoMessageOuterClass.IQMessageKeyType.IQKeyStartSession)
                    .setMessageId(StringUtils.UUIDString())
                    .setValue(jid)
                    .build();
            try {
                ProtoMessageOuterClass.IQMessage result = _pbSocket.syncSendIQMessage(iqMessage, CurrentPreference.getInstance().getPreferenceUserId(), true);
                if (result != null && result.hasDefinedKey() && result.getDefinedKey() == ProtoMessageOuterClass.IQMessageKeyType.IQKeyResult) {
                    Logger.i("??????????????????:" + result.toString());
//                    delegate.onVirtualUserResult(result, "success");
                    return result;
                }
            } catch (Exception e) {
                Logger.e(e, "get_virtual_user_real error");
                Logger.e("????????????:" + e.toString());
            }
        }

        return null;
    }


    //??????????????????List
    public ProtoMessageOuterClass.IQMessage get_virtual_user_role() {
        //???????????????????????????
        if (_pbSocket.isAuthenticated()) {
            //??????iqMessage
            ProtoMessageOuterClass.IQMessage iqMessage = ProtoMessageOuterClass.IQMessage.newBuilder()
                    .setDefinedKey(ProtoMessageOuterClass.IQMessageKeyType.IQKeyGetVUser)
                    .setMessageId(StringUtils.UUIDString())
                    .build();
            Logger.i("????????????1:" + iqMessage);
            Logger.i("??????iq????????????messageId:" + iqMessage.getMessageId());
            try {
//                delegate.onVirtualUserResult(null,"failed");
                Logger.i("????????????1:" + CurrentPreference.getInstance().getPreferenceUserId());
                ProtoMessageOuterClass.IQMessage result = _pbSocket.syncSendIQMessage(iqMessage, CurrentPreference.getInstance().getPreferenceUserId(), true);

                Logger.i("???????????????????????????:" + result);
//                if (result != null) {
//                    Logger.i("result?????????:" + result);
//                    Logger.i("result.hasDefinedKey" + result.hasDefinedKey());
//                    if (result.hasDefinedKey()) {
//                        Logger.i("result.hasDefinedKey" + result.hasDefinedKey());
//                        Logger.i("result.getDefinedKey" + result.getDefinedKey());
//                        if (result.getDefinedKey() == ProtoMessageOuterClass.IQMessageKeyType.IQKeyResult) {
//                            Logger.i("??????????????????:" + result);
//                            delegate.onVirtualUserResult(result, "success");
//                        }
//                    }
//                }
                if (result != null && result.hasDefinedKey() && result.getDefinedKey() == ProtoMessageOuterClass.IQMessageKeyType.IQKeyResult) {
                    Logger.i("??????????????????:" + result.toString());
//                    delegate.onVirtualUserResult(result, "success");
                    return result;
                }
            } catch (Exception e) {
                Logger.e(e, "get_virtual_user_role_list error");
                Logger.e("????????????:" + e.toString());
//                delegate.onVirtualUserResult(null, "faild");

            }

        }
        return null;
    }

    /**
     * ???????????????????????? ????????????
     * @return
     */
    public String getRemoteLoginKey() {
//        if (StringUtils.isEmpty(remoteLoginKey)) {
        if (_protocolType == IMProtocol.PROTOCOL_PROTOBUF) {
            return _pbSocket.getRemoteLoginKey();
        } else {
            throw new UnsupportedOperationException("getRemoteLoginKey, XmppStack is Not yet implemented");
        }
//        } else
//            return remoteLoginKey;
    }

    public String getRemoteLoginKey(boolean mandatory) {
//        if (StringUtils.isEmpty(remoteLoginKey)) {
        if (_protocolType == IMProtocol.PROTOCOL_PROTOBUF) {
            return _pbSocket.getRemoteLoginKey(mandatory);
        } else {
            throw new UnsupportedOperationException("getRemoteLoginKey, XmppStack is Not yet implemented");
        }
//        } else
//            return remoteLoginKey;
    }

    /**
     * ??????serverKey
     */
    public String clearAndGetRemoteLoginKey() {
        _pbSocket.clearRemoteLoginKey();
        CurrentPreference.getInstance().setVerifyKey("");
        return getRemoteLoginKey();
    }


    public void login(String username, String password) throws IOException {
        if (_protocolType == IMProtocol.PROTOCOL_PROTOBUF) {
            _pbSocket.setHostName(QtalkNavicationService.getInstance().getXmppHost());
            _pbSocket.setDomain(QtalkNavicationService.getInstance().getXmppdomain());
            _pbSocket.setHostPort(QtalkNavicationService.getInstance().getProtobufPort());
            _pbSocket.setUsername(username);
            _pbSocket.setPassword(password);
            _pbSocket.setVersion(GlobalConfigManager.getPBVersion());
            _pbSocket.setPlatForm(GlobalConfigManager.getAppPlatform());
            Logger.i("???_pbSocket???????????????????????????");

            if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
                //resource ???????????????????????????????????????,?????????????????????????????????
//                connectedList = new HashMap<>();
                connectedHandler.removeMessages(WHAT);
                _pbSocket.connect();
            }
        } else {
        }
    }

    /**
     * ???????????????????????????,???????????????????????????
     */
    public void clearLastUserInfo() {
        IMUserDefaults.getStandardUserDefaults().newEditor(GlobalConfigManager.getGlobalContext())
                .removeObject(Constants.Preferences.usertoken)
                .synchronize();
        //username??????sp
        IMUserDefaults.getStandardUserDefaults().newEditor(GlobalConfigManager.getGlobalContext())
                .removeObject(Constants.Preferences.lastuserid)
                .synchronize();
    }


    @Override
    //????????????????????????????????????
    public void onChatMessageReceived(ProtoMessageOuterClass.ProtoMessage message) {
        switch (message.getSignalType()) {
//              6 ????????????????????????:
            case ProtoMessageOuterClass.SignalType.SignalTypeChat_VALUE:
                try {
                    if (message.getFrom().contains("@conference")) {
                        return;
                    }
                    //??????????????????????????? Im_message???
                    IMMessage newChatMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");

                    //??????????????????
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text, newChatMessage);
//                IMDatabaseManager.getInstance().InsertChatMessage(message, "1", "0");
                    IMDatabaseManager.getInstance().InsertChatMessage(newChatMessage, true);
                    //??????????????????????????? IM_SessionList???F
                    IMDatabaseManager.getInstance().InsertIMSessionList(newChatMessage, false);
//                IMDatabaseManager.getInstance().InsertSessionList(message);

                    //??????????????????
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text_After_DB, newChatMessage);

//                    if (newChatMessage.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_Video_VALUE
//                            || newChatMessage.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_Audio_VALUE) {//???????????????
//                        ConnectionUtil.getInstance().lanuchChatVideo(newChatMessage.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_Video_VALUE,
//                                message.getFrom(),
//                                CurrentPreference.getInstance().getUserid());
//                    }

                    if (!newChatMessage.isCarbon()) {
                        JSONObject jb = new JSONObject();
                        jb.put("id", newChatMessage.getMessageId());
                        JSONArray ja = new JSONArray();
                        ja.put(jb);
                        ProtoMessageOuterClass.ProtoMessage receive = PbAssemblyUtil.getBeenNewReadStateMessage(MessageStatus.STATUS_SINGLE_DELIVERED + "", ja, newChatMessage.getFromID(), myself);
                        mandatorySendMessage(receive);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
//                //todo:???????????? ???????????????
                break;
            //7 ????????????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeGroupChat_VALUE:

                //??????????????????????????? Im_message???
                IMMessage newGroupChatMessage = PbParseUtil.parseReceiveGroupChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");

                //??????????????????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Group_Chat_Message_Text, newGroupChatMessage);

                IMDatabaseManager.getInstance().InsertChatMessage(newGroupChatMessage, true);
//                IMDatabaseManager.getInstance().InsertGroupChatMessage(message, "1", "0");
                //??????????????????????????? IM_SessionList???
                IMDatabaseManager.getInstance().InsertIMSessionList(newGroupChatMessage, false);
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Group_Chat_Message_Text_After_DB, newGroupChatMessage);

//                if((newGroupChatMessage.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_Video_Group_VALUE ||
//                        newGroupChatMessage.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_VideoMeeting_VALUE)){
//                    final String roomId = QtalkStringUtils.parseIdAndDomain(message.getFrom());
//                    ConnectionUtil.getInstance().getMucCard(roomId, new NickCallBack() {
//                        @Override
//                        public void onNickCallBack(Nick nick) {
//                            ConnectionUtil.getInstance().lanuchGroupVideo(roomId,nick.getName());
//                        }
//                    },false,false);
//                }
//                IMDatabaseManager.getInstance().InsertSessionList(message);
                break;
            //16  ????????????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeMState_VALUE:
//                //todo  ???????????????????????????????????????????????????1
//                //????????????????????????????????????????????????
                IMMessage stateMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_SUCCESS + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");
                IMDatabaseManager.getInstance().UpdateChatStateMessage(stateMessage, false);

                try {
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_State, stateMessage);

                    //                    ????????????????????????id
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(ProtoMessageOuterClass.XmppMessage.parseFrom(message.getMessage()).getMessageId(), message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            // 13 ????????????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeReadmark_VALUE:
                // TODO: 2017/9/20 ???????????????????????????????????????pb??????immessage ?????????,????????????
                try {
                    IMMessage newReadMessage = PbParseUtil.parseReceiveReadMessage(message);

                    //??????????????????ReadMark?????? ????????????ReadMark????????????
                    if (newReadMessage.getCollectionType() == ConversitionType.MSG_TYPE_GROUP) {
                        String time = String.valueOf(newReadMessage.getTime().getTime());
                        IMDatabaseManager.getInstance().updateGroupReadMarkTime(time);
                        Logger.i("SignalTypeReadmark_VALUE:" + time);
                    }
                    //????????????????????????,?????????????????????,??????????????? ?????? ???????????????????????????
                    //???????????????????????????????????????????????????,???????????????
                    IMDatabaseManager.getInstance().UpdateChatReadTypeMessage(newReadMessage);
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Read_State, newReadMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Message_Read_Mark, "HaveUpdate");
                break;
            //10 ????????????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeTyping_VALUE:
                //????????????????????????????????????????????????
                IMMessage inputMessage = PbParseUtil.parseReceiveChatMessage(message, "0", "0");
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Input, inputMessage);
                break;
            //14 ????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeRevoke_VALUE:
                RevokeInfo revokeInfo = PbParseUtil.parseRevokeMessage(message);
                if (revokeInfo.getMessageType().equals(String.valueOf(ProtoMessageOuterClass.MessageType.MessageTypeRevoke_VALUE))
                || revokeInfo.getMessageType().equals(String.valueOf(ProtoMessageOuterClass.MessageType.MessageTypeConsultRevoke_VALUE))) {
                    IMMessage imMessage = new IMMessage();
                    imMessage.setDirection(IMMessage.DIRECTION_MIDDLE);
                    imMessage.setId(revokeInfo.getMessageId());
                    imMessage.setMessageID(revokeInfo.getMessageId());
                    Nick nick = JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(revokeInfo.getFromId()).toString(), Nick.class);
                    imMessage.setBody(nick.getName() + "?????????????????????");
                    IMDatabaseManager.getInstance().UpdateRevokeChatMessage(revokeInfo.getMessageId(), nick.getName() + "?????????????????????", Integer.valueOf(revokeInfo.getMessageType()));
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Revoke, imMessage);
//                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Revoke, revokeInfo);
                }

                break;
            //2 3 iq??????
            case ProtoMessageOuterClass.SignalType.SignalTypeIQ_VALUE:
            case ProtoMessageOuterClass.SignalType.SignalTypeIQResponse_VALUE:
                try {
                    ProtoMessageOuterClass.IQMessage iqMessage = ProtoMessageOuterClass.IQMessage.parseFrom(message.getMessage());
                    if (iqMessage.hasDefinedKey()) {
                        switch (iqMessage.getValue()) {
                            case QtalkEvent.Ping:
//                                String msgId = iqMessage.getMessageId();
//                                connectedList.remove(msgId);
                                connectedHandler.removeMessages(WHAT);
                                break;
                            case QtalkEvent.Muc_Invite_User_V2:
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Muc_Invite_User_V2, message.getFrom());
                                break;
                            case QtalkEvent.IQ_CREATE_MUC:
                                //??????????????????????????????
                                String str = iqMessage.getBody().getValue();
                                if (str.equals("success")) {
                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.IQ_CREATE_MUC, message.getFrom());
                                }
                                break;
                            //???????????????
                            case QtalkEvent.IQ_GET_MUC_USER:
                                IMDatabaseManager.getInstance().insertUpdateGroupMembers(message);
                                String groupId = message.getFrom();
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Group_Member_Update, groupId);
                                //TODO ????????????
//                                List<GroupMember> memberList = IMDatabaseManager.getInstance().SelectGroupMemberByGroupId(groupId);
//                                try {
//                                    IMUserCardManager.getInstance().updateUserCardByMemberList(memberList, false, new IMUserCardManager.InsertDataBaseCallBack() {
//                                        @Override
//                                        public void onComplate(String stat) {
//                                            if ("success".equals(stat)) {
//                                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Group_Member_Update, groupId);
//                                            }
//                                        }
//                                    });
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
                                break;
                            //??????
//                            case QtalkEvent.IQ_LEAVE_GROUP:
//                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.IQ_LEAVE_GROUP, message);
//                                break;
                            case QtalkEvent.USER_GET_FRIEND:
                                List<Nick> fList = PbParseUtil.parseGetFriends(message);

//                                fList = IMDatabaseManager.getInstance().SelectIMUserByFriendList(fList);
//                                if (fList == null || fList.size() <= 0) {
//                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.USER_GET_FRIEND, "succes");
//                                    return;
//                                }
                                if (fList != null) {
                                    Logger.i("????????????????????????:" + JsonUtils.getGson().toJson(fList));
                                }
                                long startUpdateFriend = System.currentTimeMillis();
                                IMDatabaseManager.getInstance().UpdateFriendListByList(fList);
                                Logger.i("??????????????????:" + (System.currentTimeMillis() - startUpdateFriend));
//                                IMDatabaseManager.getInstance().UpdateFriendListByList(fList);
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.USER_GET_FRIEND, "succes");
//                                List<Nick> nn = IMDatabaseManager.getInstance().SelectFriendList();
//                                Logger.i(new Gson().toJson(nn) +"??????????????????");
                                break;
                            case QtalkEvent.Get_Verify_Friend_Mode:
                                Map<String, String> mode = PbParseUtil.parseVerifyFriendMode(message);
                                EventBus.getDefault().post(new EventBusEvent.VerifyFriend(mode));
                                break;
                            case QtalkEvent.User_Del_Friend:
                                EventBus.getDefault().post(new EventBusEvent.FriendsChange(true));
                                break;
                        }
                    }
//                    else {
//                        //???????????????????????????????????????
//
//                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            //1 ???????????????
            case ProtoMessageOuterClass.SignalType.SignalTypePresence_VALUE:
                try {
                    ProtoMessageOuterClass.PresenceMessage presenceMessage = ProtoMessageOuterClass.PresenceMessage.parseFrom(message.getMessage());

                    if (presenceMessage.hasCategoryType()) {
                        switch (presenceMessage.getCategoryType()) {
                            case ProtoMessageOuterClass.CategoryType.CategoryOrganizational_VALUE:
                                Logger.i("???????????????????????????????????????");
                                if (CommonConfig.isQtalk) {
                                    LoginComplateManager.processBuddy();
//                                    LoginComplateManager.updateOrganizationsFromUrl();
                                } else {
                                    LoginComplateManager.getQchatDepInfo();
                                }
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryTickUser_VALUE:
                                Logger.i("??????????????????");
                                clearLastUserInfo();
                                shutdown();
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_FAILED, 0);
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryNavigation_VALUE:

                                Logger.i("??????????????????????????????");
                                String navname = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(QtalkNavicationService.NAV_CONFIG_CURRENT_NAME, "");
                                String oldNavString = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(navname, "");
                                NavConfigResult oldNav = JsonUtils.getGson().fromJson(oldNavString, NavConfigResult.class);
                                int oldVersion = !(TextUtils.isEmpty(oldNav.version)) ? Integer.valueOf(oldNav.version) : 0;
                                NavigationNotice navigationNotice = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), NavigationNotice.class);
                                Logger.i("???????????????:" + navigationNotice.getNavversion() + ",???????????????:" + oldVersion);
                                if (navigationNotice != null && !TextUtils.isEmpty(navigationNotice.getNavversion())) {
                                    if (Integer.parseInt(navigationNotice.getNavversion()) > oldVersion) {
                                        ConnectionUtil.getInstance().initNavConfig(true);
                                    }
                                }
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryGlobalNotification_VALUE:
                                Logger.i("??????????????????");
                                NoticeBean globalNotice = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), NoticeBean.class);
                                if (globalNotice == null) {
                                    return;
                                }
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.GLOBALNOTICE, globalNotice);
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategorySpecifyNotification_VALUE:
                                Logger.i("??????????????????");
                                NoticeBean specifyNotice = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), NoticeBean.class);
                                if (specifyNotice == null) {
                                    return;
                                }
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.SPECIFYNOTICE, specifyNotice);
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryConfigSync_VALUE:
                                Logger.i("??????????????????????????????");
                                VersionBean versionBean = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), VersionBean.class);
                                try {
                                    JSONObject jsonObject = new JSONObject(presenceMessage.getBody().getValue());
                                    if(jsonObject.has("forcequickreply")){
                                        if(jsonObject.getBoolean("forcequickreply")){
                                            LoginComplateManager.updateQuickReply(true);
                                        }
                                    }

                                    if(jsonObject.has("force") && jsonObject.getBoolean("force")){
                                        LoginComplateManager.updateUserServiceConfig(versionBean.isForce());
                                    }else{
                                        boolean isMy = CurrentPreference.getInstance().getResource().equals(versionBean.getResource());
                                        if (isMy) {
                                            return;
                                        }
                                        int newConfigVersion = versionBean.getVersion();
                                        int oldConfigVersion = IMDatabaseManager.getInstance().selectUserConfigVersion();
                                        if (newConfigVersion > oldConfigVersion) {
                                            LoginComplateManager.updateUserServiceConfig(false);
                                        }
                                    }

                                    if(jsonObject.has("forceOldSearch")){
                                        if(jsonObject.getBoolean("forceOldSearch")){
                                            IMDatabaseManager.getInstance().insertFocusSearchCacheData(true+"");
                                        }else{
                                            IMDatabaseManager.getInstance().insertFocusSearchCacheData(false+"");
                                        }
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryOPSNotification_VALUE:
                                Logger.i("??????ops????????????");
                                boolean isShow = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), OpsUnreadResult.DataBean.class).isHasUnread();
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.refreshOPSUnRead, isShow);
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryCalendarSync_VALUE:
                                Logger.i("????????????????????????");
                                CalendarVersion calendarVersion = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), CalendarVersion.class);
                                long oldCalendarVersion = IMDatabaseManager.getInstance().selectUserTripVersion();
                                long newCalendarVersion = Long.parseLong(calendarVersion.getUpdateTime());
                                if (newCalendarVersion > oldCalendarVersion) {
                                    LoginComplateManager.updateTripList();
                                } else {
                                    Logger.i("??????????????????,????????????:" + oldCalendarVersion + ";???????????????:" + newCalendarVersion);
                                }


                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryOnlineClientSync_VALUE:
                                Logger.i("????????????????????????");
                                boolean showHead = false;
                                String resource = presenceMessage.getBody().getValue();
                                if (resource != null) {
                                    resource = resource.toLowerCase();
                                    if (resource.contains("pc") || resource.contains("mac") || resource.contains("linux")) {
                                        showHead = true;
                                    } else {
                                        showHead = false;
                                    }
                                }
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.SHOW_HEAD, showHead);
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryClientSpecialNotice_VALUE:
                                try {
                                    JSONObject jsonObject = new JSONObject(presenceMessage.getBody().getValue());
                                    if (jsonObject != null) {
                                        boolean checkConfig = jsonObject.optBoolean("checkConfig");
                                        if (checkConfig) {
                                            HttpUtil.getMyCapability(true);
                                        }
                                        boolean uploadLog = jsonObject.optBoolean("uploadLog");
                                        if (uploadLog) {
//                                            FeedBackServcie.runFeedBackServcieService(CommonConfig.globalContext, new String[]{"????????????"}, false);
                                            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.FEED_BACK, new String[]{"????????????"},false);
                                        }
                                        boolean resetUnreadCount = jsonObject.optBoolean("resetUnreadCount");
                                        if (resetUnreadCount) {
                                            ConnectionUtil.getInstance().resetUnreadCount();
                                        }
                                    }
                                } catch (JSONException e) {

                                }
                                break;

                            case ProtoMessageOuterClass.CategoryType.CategoryWorkWorldNotice_VALUE:
                                try {
                                    //?????????????????????


                                     WorkWorldNoticeItem item = JsonUtils.getGson().fromJson(presenceMessage.getBody().getValue(), WorkWorldNoticeItem.class);
                                    if (item.getEventType().equals(Constants.WorkWorldState.NOTICE)) {
                                        List<WorkWorldNoticeItem> array = new ArrayList<>();
                                        array.add(item);
                                        IMDatabaseManager.getInstance().InsertWorkWorldNoticeByList(array, false);
                                        if (!IMDatabaseManager.getInstance().SelectWorkWorldPremissions()) {
                                            return;
                                        }
                                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE, item);
                                    } else if (item.getEventType().equals(Constants.WorkWorldState.WORKWORLDATMESSAGE)) {
                                        List<WorkWorldNoticeItem> array = new ArrayList<>();
                                        array.add(item);
                                        IMDatabaseManager.getInstance().InsertWorkWorldNoticeByList(array, false);
                                        if (!IMDatabaseManager.getInstance().SelectWorkWorldPremissions()) {
                                            return;
                                        }
                                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE, item);
                                    } else if(item.getEventType().equals(Constants.WorkWorldState.COMMENTATMESSAGE)){
                                        List<WorkWorldNoticeItem> array = new ArrayList<>();
                                        array.add(item);
                                        IMDatabaseManager.getInstance().InsertWorkWorldNoticeByList(array, false);
                                        if (!IMDatabaseManager.getInstance().SelectWorkWorldPremissions()) {
                                            return;
                                        }
                                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_NOTICE, item);
                                    }else if (item.getEventType().equals(Constants.WorkWorldState.WORKWORLD)) {

                                        final String navurl = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(QtalkNavicationService.NAV_CONFIG_CURRENT_URL, "");
                                       WorkWorldItem workWorldItem = IMDatabaseManager.getInstance().selectWorkWorldItemByUUID(item.getPostUUID());
                                       if(workWorldItem!=null){
                                           return;
                                       }
                                        IMUserDefaults.getStandardUserDefaults().newEditor(CommonConfig.globalContext)
                                                .putObject(CurrentPreference.getInstance().getUserid()
                                                        + QtalkNavicationService.getInstance().getXmppdomain()
                                                        + CommonConfig.isDebug
                                                        + MD5.hex(navurl)
                                                        + "WORKWORLDSHOWUNREAD", true)
                                                .synchronize();
                                        //todo ?????????????????????  ??????
                                        if (!IMDatabaseManager.getInstance().SelectWorkWorldPremissions()) {
                                            return;
                                        }
                                        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.WORK_WORLD_FIND_NOTICE, item);


                                    }

                                } catch (Exception e) {

                                }
                                break;
                            case ProtoMessageOuterClass.CategoryType.CategoryHotLineSync_VALUE:
                                LoginComplateManager.get_virtual_user_role();
                                break;

                            case ProtoMessageOuterClass.CategoryType.CategoryMedalListSync_VALUE:
                              int oldMedalVersion =   IMDatabaseManager.getInstance().selectMedalListVersion();
                              int newMedalVersion = new JSONObject(presenceMessage.getBody().getValue()).getInt("medalVersion");
                              if(oldMedalVersion<newMedalVersion){
                                  LoginComplateManager.updateMedalList();
                              }

                                break;

                            case ProtoMessageOuterClass.CategoryType.CategoryMedalUserStatusListSync_VALUE:

                                    int oldMedalUserVersion = IMDatabaseManager.getInstance().selectUserMedalStatusVersion();
                                int newMedalUserVersion = new JSONObject(presenceMessage.getBody().getValue()).getInt("userMedalVersion");
                                if(oldMedalUserVersion<newMedalUserVersion){
                                    LoginComplateManager.updateMedalList();
                                }
                        }
                    }
                    if (presenceMessage.hasDefinedKey()) {
                        switch (presenceMessage.getValue()) {
                            //????????????
                            case QtalkEvent.delete_friend:
                                Nick delete = PbParseUtil.parseRemoveFriend(message);
                                if (delete != null) {
                                    IMDatabaseManager.getInstance().deleteFriend(delete);
                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.USER_GET_FRIEND);
                                }

                                break;
                            //????????????
                            case QtalkEvent.Verify_Friend:
                                Nick add = PbParseUtil.parseAddFriend(message);
                                if (add != null) {
                                    IMDatabaseManager.getInstance().addFriend(add);
                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.USER_GET_FRIEND);
                                }
//                                EventBus.getDefault().post(new EventBusEvent.FriendsChange(true));
                                break;
                            //???????????????
                            case QtalkEvent.Update_Muc_Vcard:
                                Nick nick = PbParseUtil.parseMucCard(message);
                                List<Nick> list = new ArrayList<>();
                                list.add(nick);
                                IMDatabaseManager.getInstance().updateMucCard(list);
                                nickCache.put(nick.getGroupId(), nick);
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Update_Muc_Vcard, "success");
                                break;
                            //?????????????????????????????????
                            case QtalkEvent.Del_Muc_Register:
                                GroupMember dgm = PbParseUtil.parseDeleteMucMember(message);
                                IMDatabaseManager.getInstance().DeleteGroupMemberByGM(dgm);
                                //????????????Id???????????????,???????????????????????????,??????????????????????????????,?????????????????????????????????
                                if (dgm.getMemberId().equals(CurrentPreference.getInstance().getPreferenceUserId())) {
                                    IMGroup i = new IMGroup();
                                    i.setGroupId(dgm.getGroupId());
                                    IMDatabaseManager.getInstance().DeleteGroupAndSessionListByGM(i);
                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Remove_Session, dgm.getGroupId());
                                }
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Del_Muc_Register, dgm.getGroupId());
                                break;
                            //??????????????????
                            case QtalkEvent.Invite_User:
                                GroupMember igm = PbParseUtil.parseInviteMucMember(message);
                                IMDatabaseManager.getInstance().InsertGroupMemberByGM(igm);
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Invite_User, "success");
                                break;
                            //?????????????????????
                            case QtalkEvent.Destory_Muc:
                                IMGroup dimGroup = PbParseUtil.parseDeleteMuc(message);
                                String mucName = dimGroup.getName();
                                if(TextUtils.isEmpty(mucName)){
                                    JSONObject mucJson = IMDatabaseManager.getInstance().selectMucByGroupId(dimGroup.getGroupId());
                                    mucName = mucJson.optString("Name");
                                }
                                IMDatabaseManager.getInstance().DeleteGroupAndSessionListByGM(dimGroup);
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Remove_Session, dimGroup.getGroupId());
                                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Destory_Muc, dimGroup.getGroupId(), mucName);
                                break;
                            case QtalkEvent.USER_JOIN_MUC:
                                GroupMember member = PbParseUtil.parseGroupAffiliation(message);
                                if(member != null){
                                    IMDatabaseManager.getInstance().InsertGroupMemberByGM(member);
                                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Group_Member_Update, member.getGroupId());
                                }

                                break;

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            //136 ??????
            case ProtoMessageOuterClass.SignalType.SignalTypeEncryption_VALUE:
                //??????????????????????????? Im_message???
                IMMessage encryptMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");
                //??????????????????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.CHAT_MESSAGE_ENCRYPT, encryptMessage);
//                //??????????????????????????? Im_message???
//                IMDatabaseManager.getInstance().InsertChatMessage(message, "1", "0");
//                //??????????????????????????? IM_SessionList???
//                IMDatabaseManager.getInstance().InsertSessionList(message);
                break;
            //15
            case ProtoMessageOuterClass.SignalType.SignalTypeSubscription_VALUE:
                //??????????????????????????? IM_Public_Number_Message???
                IMMessage newSubscriptionMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");
                IMDatabaseManager.getInstance().InsertPublicNumberMessage(newSubscriptionMessage);

                //???????????? ???????????? ????????????  ??????  ?????????????????? ????????????
                if (newSubscriptionMessage.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeNotice_VALUE || newSubscriptionMessage.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeSystem_VALUE
                        || newSubscriptionMessage.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeGrabMenuVcard_VALUE || newSubscriptionMessage.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeGrabMenuResult_VALUE) {
                    IMDatabaseManager.getInstance().InsertChatMessage(newSubscriptionMessage, true);
                    //??????????????????????????? IM_SessionList???
                    IMDatabaseManager.getInstance().InsertIMSessionList(newSubscriptionMessage, false);
                }
                //??????????????????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.CHAT_MESSAGE_SUBSCRIPTION, newSubscriptionMessage);
                break;
            // 17 qtalk ??????????????????
            //17  qtalk ??????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeHeadline_VALUE:
                //??????????????????????????? Im_message???
                IMMessage headlineMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");
                headlineMessage.setConversationID(Constants.SYS.SYSTEM_MESSAGE);//?????????????????????????????????
                headlineMessage.setFromID(Constants.SYS.SYSTEM_MESSAGE);
                headlineMessage.setType(ConversitionType.MSG_TYPE_HEADLINE);
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text, headlineMessage);
                IMDatabaseManager.getInstance().InsertChatMessage(headlineMessage, true);
                //??????????????????????????? IM_SessionList???
                IMDatabaseManager.getInstance().InsertIMSessionList(headlineMessage, false);
                //??????????????????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text_After_DB, headlineMessage);
                break;
            //signltype 132 ??????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeConsult_VALUE:
                try {
                    IMMessage newConsultMessage = PbParseUtil.parseReceiveChatMessage(message, MessageStatus.REMOTE_STATUS_CHAT_DELIVERED + "", MessageStatus.LOCAL_STATUS_SUCCESS + "");
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text, newConsultMessage);
                    IMDatabaseManager.getInstance().InsertChatMessage(newConsultMessage, true);
                    //??????????????????????????? IM_SessionList???
                    IMDatabaseManager.getInstance().InsertIMSessionList(newConsultMessage, true);
                    //??????????????????
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text_After_DB, newConsultMessage);

                    if (!newConsultMessage.isCarbon()) {
                        JSONObject jb = new JSONObject();
                        jb.put("id", newConsultMessage.getMessageId());
                        JSONArray ja = new JSONArray();
                        ja.put(jb);
                        ProtoMessageOuterClass.ProtoMessage receive = PbAssemblyUtil.getBeenNewReadStateMessage(MessageStatus.STATUS_SINGLE_DELIVERED + "", ja, newConsultMessage.getRealfrom(), myself);
                        mandatorySendMessage(receive);

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


                break;
            //140 ????????????????????????
            case ProtoMessageOuterClass.SignalType.SignalTypeCollection_VALUE:
                //??????immessage??????
                IMMessage collectionMessage = PbParseUtil.parseReceiveCollectionMessage(message);
                //??????message??????collectionmessage???
                IMDatabaseManager.getInstance().InsertCollectionMessage(collectionMessage);
                //??????sessionlist???
                IMDatabaseManager.getInstance().InsertIMSessionList(collectionMessage, false);
                //??????collectionuser???
                IMDatabaseManager.getInstance().InsertCollectionUser(collectionMessage);
                //????????????
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Collection_Message_Text, collectionMessage);
                break;
            case ProtoMessageOuterClass.SignalType.SignalTypeWebRtc_VALUE://110
                IMMessage webrtcMsg = PbParseUtil.parseReceiveChatMessage(message, "0", "0");

                String ext = webrtcMsg.getExt();
                if (!TextUtils.isEmpty(ext)) {
                    WebRtcJson json = JsonUtils.getGson().fromJson(ext, WebRtcJson.class);
                    if(json != null && "create".equals(json.type) && !webrtcMsg.isCarbon()) {
                        if (webrtcMsg.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_VideoCall_VALUE) {
                            Intent i = new Intent("android.intent.action.VIEW",
                                    Uri.parse(CommonConfig.schema + "://qcrtc/webrtc?fromid="
                                            + CurrentPreference.getInstance().getPreferenceUserId()
                                            + "&toid=" + message.getFrom()
                                            + "&chattype" + ConversitionType.MSG_TYPE_CHAT
                                            + "&realjid" + message.getFrom()
                                            + "&isFromChatRoom" + false
                                            + "&offer=true&video=true&msgid=" + webrtcMsg.getId()));
                            i.putExtra("messge", webrtcMsg);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            CommonConfig.globalContext.startActivity(i);
                        } else if (webrtcMsg.getMsgType() == ProtoMessageOuterClass.MessageType.WebRTC_MsgType_AudioCall_VALUE) {
                            Intent i = new Intent("android.intent.action.VIEW",
                                    Uri.parse(CommonConfig.schema + "://qcrtc/webrtc?fromid="
                                            + CurrentPreference.getInstance().getPreferenceUserId()
                                            + "&toid=" + message.getFrom()
                                            + "&chattype" + ConversitionType.MSG_TYPE_CHAT
                                            + "&realjid" + message.getFrom()
                                            + "&isFromChatRoom" + false
                                            + "&offer=true&video=false&msgid=" + webrtcMsg.getId()));
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            CommonConfig.globalContext.startActivity(i);
                        }
                        return;
                    }
                }

                EventBus.getDefault().post(new EventBusEvent.WebRtcMessage(webrtcMsg));
                break;
            //?????????
            case ProtoMessageOuterClass.SignalType.SignalTypeError_VALUE:
                IMMessage errorMessage = PbParseUtil.parseErrorMessage(message);
                IMDatabaseManager.getInstance().UpdateChatStateMessage(errorMessage, true);
                if (!TextUtils.isEmpty(errorMessage.getId())) {
                    //todo ?????????????????????????????????????????????consult??????
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Send_State, errorMessage);
                    IMMessage newerrormessage = new IMMessage();
                    newerrormessage.setBody("????????????????????????????????????????????????????????????");
                    newerrormessage.setConversationID(errorMessage.getConversationID());
                    newerrormessage.setFromID(errorMessage.getFromID());
                    newerrormessage.setToID(errorMessage.getToID());
                    newerrormessage.setDirection(IMMessage.DIRECTION_MIDDLE);
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Text, newerrormessage);
                }
                break;
        }
    }

    @Override
    public void onSocketConnected() {
//        try {
//            IMNotificaitonCenter.getInstance().postNotificationName(QtalkEvent.LOGIN_EVENT, true, "login");
//        } catch (Exception e) {
//            Logger.e(e, "onChatMessageReceived failed {}", "login");
//        }
    }

    @Override
    public void onStreamDidAuthenticate() {
        Logger.i("??????_pbSocket LoginStatus???true");
        _pbSocket.setLoginStatus(true);
        _pbSocket.setConnecting(false);
//        long lastMsgTime = IMDatabaseManager.getInstance().getLastestMessageTime();
//        List tempList = IMDatabaseManager.getInstance().getGroupListMsgMaxTime();
//        UserPresenceManager.getInstance().update();

        //???????????????????????????
//        Logger.i("??????????????????UI:?????????");
//        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_EVENT, LoginStatus.Login);
        //?????????????????? ?????????????????????
        //// TODO: 2017/9/7 ??????????????????????????????
        Logger.i("??????????????????,???????????????");
        getCurrentNetDBM(CommonConfig.globalContext, true);
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                LoginComplateManager.loginComplate();
            }
        });
    }

    public boolean isLoginStatus() {
        return _pbSocket.isLoginStatus();
    }

    public boolean isConnected() {
        return _pbSocket.isConnected();
    }

    public void setLoginStatus(boolean b) {
        _pbSocket.setLoginStatus(b);
    }

    public XMPPJID getMyself() throws NullPointerException {

        if (_pbSocket.getMyJID() == null) {
            String str = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext, Constants.Preferences.lastMySelf);
            Logger.i("??????myself???null?????????,????????????,?????????:" + str);
            _pbSocket.setMyJID(JsonUtils.getGson().fromJson(str, XMPPJID.class));

        }
        return _pbSocket.getMyJID();
    }

    public void logout(String userName) {
        Logger.i("??????");
        _pbSocket.shutdown();

    }

    private static Timer timer = new Timer();
    private HeartBeatTimerTask heartBeatTimerTask = null;

    public void startHeartBeat(long period) {
        if (heartBeatTimerTask != null) {
            heartBeatTimerTask.cancel();
        }
        timer.purge();
        heartBeatTimerTask = new HeartBeatTimerTask();
        timer.schedule(heartBeatTimerTask, 3 * 1000, period);

    }

    public class HeartBeatTimerTask extends TimerTask {

        @Override
        public void run() {
            if (_pbSocket == null || !_pbSocket.isLoginStatus()) {
                cancel();
//                timer.purge();
//                pingTimer.cancel();
                return;
            }

            try {
                Logger.i("???????????????:");
                sendHeartMessage(PbAssemblyUtil.getHeartBeatMessage());
//                sendMessage(PbAssemblyUtil.getHeartBeatMessage());
            } catch (Exception e) {
                Logger.i("??????????????????:" + e);
                cancel();
                timer.purge();
//                pingTimer.cancel();
            }
        }
    }

    private Handler connectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Logger.i("???????????????");
            if (_pbSocket != null)
                _pbSocket.shutdown(false);
            QtalkSDK.getInstance().login(false);
        }
    };

    private static final int WHAT = 0xff;//handler message??????

    public void wakeup() {
        if (_pbSocket != null) {
            _pbSocket.selectorWakup();
        }
    }

    public void setForceConnect() {
        if (_pbSocket != null) {
            _pbSocket.setForceConnect();
        }
    }

    public boolean isForceConnect() {
        if (_pbSocket != null) {
            return _pbSocket.isForceConnect();
        }
        return false;
    }

    public void sendHeartMessage(final ProtoMessageOuterClass.ProtoMessage protoMessage) {
        DispatchHelper.Async("sendHeart", false, new Runnable() {
            @Override
            public void run() {
                connectedHandler.sendEmptyMessageDelayed(WHAT, 40 * 1000);
                sendMessage(protoMessage);
            }
        });
    }


    static long lastTime = 0;

    /**
     * ?????????????????????????????????????????????
     * ??????LTE?????????3G/2G????????????????????????????????????????????????
     * LTE????????????????????????????????????????????????
     * 3G/2G???????????????????????????API????????????????????????
     * asu ??? dbm ???????????????????????? dbm=-113 + 2*asu
     */
    public void getCurrentNetDBM(final Context context, final boolean b) {
        final boolean[] start = {b};
        final TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener mylistener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                try {
                    int lte_rsrp = (Integer) signalStrength.getClass().getMethod("getLteRsrp").invoke(signalStrength);
//                    Logger.i("???????????????dBm???:" + lte_rsrp);
                    long time = 0;
                    if (0 > lte_rsrp && lte_rsrp >= -50) {
//                        Logger.i("???????????????dBm???:????????????" + lte_rsrp);
                        //???????????????????????????????????????
                        time = 60 * 1000;
                    } else if (-50 > lte_rsrp && lte_rsrp >= -90) {
                        //??????????????????????????????????????????
//                        Logger.i("???????????????dBm???:???????????????" + lte_rsrp);
                        time = 30 * 1000;
                    } else {
                        //???????????????????????????????????????,????????????
//                        Logger.i("???????????????dBm???:???????????????" + lte_rsrp);
                        time = 15 * 1000;

                    }
                    //???????????????????????????,????????????????????????
                    if (time != lastTime || start[0]) {
                        Logger.i("?????????????????????;??????:" + time);
                        //// TODO: 2017/11/21 ????????????,?????????????????????????????? ??????????????? ????????????
                        startHeartBeat(time);
                        lastTime = time;
                        start[0] = false;
                    }
                } catch (Exception e) {
                    startHeartBeat(15 * 1000);
                    e.printStackTrace();
                    return;
                }

            }
        };
        //????????????
        tm.listen(mylistener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

}