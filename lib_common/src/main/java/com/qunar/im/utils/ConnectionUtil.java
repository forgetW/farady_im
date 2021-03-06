package com.qunar.im.utils;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;

import com.orhanobut.logger.Logger;
import com.qunar.im.base.common.ConversitionType;
import com.qunar.im.base.common.QChatRSA;
import com.qunar.im.base.jsonbean.AtInfo;
import com.qunar.im.base.jsonbean.GetDepartmentResult;
import com.qunar.im.base.jsonbean.JSONChatHistorys;
import com.qunar.im.base.jsonbean.JSONMucHistorys;
import com.qunar.im.base.jsonbean.NewRemoteConfig;
import com.qunar.im.base.jsonbean.NotificationConfig;
import com.qunar.im.base.jsonbean.QuickReplyResult;
import com.qunar.im.base.jsonbean.RNSearchData;
import com.qunar.im.base.jsonbean.RemoteConfig;
import com.qunar.im.base.module.CollectionConversation;
import com.qunar.im.base.module.DepartmentItem;
import com.qunar.im.base.module.GroupMember;
import com.qunar.im.base.module.IMGroup;
import com.qunar.im.base.module.IMMessage;
import com.qunar.im.base.module.MedalListResponse;
import com.qunar.im.base.module.Nick;
import com.qunar.im.base.module.QuickReplyData;
import com.qunar.im.base.module.RecentConversation;
import com.qunar.im.base.module.UserConfigData;
import com.qunar.im.base.module.UserHaveMedalStatus;
import com.qunar.im.base.module.WorkWorldItem;
import com.qunar.im.base.module.WorkWorldNewCommentBean;
import com.qunar.im.base.module.WorkWorldNoticeItem;
import com.qunar.im.base.module.WorkWorldSingleResponse;
import com.qunar.im.base.protocol.LoginAPI;
import com.qunar.im.base.protocol.Protocol;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.base.shortutbadger.ShortcutBadger;
import com.qunar.im.base.structs.MessageStatus;
import com.qunar.im.base.structs.MessageType;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.core.manager.IMDatabaseManager;
import com.qunar.im.core.manager.IMLogicManager;
import com.qunar.im.core.manager.IMNotificaitonCenter;
import com.qunar.im.core.manager.IMPayManager;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.other.CacheDataType;
import com.qunar.im.other.IQTalkLoginDelegate;
import com.qunar.im.other.QtalkSDK;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.protobuf.common.LoginType;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.dispatch.DispatchHelper;
import com.qunar.im.protobuf.entity.XMPPJID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by hubin on 2017/8/8.
 * pb???????????????
 */


public class ConnectionUtil {
    public static ConnectionUtil instance = null;
    public QtalkSDK qtalkSDK;


    public static String defaultUserImage = QtalkNavicationService.getInstance().getInnerFiltHttpHost() + "/file/v2/download/perm/3ca05f2d92f6c0034ac9aee14d341fc7.png";


    private ConnectionUtil() {
        qtalkSDK = QtalkSDK.getInstance();
    }

    /**
     * ????????????,?????????????????????Applactioncontext ??????????????????
     *
     * @return
     */

    public static ConnectionUtil getInstance() {
        if (instance == null) {
            synchronized (ConnectionUtil.class) {
                if (instance == null) {
                    instance = new ConnectionUtil();
                }
            }
        }
        return instance;
    }

    public static void setInitialized(boolean initialized) {
        IMDatabaseManager.getInstance().setInitialized(initialized);
    }

    //???????????????????????????
    public void takeSmsCode(final String userName, final IQTalkLoginDelegate delegate) {
        qtalkSDK.takeSmsCode(userName, delegate);
    }

    //????????????????????????
    public void clearSmsCode() {
        qtalkSDK.clearSmscode();
    }

    public String getMarkupNameById(String xmppid){
        String name =  CurrentPreference.getInstance().getMarkupNames().get(xmppid);
        if(TextUtils.isEmpty(name)){
            name = selectMarkupNameById(xmppid);
        }
        return name;
    }
    /**
     * ???????????????
     *
     * @return
     * @param isForce
     */
    public void initNavConfig(boolean isForce) {
        qtalkSDK.initNavConfig(isForce);
    }

    //??????????????????????????????????????????
    public LoginType getLoginType() {
        return QtalkNavicationService.getInstance().getLoginType();
    }

    //?????????event
    public void addEvent(IMNotificaitonCenter.NotificationCenterDelegate delegate, String key) {
        qtalkSDK.addEvent(delegate, key);
    }

    //??????event
    public void removeEvent(IMNotificaitonCenter.NotificationCenterDelegate delegate, String key) {
        qtalkSDK.removeEvent(delegate, key);
    }

    //??????event
    public void sendEvent(String key,Object... objects){
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.PAY_SUCCESS, objects);
    }

    /**
     * ?????????????????????
     * @param jid
     * @return
     */
    public boolean isHotline(String jid) {
        List<String> hotlines = CurrentPreference.getInstance().getHotLineList();
        if(hotlines == null){
            hotlines = IMDatabaseManager.getInstance().searchHotlines();
            if(hotlines == null) {
                return false;
            }else {
                CurrentPreference.getInstance().setHotLineList(hotlines);
            }
        }

        return hotlines.contains(jid);
    }

    /**
     * ????????????????????????
     * @return
     */
    public boolean isHotlineMerchant(String hotlinejid) {
        List<String> myholines = CurrentPreference.getInstance().getMyHotlines();
        if(myholines != null && myholines.contains(hotlinejid)) {
            return true;
        }
        return false;
    }

    public void reConnection() {
        IMLogicManager.getInstance().reConnection();
    }

    public void reConnectionForce(){
        IMLogicManager.getInstance().reConnectionForce();
    }

    public void shutdown() {
        IMLogicManager.getInstance().shutdown();
    }

//    //??????????????????
//    public void setLoginStatus(boolean b) {
//        QtalkSDK.getInstance().setLoginStatus(b);
//    }

    //pb??????????????????
    public void pbLogin(String username, String password, boolean isPublic) {
        //?????????????????????
        pbLogout();
        CurrentPreference.getInstance().setUserid(username);
        initNavConfig(true);
        setInitialized(false);
        if (isPublic) {
            qtalkSDK.publicLogin(username, password);
        } else {
            qtalkSDK.login(username, password);
        }
    }

    /**
     * ????????? ??????????????????token
     * @param username
     * @param password
     */
    public void pbLoginNew(final String username, String password){
        //?????????????????????
        pbLogout();
        CurrentPreference.getInstance().setUserid(username);
        initNavConfig(true);
        setInitialized(false);
        try {
            password = QChatRSA.QTalkEncodePassword(password);
        } catch (Exception e) {
            Logger.e("QChatRSAError:" + e.getLocalizedMessage());
        }
        LoginAPI.getNewLoginToken(username, password, new ProtocolCallback.UnitCallback<String[]>() {
            @Override
            public void onCompleted(String[] ss) {
                if(ss != null && ss.length > 1){
                    qtalkSDK.newLogin(ss[0],ss[1]);
                }else {
                    IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_FAILED, -99);
                }
            }

            @Override
            public void onFailure(String errMsg) {
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.LOGIN_FAILED, -99);
            }
        });
    }

    /**
     * ?????????????????????
     * @param xmppId
     */
    public void deleteIMmessageByXmppId(String xmppId){
        IMDatabaseManager.getInstance().deleteIMmessageByXmppId(xmppId);
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.CLEAR_MESSAGE,xmppId);
    }

    /**
     * pb ????????????
     */
    public void pbLogout() {
        qtalkSDK.logout("");
    }

    public void pbAutoLogin() {
        qtalkSDK.login(false);
    }

    //????????????????????????
    public boolean isConnected() {
        return qtalkSDK.isConnected();
    }

    //????????????????????????
    public boolean isLoginStatus() {
        return qtalkSDK.isLoginStatus();
    }

    //??????????????????????????????????????????
    public boolean isCanAutoLogin() {
        return qtalkSDK.needTalkSmscode();
    }


    //--------------------------------------------xmpp??????-------------------------------------------

    /**
     * ??????????????? ??????????????????????????? ?????????
     * @param imMessage
     */
//    public void sendSpecialNoticeMessage(String target,String key,String value){
//        String from = CurrentPreference.getInstance().getPreferenceUserId();
//        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getSpecialNoticeMessage(from,from,key,value);
//        qtalkSDK.sendMessage(protoMessage);
//    }

    //??????????????????????????????
    public void sendTypingStatus(IMMessage imMessage) {
        //???????????????protoMessage
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getTypingStatusMessage(imMessage);
        //??????????????????????????????
        qtalkSDK.sendMessage(protoMessage);

    }

    //??????????????????
    public void sendTextOrEmojiMessage(final IMMessage imMessage) {
        DispatchHelper.Async("sendMessage",false, new Runnable() {
            @Override
            public void run() {
                //??????????????????protoMessage
                ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getTextOrEmojiMessage(imMessage);
                //????????????????????????????????? im_message??????
                IMDatabaseManager.getInstance().InsertChatMessage(imMessage, false);
                //IMDatabaseManager.getInstance().InsertChatMessage(protoMessage, "2", "1");
                //??????????????????,??????im_sessionList??????
                IMDatabaseManager.getInstance().InsertIMSessionList(imMessage, false);
                //IMDatabaseManager.getInstance().InsertSessionList(protoMessage);
                qtalkSDK.sendMessageSync(protoMessage);
            }
        });
    }

    public void sendTransMessage(IMMessage imMessage) {
        //??????????????????protoMessage
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getTransMessage(imMessage);
        qtalkSDK.sendMessage(protoMessage);
    }

    public void sendErrorMessage(String userid,String str){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getTextErrorMessage(userid,str);

        qtalkSDK.sendMessage(protoMessage);
    }


    //????????????????????????
    public void sendEncryptSignalMessage(IMMessage imMessage) {
        //??????????????????protoMessage
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getEncryptSignalMessage(imMessage);
//        //????????????????????????????????? im_message??????
//        IMDatabaseManager.getInstance().InsertChatMessage(protoMessage, "2", "1");
//        //??????????????????,??????im_sessionList??????
//        IMDatabaseManager.getInstance().InsertSessionList(protoMessage);
        qtalkSDK.sendMessage(protoMessage);
    }

    //???????????????
    public void sendSubscriptionMessage(IMMessage imMessage) {
        //??????????????????protoMessage
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getSubscriptionMessage(imMessage);
        //????????????????????????????????? IM_Public_Number_Message??????
        IMDatabaseManager.getInstance().InsertPublicNumberMessage(imMessage);
        //??????????????????,??????im_sessionList??????
//        IMDatabaseManager.getInstance().InsertIMSessionList(imMessage);
        qtalkSDK.sendMessage(protoMessage);
    }
    //webrtc
    public void sendWebrtcMessage(IMMessage imMessage){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getWebrtcMessage(imMessage);
        qtalkSDK.sendMessage(protoMessage);
    }

    //??????????????????
    public void sendRevokeMessage(IMMessage imMessage) {
        //??????pb
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getRevokeMessage(imMessage);
        //?????????????????????
        IMDatabaseManager.getInstance().UpdateRevokeChatMessage(imMessage.getId(), imMessage.getNick().getName() + "?????????????????????", imMessage.getMsgType());
        //????????????
        qtalkSDK.sendMessage(protoMessage);
    }


    //??????????????????
    public void sendGroupTextOrEmojiMessage(IMMessage imMessage) {
        if (TextUtils.isEmpty(imMessage.getFromID())) {
            imMessage.setFromID(CurrentPreference.getInstance().getPreferenceUserId());
        }
        //??????????????????protoMessage
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getGroupTextOrEmojiMessage(imMessage);
        //????????????????????????????????? im_message??????
//        IMDatabaseManager.getInstance().InsertSendGroupChatMessage(protoMessage, "2", "1");
        IMDatabaseManager.getInstance().InsertChatMessage(imMessage, false);
        //??????????????????,??????im_sessionList??????
//        IMDatabaseManager.getInstance().InsertIMSessionList(imMessage);
        IMDatabaseManager.getInstance().InsertSessionList(protoMessage);
        qtalkSDK.sendMessage(protoMessage);
        //TODO ????????????????????????????????????????????????????????????????????? ????????????
//        sendGroupAllRead(imMessage.getToID());
    }

    public Nick getMyselfCard(String jid) {
        XMPPJID target = XMPPJID.parseJID(jid);
        return JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectUserByJID(target.fullname()).toString(), Nick.class);
    }

    //??????????????????
    public void getUserCard(String jid, IMLogicManager.NickCallBack nickCallBack, boolean enforce, boolean toDB) {
        XMPPJID target = XMPPJID.parseJID(jid);
        IMLogicManager.getInstance().getUserInfoByUserId(target, IMLogicManager.getInstance().getMyself(), enforce, toDB, nickCallBack);
    }

    public List<UserHaveMedalStatus>  getUserMedalList(String xmppId){
        return IMLogicManager.getInstance().getUserMedalList(xmppId);
    }

    //??????????????????
    public void getUserCard(String jid, IMLogicManager.NickCallBack nickCallBack) {
        XMPPJID target = XMPPJID.parseJID(jid);
        IMLogicManager.getInstance().getUserInfoByUserId(target, nickCallBack);
    }

    public Nick testgetnick(String jid){
        return IMLogicManager.getInstance().getNickkk(jid);
    }

    public LruCache<String,String> selectMarkupNames(){
           return    IMDatabaseManager.getInstance().selectMarkupNames();
    }

    public String selectMarkupNameById(String xmppid){
        return  IMDatabaseManager.getInstance().selectMarkupNameById(xmppid);
    }

    public void getCollectionUserCard(final String jid, final IMLogicManager.NickCallBack nickCallBack, final boolean enforce, final boolean toDB) {
//        DispatchHelper.Async("getCollectionuserAsync", true, new Runnable() {
//            @Override
//            public void run() {
        XMPPJID target = XMPPJID.parseJID(jid);
        IMLogicManager.getInstance().getCollectionUserInfoByUserId(target, enforce, toDB, nickCallBack);
//            }
//        });

    }

    public void getCollectionMucCard(final String jid, final IMLogicManager.NickCallBack nickCallBack, final boolean enforce, final boolean toDB) {
//        DispatchHelper.Async("getCollectionmucAsync", true, new Runnable() {
//            @Override
//            public void run() {
        XMPPJID target = XMPPJID.parseJID(jid);
        IMLogicManager.getInstance().getCollectionMucInfoByGroupId(target, enforce, toDB, nickCallBack);
//            }
//        });
    }


    //??????????????????(??????id????????? ??????)
    public Nick getNickById(String jid) {
        XMPPJID target = XMPPJID.parseJID(jid);
        return IMLogicManager.getInstance().getNickById(target);
    }

    public void updateUserImage(String jid,String url){
        IMDatabaseManager.getInstance().updateUserImage(jid,url);
    }


    //??????????????????(??????id?????? ??????)
    public Nick getMucNickById(String id) {
        return JsonUtils.getGson().fromJson(IMDatabaseManager.getInstance().selectMucByGroupId(id).toString(), Nick.class);
    }

    public boolean checkGroupByJid(String jid){
        return IMDatabaseManager.getInstance().checkGroupByJid(jid);
    }


    //?????????????????????GroupId
    public List<GroupMember> SelectGroupMemberByGroupId(String groupId) {
        return IMDatabaseManager.getInstance().SelectGroupMemberByGroupId(groupId);
    }

    //?????????????????????????????????????????????
    public int selectGroupMemberPermissionsByGroupIdAndMemberId(String groupId,String memberId){
        return IMDatabaseManager.getInstance().selectGroupMemberPermissionsByGroupIdAndMemberId(groupId,memberId);
    }

    /**
     * ?????????????????????
     */
    public void DeleteSessionList(){
        IMDatabaseManager.getInstance().DeleteSessionList();
    }

    /**
     * ??????????????????????????????
     * @return
     */
    public List<GetDepartmentResult.UserItem> getAllOrgaUsers(){
        return IMDatabaseManager.getInstance().getAllOrgaUsers();
    }


    //???????????????
    public void getMucCard(String jid, IMLogicManager.NickCallBack nickCallBack, boolean enforce, boolean toDB) {
        XMPPJID target = XMPPJID.parseJID(jid);
        IMLogicManager.getInstance().getMucInfoByGroupId(target, IMLogicManager.getInstance().getMyself(), enforce, toDB, nickCallBack);
    }

    public List<Nick> SelectFriendList() {
        return IMDatabaseManager.getInstance().SelectFriendList();
    }

    public List<Nick> SelectFriendListForRN(){
        return IMDatabaseManager.getInstance().SelectFriendListForRN();
    }

    public boolean isMyFriend(String XmppId){
        return IMDatabaseManager.getInstance().isFriend(XmppId);
    }

    public List<IMMessage> searchVoiceMsg(String convid, long t, int msgType) {
        return IMDatabaseManager.getInstance().searchVoiceMsg(convid, t, msgType);
    }

    public List<IMMessage> searchEncryptMsg(String convid, long t) {
        return IMDatabaseManager.getInstance().searchMessageByMsgType(convid, t, ProtoMessageOuterClass.MessageType.MessageTypeEncrypt_VALUE);
    }

    public Nick getUserCardByName(String name) {
        return IMDatabaseManager.getInstance().selectUserByName(name);
    }


    public List<IMMessage> SelectHistoryCollectionChatMessage(String of, String ot, String chatType, int count, int size) {
        List<IMMessage> list = SelectInitReloadCollectionChatMessage(of, ot, chatType, count, size);
        if (list != null && list.size() > 0) {
            return list;
        } else {
            List<IMMessage> noMessage = createNoMessage();
            return noMessage;
        }

    }

    /**
     * ???????????????????????? ?????????????????? ???????????????,?????????????????????  ????????????
     *
     * @param xmppid
     * @param count
     * @param size
     * @param historyMessage
     */
    public void SelectHistoryChatMessage(String chattype, String xmppid, String realJid, int count, int size, final HistoryMessage historyMessage) {
        List<IMMessage> messageList;
        long time;
        if ((ConversitionType.MSG_TYPE_CONSULT_SERVER + "").equals(chattype)) {
            messageList = IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, realJid, count, size);
            time = IMDatabaseManager.getInstance().getFirstMessageTimeByXmppIdAndRealJid(xmppid, realJid);
        } else if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chattype)) {
            messageList = IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, xmppid, count, size);
            time = IMDatabaseManager.getInstance().getFirstMessageTimeByXmppIdAndRealJid(xmppid, xmppid);
        } else {
            messageList = IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, realJid, count, size);
            time = IMDatabaseManager.getInstance().getFirstMessageTimeByXmppIdAndRealJid(xmppid, realJid);
        }


//        List<IMMessage> messageList = IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid,realJid, count, size);
        if (messageList.size() > 0) {
            historyMessage.onMessageResult(messageList);
        } else {
            if ((ConversitionType.MSG_TYPE_CONSULT_SERVER + "").equals(chattype)) {
                HttpUtil.getJsonConsultChatOfflineMsg(chattype, xmppid, realJid, time, count, size, 0, true, false, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                    @Override
                    public void onCompleted(List<IMMessage> messageList) {
                        if (messageList != null) {
                            historyMessage.onMessageResult(messageList);
                        } else {
                            List<IMMessage> noMessage = createNoMessage();
                            historyMessage.onMessageResult(noMessage);
                        }
                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
            } else if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chattype)) {
                HttpUtil.getJsonSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, true, 0, false, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                    @Override
                    public void onCompleted(List<IMMessage> messageList) {
                        if (messageList != null) {
                            historyMessage.onMessageResult(messageList);
                        } else {
                            List<IMMessage> noMessage = createNoMessage();
                            historyMessage.onMessageResult(noMessage);
                        }

                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
            } else {
                HttpUtil.getJsonSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, true, 0, false, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                    @Override
                    public void onCompleted(List<IMMessage> messageList) {
                        if (messageList != null) {
                            historyMessage.onMessageResult(messageList);
                        } else {
                            List<IMMessage> noMessage = createNoMessage();
                            historyMessage.onMessageResult(noMessage);
                        }

                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
//                HttpUtil.getSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, 0, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
//                    @Override
//                    public void onCompleted(List<IMMessage> messageList) {
//                        if (messageList != null) {
//                            historyMessage.onMessageResult(messageList);
//                        } else {
//                            List<IMMessage> noMessage = new ArrayList<IMMessage>();
//                            IMMessage imMessage = new IMMessage();
//                            String uid = UUID.randomUUID().toString();
//                            imMessage.setId(uid);
//                            imMessage.setMessageID(uid);
//                            imMessage.setDirection(2);
//                            imMessage.setBody("?????????????????????");
//                            noMessage.add(imMessage);
//                            historyMessage.onMessageResult(noMessage);
//                        }
//
//                    }
//
//                    @Override
//                    public void onFailure() {
//
//                    }
//                });
            }

        }
//        return  messageList;
    }

    /**
     * ????????????????????????,??????????????????,?????????????????????,????????????
     *
     * @param xmppid
     * @param count
     * @param size
     * @param historyMessage
     * @return
     */
    public void SelectHistoryGroupChatMessage(String xmppid, String realJid, int count, int size, final HistoryMessage historyMessage) {
        List<IMMessage> list = IMDatabaseManager.getInstance().SelectHistoryGroupChatMessage(xmppid, realJid, count, size);
        if (list.size() > 0) {
            historyMessage.onMessageResult(list);
        } else {
            long time = IMDatabaseManager.getInstance().getFirstMessageTimeByXmppIdAndRealJid(xmppid, realJid);

            HttpUtil.getJsonMultiChatOffLineMsg(xmppid, realJid, time, count, size, 0, true, false, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                @Override
                public void onCompleted(List<IMMessage> messageList) {
                    if (messageList != null) {
                        historyMessage.onMessageResult(messageList);
                    } else {
                        List<IMMessage> noMessage = createNoMessage();
                        historyMessage.onMessageResult(noMessage);
                    }
                }

                @Override
                public void onFailure(String errMsg) {

                }
            });
        }

    }

    /**
     * ??????????????????????????????????????????
     * @param xmppid
     * @param realJid
     * @param count
     * @param size
     * @param time
     * @param direction
     * @param isInclude
     * @param historyMessage
     */
    public void SelectHistoryChatMessageForNet(String xmppid, String realJid, int count, int size, long time,int direction,boolean isInclude,String chattype,final HistoryMessage historyMessage){
        if ((ConversitionType.MSG_TYPE_CONSULT_SERVER + "").equals(chattype)) {
            HttpUtil.getJsonConsultChatOfflineMsg(chattype, xmppid, realJid, time, count, size, direction, false, isInclude, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                @Override
                public void onCompleted(List<IMMessage> messageList) {
                    if (messageList != null) {
                        historyMessage.onMessageResult(messageList);
                    } else {
                        List<IMMessage> noMessage = createNoMessage();
                        historyMessage.onMessageResult(noMessage);
                    }
                }

                @Override
                public void onFailure(String errMsg) {

                }
            });
        } else if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chattype)) {
            HttpUtil.getJsonSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, false, direction, isInclude, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                @Override
                public void onCompleted(List<IMMessage> messageList) {
                    if (messageList != null) {
                        historyMessage.onMessageResult(messageList);
                    } else {
                        List<IMMessage> noMessage = createNoMessage();
                        historyMessage.onMessageResult(noMessage);
                    }

                }

                @Override
                public void onFailure(String errMsg) {

                }
            });
        } else {
            HttpUtil.getJsonSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, false, direction, isInclude, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
                @Override
                public void onCompleted(List<IMMessage> messageList) {
                    if (messageList != null) {
                        historyMessage.onMessageResult(messageList);
                    } else {
                        List<IMMessage> noMessage = createNoMessage();
                        historyMessage.onMessageResult(noMessage);
                    }

                }

                @Override
                public void onFailure(String errMsg) {

                }
            });
//                HttpUtil.getSingleChatOfflineMsg(chattype, xmppid, realJid, time, count, size, 0, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
//                    @Override
//                    public void onCompleted(List<IMMessage> messageList) {
//                        if (messageList != null) {
//                            historyMessage.onMessageResult(messageList);
//                        } else {
//                            List<IMMessage> noMessage = new ArrayList<IMMessage>();
//                            IMMessage imMessage = new IMMessage();
//                            String uid = UUID.randomUUID().toString();
//                            imMessage.setId(uid);
//                            imMessage.setMessageID(uid);
//                            imMessage.setDirection(2);
//                            imMessage.setBody("?????????????????????");
//                            noMessage.add(imMessage);
//                            historyMessage.onMessageResult(noMessage);
//                        }
//
//                    }
//
//                    @Override
//                    public void onFailure() {
//
//                    }
//                });
        }
    }

    /**
     * ??????????????????????????????????????????
     * @param xmppid
     * @param realJid
     * @param count
     * @param size
     * @param time
     * @param direction
     * @param isInclude
     * @param historyMessage
     */
    public void SelectHistoryGroupChatMessageForNet(String xmppid, String realJid, int count, int size, long time,int direction,boolean isInclude,final HistoryMessage historyMessage){
        HttpUtil.getJsonMultiChatOffLineMsg(xmppid, realJid, time, count, size, direction, false, isInclude, new ProtocolCallback.UnitCallback<List<IMMessage>>() {
            @Override
            public void onCompleted(List<IMMessage> messageList) {
                if (messageList != null) {
                    historyMessage.onMessageResult(messageList);
                } else {
                    List<IMMessage> noMessage = createNoMessage();
                    historyMessage.onMessageResult(noMessage);
                }
            }

            @Override
            public void onFailure(String errMsg) {

            }
        });
    }


    /**
     * ????????????????????????,??????json
     *
     * @param list
     * @param selfId
     */
    public   List<IMMessage> ParseHistoryChatData(List<JSONChatHistorys.DataBean> list, String selfId) {
          List<IMMessage> messageList = new ArrayList<>();
        try {

            int size = list.size();
            for (int i = 0; i < size; ++i) {
                IMMessage imMessage = new IMMessage();
                try {

                    JSONChatHistorys.DataBean data = list.get(i);
                    JSONChatHistorys.DataBean.BodyBean body = data.getBody();
                    JSONChatHistorys.DataBean.MessageBean message = data.getMessage();
                    JSONChatHistorys.DataBean.TimeBean time = data.getTime();
                    if (data == null || body == null || message == null || time == null) {
                        continue;
                    }
                    String from = TextUtils.isEmpty(QtalkStringUtils.parseIdAndDomain(message.getFrom())) ? data.getFrom() + "@" + data.getFrom_host() : QtalkStringUtils.parseIdAndDomain(message.getFrom());
                    String to = TextUtils.isEmpty(QtalkStringUtils.parseIdAndDomain(message.getTo())) ? data.getTo() + "@" + data.getTo_host() : QtalkStringUtils.parseIdAndDomain(message.getTo());
                    String ofrom = QtalkStringUtils.parseIdAndDomain(message.getOriginfrom());
                    String oto = QtalkStringUtils.parseIdAndDomain(message.getOriginto());
                    String realFrom = TextUtils.isEmpty(QtalkStringUtils.parseIdAndDomain(message.getRealjid())) ? QtalkStringUtils.parseIdAndDomain(message.getRealfrom()) : QtalkStringUtils.parseIdAndDomain(message.getRealjid());
                    String realTo = QtalkStringUtils.parseIdAndDomain(message.getRealto());
                    //?????????????????????

                    String msgId = body.getId();

//                    isstat.bindString(3, from.equals(selfId) ? to : from);
                    imMessage.setFromID(from.equals(selfId) ? to : from);

//                    imstat.bindString(1, body.getId());
                    imMessage.setId(body.getId());
                    imMessage.setMessageID(body.getId());
//                    imstat.bindString(2, from.equals(selfId) ? to : from);//????????????me???xmppid
                    imMessage.setConversationID(from.equals(selfId) ? to : from);
//                    imstat.bindString(3, from);
                    imMessage.setFromID(from);
//                    imstat.bindString(4, to);
                    imMessage.setToID(to);
//                    imstat.bindString(5, body.getContent());
                    imMessage.setBody(body.getContent());
                    if (!TextUtils.isEmpty(message.getClient_type())) {
                        switch (message.getClient_type()) {
                            case "ClientTypeMac":
//                                imstat.bindString(6, 1 + "");
                                imMessage.setMaType(1+"");
                                break;
                            case "ClientTypeiOS":
//                                imstat.bindString(6, 2 + "");
                                imMessage.setMaType(2+"");
                                break;
                            case "ClientTypePC":
//                                imstat.bindString(6, 3 + "");
                                imMessage.setMaType(3+"");
                                break;
                            case "ClientTypeAndroid":
//                                imstat.bindString(6, 4 + "");
                                imMessage.setMaType(4+"");
                                break;
                            default:
//                                imstat.bindString(6, 0 + "");
                                imMessage.setMaType(0+"");
                                break;
                        }
                    } else if (!TextUtils.isEmpty(body.getMaType())) {
//                        imstat.bindString(6, body.getMaType());
                        imMessage.setMaType(body.getMaType());
                    } else {
//                        imstat.bindString(6, 0 + "");
                        imMessage.setMaType(0+"");
                    }
                    String msgType = body.getMsgType();
//                    imstat.bindString(7, msgType);
                    imMessage.setMsgType(Integer.parseInt(msgType));

//                    imstat.bindString(8, String.valueOf(MessageStatus.LOCAL_STATUS_SUCCESS));
                    imMessage.setMessageState(MessageStatus.LOCAL_STATUS_SUCCESS);

                    if ("-1".equals(body.getMsgType())
                            || "15".equals(body.getMsgType())
                            || (ProtoMessageOuterClass.MessageType.MessageTypeRobotTurnToUser_VALUE + "").equals(body.getMsgType())) {
//                        imstat.bindString(9, "2");
                        imMessage.setDirection(2);
                    } else {
//                        imstat.bindString(9, from.equals(selfId) ? "1" : "0");
                        imMessage.setDirection(from.equals(selfId) ? 1 : 0);
                    }
                    String t = "";
                    if (TextUtils.isEmpty(message.getMsec_times())) {
                        String d = time.getStamp();
                        String str = "yyyyMMdd'T'HH:mm:ss";
                        SimpleDateFormat sdf = new SimpleDateFormat(str);
                        TimeZone timeZone = TimeZone.getTimeZone("GMT");
                        sdf.setTimeZone(timeZone);
                        Date date = null;
                        try {
                            if (TextUtils.isEmpty(d)) {
                                date = new Date();
                            } else {
                                date = sdf.parse(d, new ParsePosition(0));
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        t = date.getTime() + "";
//                        stat.bindString(10, date.getTime() + "");
                    } else {
                        t = message.getMsec_times();
//                        stat.bindString(10, message.getMsec_times());
                    }
//                    imstat.bindString(10, t);
                    imMessage.setTime(new Date(Long.parseLong(t)));

//                    imstat.bindString(11, String.valueOf(data.getRead_flag()));
                    imMessage.setReadState(MessageStatus.REMOTE_STATUS_CHAT_READED);
//                    imstat.bindString(12, JsonUtils.getGson().toJson(data));
//                    imstat.bindString(13, from.equals(selfId) ? to : from);
                    imMessage.setRealfrom( from.equals(selfId) ? to : from);
                    //// TODO: 2017/12/6 ???????????????????????????
//                    imstat.bindString(14, TextUtils.isEmpty(body.getExtendInfo()) ? "" : body.getExtendInfo());
                    imMessage.setExt( TextUtils.isEmpty(body.getExtendInfo()) ? "" : body.getExtendInfo());

                    //?????????????????????
                    if ("collection".equals(message.getType())) {
                        //??????????????????????????????????????????
//                        isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_COLLECTION));
//                        imstat.bindString(9, "0");
                        imMessage.setDirection(0);
//                        imcstat.bindString(1, body.getId());
//                        imcstat.bindString(2, ofrom);
//                        imcstat.bindString(3, oto);
                        if ("chat".equals(message.getOrigintype())) {
//                            imcstat.bindString(4, 0 + "");
                        } else if ("groupchat".equals(message.getOrigintype())) {
                            //???????????????????????? message???from???realJid
//                            imstat.bindString(3, realFrom);
                            imMessage.setFromID(realFrom);
//                            imcstat.bindString(4, 1 + "");
                        }
//                        imcstat.executeInsert();
//                        icustat.bindString(1, oto);
//                        icustat.bindString(2, "1");
//                        icustat.executeInsert();
                    }
                    if ("consult".equals(message.getType())) {

                        if (from.equals(selfId)) {//?????????????????????
                            if ("5".equals(message.getQchatid())) {//?????????????????? ???????????????
//                                imstat.bindString(13, realTo);
                                imMessage.setRealfrom(realTo);
//                                isstat.bindString(2, realTo);//consult?????????????????? sessionlist??????realJid
//                                updatestat.bindString(4, realTo);
//                                isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_CONSULT_SERVER));//consult??????????????? ????????????
                            } else {//?????????????????????,???????????????,?????????????????????
//                                isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_CONSULT));
                            }
                        } else {//??????????????????
                            if ("5".equals(message.getQchatid())) {//?????????????????????
//                                isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_CONSULT));//consult??????????????? ????????????
                            } else {//????????????,??????????????????,
//                                imstat.bindString(13, realFrom);
                                imMessage.setRealfrom(realFrom);
//                                updatestat.bindString(4, realFrom);
//                                isstat.bindString(2, realFrom);//consult?????????????????? sessionlist??????realJid
//                                isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_CONSULT_SERVER));
                            }
                        }
                    }
                    if ("headline".equals(message.getType())) {
//                        imstat.bindString(2, Constants.SYS.SYSTEM_MESSAGE);
                        imMessage.setConversationID(Constants.SYS.SYSTEM_MESSAGE);
//                        imstat.bindString(3, "history");
                        imMessage.setFromID("history");
//                        imstat.bindString(4, selfId);
                        imMessage.setToID(selfId);
//                        imstat.bindString(9, "0");
                        imMessage.setDirection(0);
//                        imstat.bindString(13, Constants.SYS.SYSTEM_MESSAGE);
                        imMessage.setRealfrom(Constants.SYS.SYSTEM_MESSAGE);
                    }
                    if ("subscription".equals(message.getType())) {
//                        isstat.bindString(6, String.valueOf(ConversitionType.MSG_TYPE_SUBSCRIPT));
                    }

//                    if (!isUpdown) {//??????????????????(????????????????????????session?????? ???????????????????????????&??????????????????)
//                        int count = updatestat.executeUpdateDelete();
//                        if (count <= 0) {//??????????????????
//                            isstat.executeInsert();
//                        }
//                    } else {//??????????????? ???????????? ??????????????????????????????????????????????????????????????????
//
//                    }
//                    long count = imstat.executeInsert();
//                    if (count <= 0 && msgType.equals(String.valueOf(ProtoMessageOuterClass.MessageType.MessageTypeRevoke_VALUE))) {//???????????? update body
//                        revokeStat.bindString(1, body.getContent());
//                        revokeStat.bindString(2, String.valueOf(IMMessage.DIRECTION_MIDDLE));
//                        revokeStat.bindString(3, String.valueOf(ProtoMessageOuterClass.MessageType.MessageTypeRevoke_VALUE));
//                        revokeStat.bindString(4, msgId);
//                        revokeStat.executeUpdateDelete();
//                    }
                } catch (Exception e) {
//                    success = false;
                    continue;
                }

                messageList.add(imMessage);
            }
//            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.e(e, "bulkInsertMessage crashed.");
            throw e;
        } finally {
//            db.endTransaction();
        }
        return messageList;
    }

    public List<MedalListResponse.DataBean.MedalListBean> selectMedalList(){
        return IMDatabaseManager.getInstance().selectMedalList();
    }

    public List<UserHaveMedalStatus> selectUserHaveMedalStatus(String userid,String host){
        return IMDatabaseManager.getInstance().selectUserHaveMedalStatus(userid,host);
    }

    public List<IMMessage> ParseHistoryGroupChatData(List<JSONMucHistorys.DataBean> list, String selfUser){
        String sql = "insert or ignore into IM_Message(MsgId, XmppId, \"From\", \"To\", Content, " +
                "Platform, Type, State, Direction,LastUpdateTime,ReadedTag,MessageRaw,RealJid,ExtendedInfo) values" +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?);";
        List<IMMessage> messageList = new ArrayList<>();
            //???????????????
            int size = list.size();
            for (int i = 0; i < size; i++) {
                IMMessage imMessage = new IMMessage();
                Cursor cursor = null;
                try {
                    JSONMucHistorys.DataBean msg = list.get(i);
                    JSONMucHistorys.DataBean.BodyBean body = msg.getBody();
                    JSONMucHistorys.DataBean.MessageBean message = msg.getMessage();
                    JSONMucHistorys.DataBean.TimeBean time = msg.getTime();
                    if (msg == null || body == null || message == null || time == null) {
                        continue;
                    }
                    String msgId = body.getId();

//                    imstat.bindString(1, msgId);
                    imMessage.setId(msgId);
                    imMessage.setMessageID(msgId);
                    imMessage.setType(ConversitionType.MSG_TYPE_GROUP);
//                    imstat.bindString(2, message.getTo());
                    imMessage.setConversationID(message.getTo());

//                    imstat.bindString(3, message.getSendjid());
                    imMessage.setFromID(message.getSendjid());

//                    imstat.bindString(4, "");
                    imMessage.setToID(message.getTo());

//                    imstat.bindString(5, body.getContent());
                    imMessage.setBody(body.getContent());
                    if (!TextUtils.isEmpty(message.getClient_type())) {
                        switch (message.getClient_type()) {
                            case "ClientTypeMac":
//                                imstat.bindString(6, 1 + "");
                                imMessage.setMaType(1+"");
                                break;
                            case "ClientTypeiOS":
//                                imstat.bindString(6, 2 + "");
                                imMessage.setMaType(2+"");
                                break;
                            case "ClientTypePC":
//                                imstat.bindString(6, 3 + "");
                                imMessage.setMaType(3+"");
                                break;
                            case "ClientTypeAndroid":
//                                imstat.bindString(6, 4 + "");
                                imMessage.setMaType(4+"");
                                break;
                            default:
//                                imstat.bindString(6, 0 + "");
                                imMessage.setMaType(0+"");
                                break;
                        }
                    } else if (!TextUtils.isEmpty(body.getMaType())) {
//                        imstat.bindString(6, body.getMaType());
                        imMessage.setMaType(body.getMaType());
                    } else {
//                        imstat.bindString(6, 0 + "");
                        imMessage.setMaType(0+"");
                    }
                    String msgType = body.getMsgType();
//                    imstat.bindString(7, msgType);
                    imMessage.setMsgType(Integer.parseInt(msgType));
                    //???????????????????????????,?????????????????????????????????
//                    imstat.bindString(8, String.valueOf(MessageStatus.LOCAL_STATUS_SUCCESS_PROCESSION));
                    imMessage.setMessageState(MessageStatus.LOCAL_STATUS_SUCCESS_PROCESSION);
                    //?????????????????????????????????,????????????????????????
                    //????????????????????????,??????????????????????????????
                    if (selfUser.equals(message.getRealfrom())) {
//                        imstat.bindString(9, "1");
                        imMessage.setDirection(1);
                    } else {
//                        imstat.bindString(9, "0");
                        imMessage.setDirection(0);
                    }
                    if ("-1".equals(body.getMsgType())) {
//                        imstat.bindString(9, "2");
                        imMessage.setDirection(2);
//                        imstat.bindString(3, message.getFrom());
                        imMessage.setFromID(message.getFrom());
//                        imstat.bindString(5, msg.getNick() + body.getContent());
                        imMessage.setBody(msg.getNick()+body.getContent());
                    }
                    if ("15".equals(body.getMsgType())) {
//                        imstat.bindString(9, "2");
                        imMessage.setDirection(2);
//                        imstat.bindString(3, message.getFrom());
                    }
                    String t = "";
                    if (TextUtils.isEmpty(message.getMsec_times())) {
                        if (time == null || TextUtils.isEmpty(time.getStamp())) {
                            new String();
                        }
                        String d = time.getStamp();
                        String str = "yyyyMMdd'T'HH:mm:ss";
                        SimpleDateFormat sdf = new SimpleDateFormat(str);
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                        Date date = null;
                        try {
                            if (TextUtils.isEmpty(d)) {
                                date = new Date();
                            } else {
                                date = sdf.parse(d);


                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        t = date.getTime() + "";
                    } else {
                        t = message.getMsec_times();
                    }
//                    imstat.bindString(10, t);
                    imMessage.setTime(new Date(Long.parseLong(t)));
                    //??????????????????????????? ??????????????????????????????
//                    imstat.bindString(11, String.valueOf(isUpturn ? MessageStatus.REMOTE_STATUS_CHAT_READED : MessageStatus.REMOTE_STATUS_CHAT_DELIVERED));
                    imMessage.setReadState(MessageStatus.REMOTE_STATUS_CHAT_READED);

//                    imstat.bindString(12, JsonUtils.getGson().toJson(msg));

//                    imstat.bindString(13, message.getTo());
                    imMessage.setRealfrom(message.getSendjid());
                    if (!TextUtils.isEmpty(body.getExtendInfo())) {
//                        imstat.bindString(14, body.getExtendInfo());
                        imMessage.setExt(body.getExtendInfo());
                    } else if (!TextUtils.isEmpty(body.getBackupinfo())) {
//                        imstat.bindString(14, body.getBackupinfo());
                        imMessage.setExt(body.getBackupinfo());
                    } else {
//                        imstat.bindString(14, "");
                        imMessage.setExt("");
                    }




                } catch (Exception e) {
                    continue;

                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                messageList.add(imMessage);
            }

            return messageList;
        }


    /**
     * ?????????????????? ???????????? ????????????(chat)
     * @param xmppid
     * @param realJid
     * @param t
     */
    public List<IMMessage> selectChatMessageAfterSearch(String xmppid, String realJid, long t){
        List<IMMessage> list = IMDatabaseManager.getInstance().selectChatMessageAfterSearch(xmppid,realJid,t);
        if(list!=null && !list.isEmpty()){
            return list;
        }else {
            List<IMMessage> noMessage = createNoMessage();
            return noMessage;
        }
    }

    /**
     * ?????????????????? ???????????? ????????????(Group)
     * @param xmppid
     * @param realJid
     * @param t
     */
    public List<IMMessage> selectGroupMessageAfterSearch(String xmppid, String realJid, long t){
        List<IMMessage> list = IMDatabaseManager.getInstance().selectGroupMessageAfterSearch(xmppid,realJid,t);
        if(list!=null && !list.isEmpty()){
            return list;
        }else {
            List<IMMessage> noMessage = createNoMessage();
            return noMessage;
        }
    }

    /**
     * ?????????????????????????????????
     * @param searchText
     * @return
     */
    public List<Nick> SelectUserListBySearchText(String groupId,String searchText){
        return IMDatabaseManager.getInstance().SelectUserListBySearchText(groupId,searchText);
    }

    public List<String> SelectAllUserListBySearchText(String searchText){
        return IMDatabaseManager.getInstance().SelectAllUserXmppIdListBySearchText(searchText);
    }

    /**
     * ????????? ????????????
     * @param groupId
     * @return
     */
    public List<Nick> selectFriendsForGroupAdd(String groupId){
        return IMDatabaseManager.getInstance().selectFriendListForGroupAdd(groupId);
    }

    /**
     * ?????????
     * @param groupId
     * @return
     */
    public List<Nick> selectGroupMemberForKick(String groupId){
        return IMDatabaseManager.getInstance().selectGroupMemberForKick(groupId);
    }

    /**
     * ??????groupid&searchindex???????????????
     * @param groupId
     * @param searchIndex
     * @return
     */
    public List<Nick> selectMemberFromGroup(String groupId,String searchIndex){
        return IMDatabaseManager.getInstance().selectMemberFromGroup(groupId,searchIndex);
    }


    /**
     * ???????????????????????????????????????
     *
     * @return
     */
    public List<Nick> SelectAllContacts() {

        return IMDatabaseManager.getInstance().SelectAllUserCard();
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public List<Nick> SelectAllGroup() {
        return IMDatabaseManager.getInstance().SelectAllGroupCard();
    }

    /**
     * ????????????????????????????????????
     * @param searchText
     * @return
     */
    public List<Nick> SelectGroupListBySearchText(String searchText,int limit){
        return IMDatabaseManager.getInstance().SelectGroupListBySearchText(searchText,limit);
    }

    /**
     * ???????????????????????????
     *
     * @param ser   ??????????????????
     * @param limit ????????????
     * @return
     */
    public List<Nick> SelectContactsByLike(String ser, int limit) {
        return IMDatabaseManager.getInstance().SelectContactsByLike(ser, limit);
    }

    /**
     * ??????qchat????????????
     *
     * @param ditems
     * @param isReplase
     */
    public void insertQchatOrgDatas(List<DepartmentItem> ditems, boolean isReplase) {
        IMDatabaseManager.getInstance().insertQchatOrgDatas(ditems, isReplase);
    }

    public List<IMGroup> SelectIMGroupByLike(String ser, int limit) {
        return IMDatabaseManager.getInstance().SelectIMGroupByLike(ser, limit);
    }

    /**
     * ??????????????????????????????
     *
     * @param message
     */
    public void updateVoiceMessage(IMMessage message) {
        IMDatabaseManager.getInstance().updateVoiceMessage(message);
    }

    /**
     * ????????????
     *
     * @param message
     */
    public void deleteMessage(IMMessage message) {
        IMDatabaseManager.getInstance().DeleteMessageByMessage(message);
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Delete_Message);
    }

    /**
     * ????????????
     *
     * @param xmppId
     * @param realUserId
     */
    public void deleteCoversationAndMessage(String xmppId, String realUserId) {
        IMDatabaseManager.getInstance().DeleteSessionAndMessageByXmppId(xmppId, realUserId);
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Remove_Session, xmppId);
    }

    /**
     * RN???????????????????????????
     *
     * @param key
     * @param start
     * @param len
     */
    public List<RNSearchData> getLocalSearch(String key, int start, int len) {

        return IMDatabaseManager.getInstance().getLocalSearch(key, start, len);
    }

    /**
     * ??????????????????RN???
     *
     * @param key
     * @param start
     * @param len
     * @return
     */
    public List<RNSearchData.InfoBean> getLocalUser(String key, int start, int len) {
        return IMDatabaseManager.getInstance().getLocalUser(key, start, len);
    }

    /**
     * ??????????????????RN???
     *
     * @param key
     * @param start
     * @param len
     * @return
     */
    public List<RNSearchData.InfoBean> getLocalGroup(String key, int start, int len) {
        return IMDatabaseManager.getInstance().getLocalGroup(key, start, len);
    }

    /**
     * ??????????????????RN???
     *
     * @param key
     * @param start
     * @param len
     * @return
     */
    public List<RNSearchData.InfoBean> getOutGroup(String key, int start, int len) {
        return IMDatabaseManager.getInstance().getOutGroup(key, start, len);
    }

    public List<CollectionConversation> SelectCollectionConversationList(String xmppid) {
        return IMDatabaseManager.getInstance().SelectCollectionConversationList(xmppid);
    }

    public void sendCollectionAllRead(String of, String ot) {
        IMDatabaseManager.getInstance().updateCollectionRead(of, ot);
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.COLLECTION_CHANGE, "succes");
    }

    /**
     * ??????????????????
     */
    public void getBindUser() {
        HttpUtil.getBindUser();
    }

    /**
     * ???????????????,???????????????????????????
     */
    public static void clearLastUserInfo() {
        Logger.i("????????????????????????");
        IMLogicManager.getInstance().clearLastUserInfo();
    }

    /**
     * ????????????????????????
     * @param pushName
     */
    public boolean getPushStateBy(int pushName) {
       return IMDatabaseManager.getInstance().getPushStateBy(pushName);
    }

    /**
     * ??????push????????????
     * @param pushIndex ?????? ???????????????
     * @param state ??????????????????
     */
    public void setPushState(int pushIndex, int state) {
        IMDatabaseManager.getInstance().setPushState(pushIndex,state);
    }

    /**
     * ?????????????????????nick????????????
     * @param nick
     */
    public void setNickToCache(Nick nick) {
        IMLogicManager.getInstance().setNickToCache(nick);
    }


    /**
     * ??????value ?????? key
     * @param userConfigData
     * @return
     */
    public List<UserConfigData> selectUserConfigValueInString(UserConfigData userConfigData) {
        return IMDatabaseManager.getInstance().selectUserConfigValueInString(userConfigData);
    }

    public List<QuickReplyData> selectQuickReplies() {
        return IMDatabaseManager.getInstance().selectQuickReplies();
    }

    public int selectWorkWorldNotice() {
        return IMDatabaseManager.getInstance().selectWorkWorldNotice();
    }


    public interface HistoryMessage {
        void onMessageResult(List<IMMessage> messageList);
    }


    //??????????????????
    public List<RecentConversation> SelectConversationList(boolean isOnlyUnRead) {

        return IMDatabaseManager.getInstance().SelectConversationList(isOnlyUnRead);
    }

    //??????sessionmap
    public Map<String,List<AtInfo>> getAtMessageMap(){
        return IMDatabaseManager.getInstance().getAtMessageMap();
    }

    public void initAtMessage(){
        IMDatabaseManager.getInstance().selectAtOwnMessage();
    }
    public String getLastMsg(int type,String msg){
        return IMDatabaseManager.getInstance().getLastMessageText(type,msg);
    }

    public int querryConversationTopCount(){
        return IMDatabaseManager.getInstance().querryConversationTopCount();
    }
    //?????????????????????
//    public List<RecentConversation> SelectConversationListCache() {
//
//        return IMDatabaseManager.getInstance().SelectConversationListCache();
//    }



    //???????????????????????????
    public int SelectUnReadCount() {
        return IMDatabaseManager.getInstance().SelectUnReadCount();
    }


//    public void setConversationTopSession(RecentConversation rc){
//        int version = IMDatabaseManager.getInstance().selectUserConfigVersion();
//        Logger.i("userconfig-???????????????:"+version);
//    }


    /**
     * ????????????????????????,?????????????????? isdel ????????????value??????
     *
     * @param userConfigData
     */
    public void updateUserConfigBatch(UserConfigData userConfigData) {
        IMDatabaseManager.getInstance().updateUserConfigBatch(userConfigData);
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    public int selectUserConfigVersion() {
        return IMDatabaseManager.getInstance().selectUserConfigVersion();
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param newRemoteConfig
     */
    public static void refreshTheConfig(NewRemoteConfig newRemoteConfig) {
        IMDatabaseManager.getInstance().insertUserConfigVersion(newRemoteConfig.getData().getVersion());
        IMDatabaseManager.getInstance().bulkUserConfig(newRemoteConfig);


        for (int i = 0; i < newRemoteConfig.getData().getClientConfigInfos().size(); i++) {
            if (newRemoteConfig.getData().getClientConfigInfos().get(i).getKey().equals(CacheDataType.kCollectionCacheKey)) {
//                ConnectionUtil.getInstance().handleMyEmotion(newRemoteConfig.getData().getClientConfigInfos().get(i));
            } else if(newRemoteConfig.getData().getClientConfigInfos().get(i).getKey().equals(CacheDataType.kMarkupNames)){
                LruCache<String,String> markups = ConnectionUtil.getInstance().selectMarkupNames();
//                Logger.i("initreload map:" + JsonUtils.getGson().toJson(markups));
                CurrentPreference.getInstance().setMarkupNames(markups);
//                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Show_List);
            } else if(newRemoteConfig.getData().getClientConfigInfos().get(i).getKey().equals(CacheDataType.kStickJidDic)){
                IMDatabaseManager.getInstance().setConversationTopSession(newRemoteConfig.getData().getClientConfigInfos().get(i));

            } else if(newRemoteConfig.getData().getClientConfigInfos().get(i).getKey().equals(CacheDataType.kNoticeStickJidDic)){
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Update_ReMind);
            } else if(newRemoteConfig.getData().getClientConfigInfos().get(i).getKey().equals(CacheDataType.kQuickResponse)) {
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.UPDATE_QUICK_REPLY);
            }
        }
        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Show_List);
    }
    /**
     * ?????????????????????????????????????????????
     * @param dataBean
     */
    public static void refreshTheQuickReply(QuickReplyResult.DataBean dataBean) {
//        IMDatabaseManager.getInstance().insertUserConfigVersion(newRemoteConfig.getData().getVersion());
        IMDatabaseManager.getInstance().batchInsertQuickReply(dataBean);

        QuickReplyUtils.getQuickReplies();

        IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.UPDATE_QUICK_REPLY);
    }

    /**
     * ??????????????????
     * @param hotlines
     */
    public static void cacheHotlines(List<String> hotlines) {
        IMDatabaseManager.getInstance().InsertHotlines(JsonUtils.getGson().toJson(hotlines));
    }


    /**
     * ???????????????????????????
     *
     * @param version
     */
    public void insertUserConfigVersion(int version) {
        IMDatabaseManager.getInstance().insertUserConfigVersion(version);
    }
    public UserConfigData selectUserConfigValueForKey(UserConfigData userConfigData){
        return IMDatabaseManager.getInstance().selectUserConfigValueForKey(userConfigData);
    }


    public interface CallBackByUserConfig{
        public void onCompleted();
        public void onFailure();
    }

    public void setConversationReMindOrCancel(final UserConfigData userConfigData, final CallBackByUserConfig callBackByUserConfig){
        final UserConfigData ucd = IMDatabaseManager.getInstance().selectUserConfigValueForKey(userConfigData);
        if(ucd == null){
            userConfigData.setValue(CacheDataType.Y+"");
            userConfigData.setType(CacheDataType.set);
            userConfigData.setIsdel(CacheDataType.N);

        }else{
            userConfigData.setValue(CacheDataType.N+"");
            userConfigData.setType(CacheDataType.cancel);
            userConfigData.setIsdel(CacheDataType.Y);

        }
        HttpUtil.setUserConfig(userConfigData, new ProtocolCallback.UnitCallback<NewRemoteConfig>() {
            @Override
            public void onCompleted(NewRemoteConfig newRemoteConfig) {
                if (newRemoteConfig.getData().getClientConfigInfos().size() > 0) {
                    ConnectionUtil.getInstance().refreshTheConfig(newRemoteConfig);
                    callBackByUserConfig.onCompleted();
                }else{
                    callBackByUserConfig.onFailure();
                }
            }

            @Override
            public void onFailure(String errMsg) {
                callBackByUserConfig.onFailure();

            }
        });
    }

    public void setConversationTopOrCancel(final UserConfigData userConfigData, final CallBackByUserConfig callBackByUserConfig) {
        final UserConfigData ucd = IMDatabaseManager.getInstance().selectUserConfigValueForKey(userConfigData);
        //todo ???????????????????????????????????? ??????????????????
        if (ucd == null) {
            UserConfigData.TopInfo topInfo = userConfigData.getTopInfo();
            topInfo.setTopType(CacheDataType.Y+"");
            userConfigData.setValue(topInfo.toJson());
            userConfigData.setType(CacheDataType.set);
            userConfigData.setIsdel(CacheDataType.N);

        } else {

            UserConfigData.TopInfo topInfo = JsonUtils.getGson().fromJson(ucd.getValue(),UserConfigData.TopInfo.class);
            if ((CacheDataType.Y + "").equals(topInfo.getTopType())) {
                topInfo.setTopType(CacheDataType.N + "");
                userConfigData.setType(CacheDataType.cancel);
//                ucd.setValue();
            } else {
                topInfo.setTopType(CacheDataType.Y + "");
                userConfigData.setType(CacheDataType.set);
//                ucd.setValue(CacheDataType.Y + "");
            }
            userConfigData.setValue(topInfo.toJson());

        }

        HttpUtil.setUserConfig(userConfigData, new ProtocolCallback.UnitCallback<NewRemoteConfig>() {
            @Override
            public void onCompleted(NewRemoteConfig newRemoteConfigs) {
//                            Logger.i("???????????????????????? set");
                if (newRemoteConfigs.getData().getClientConfigInfos().size() > 0) {
//                    IMDatabaseManager.getInstance().insertUserConfigVersion(newRemoteConfigs.getData().getVersion());
//                    IMDatabaseManager.getInstance().bulkUserConfig(newRemoteConfigs);
                    ConnectionUtil.getInstance().refreshTheConfig(newRemoteConfigs);
                    callBackByUserConfig.onCompleted();
                } else {
//                        IMDatabaseManager.getInstance().insertUserConfigVersion(newRemoteConfigs.getData().getVersion());
//                                if(ucd.getType()==CacheDataType.set){
//                                    userConfigData.setIsdel(CacheDataType.Y);
//                                }else{
//                                    userConfigData.setIsdel(CacheDataType.N);
//                                }

//                                userConfigData.setVersion(newRemoteConfigs.getData().getVersion());
//                        ucd.setIsdel(CacheDataType.Y);
//                        IMDatabaseManager.getInstance().insertUserConfigVersion(ucd);
                    callBackByUserConfig.onFailure();
                    //todo ????????????????????????????????????
                }
                IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Show_List);
            }

            @Override
            public void onFailure(String errMsg) {
            callBackByUserConfig.onFailure();
            }
        });

    }

    public void setConversationParams(String key, String value) {
        //?????????????????????
        Map<String, Object> param = new HashMap<>();
        param.put(key, value);
        IMDatabaseManager.getInstance().updateConversationParams(param);

        String params = IMDatabaseManager.getInstance().selectAllConversationParams().toString();
        if (TextUtils.isEmpty(params)) {
            return;
        }
        //??????json??????
        final List<RemoteConfig.ConfigItem> configItems = new ArrayList<>();
        //?????????Item??????,????????????,
        final RemoteConfig.ConfigItem configItem = new RemoteConfig.ConfigItem();
        configItem.key = Constants.SYS.CONVERSATION_PARAMS;
        configItem.value = params;
        String version = DataUtils.getInstance(CommonConfig.globalContext).getPreferences(Constants.Preferences.qchat_conversation_params_version, "0");
        configItem.version = version;
        configItems.add(configItem);

        //????????????
        HttpUtil.setRemoteConfig(configItems, null);
    }

    /**
     * ??????????????????????????????
     */
    public void setNotificationConfig() {
        NotificationConfig notificationConfig = new NotificationConfig();
        notificationConfig.BadgeSetting = ShortcutBadger.isBadgeCounterSupported(CommonConfig.globalContext);
        notificationConfig.NotificationCenterSetting = SystemUtil.isNotificationEnabled(CommonConfig.globalContext);
        notificationConfig.SoundSetting = SystemUtil.isMusicEnable(CommonConfig.globalContext);
        String params = JsonUtils.getGson().toJson(notificationConfig);
        if (TextUtils.isEmpty(params)) {
            return;
        }
        //??????json??????
        final List<RemoteConfig.ConfigItem> configItems = new ArrayList<>();
        //?????????Item??????,????????????,
        final RemoteConfig.ConfigItem configItem = new RemoteConfig.ConfigItem();
        configItem.key = Constants.SYS.NOTIFICATION_CONFIG;
        configItem.value = params;
        configItems.add(configItem);
        //????????????
        HttpUtil.setRemoteConfig(configItems, null);
    }

    /**
     * ????????????sessionList??????xmppid ???realId
     */

    public RecentConversation SelectConversationByRC(RecentConversation rc) {
        return IMDatabaseManager.getInstance().SelectConversationByRC(rc);
    }

    /**
     * ????????????id??????????????????
     *
     * @param xmppid
     * @param realJid
     * @return
     */
    public int SelectUnReadCountByConvid(String xmppid, String realJid, String chatType) {
        if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chatType)) {
            return IMDatabaseManager.getInstance().SelectUnReadCountByConvid(xmppid, xmppid);
        } else if ((ConversitionType.MSG_TYPE_CONSULT_SERVER + "").equals(chatType)) {
            return IMDatabaseManager.getInstance().SelectUnReadCountByConvid(xmppid, realJid);
        } else {
            return IMDatabaseManager.getInstance().SelectUnReadCountByConvid(xmppid, realJid);
        }

    }

    public void ALLMessageRead(){
        IMDatabaseManager.getInstance().ALLMessageRead();
        ProtoMessageOuterClass.ProtoMessage receive = PbAssemblyUtil.getBeenNewReadStateMessage(MessageStatus.STATUS_ALL_READED + "", new JSONArray(), CurrentPreference.getInstance().getPreferenceUserId(), null);
        qtalkSDK.sendMessage(receive);
    }


    /**
     * ????????????????????????
     *
     * @param of
     * @param ot
     * @return
     */
    public int SelecCollectiontUnReadCountByConvid(String of, String ot) {

        return IMDatabaseManager.getInstance().SelectCollectionUnReadCountByConvid(of, ot);


    }

    /**
     * ?????????????????????video
     * @param xmppId
     * @param realJid
     * @param start
     * @param end
     * @return
     */
    public List<IMMessage> searchImageVideoMsg(String xmppId,String realJid,int start,int end){
        return IMDatabaseManager.getInstance().searchImageVideoMsg(xmppId,realJid,start,end);
    }

    /**
     * ????????????
     * @param convId
     * @param limit
     * @return
     */
    public List<IMMessage> searchImageMsg(String convId, int limit) {
        return IMDatabaseManager.getInstance().searchImageMsg(convId, limit);
    }

    /**
     * ??????????????????
     * @param of
     * @param ot
     * @param limit
     * @return
     */
    public List<IMMessage> searchImageMsg(String of,String ot, int limit) {
        return IMDatabaseManager.getInstance().searchImageMsg(of,ot, limit);
    }

    public List<IMMessage> searchFilesMsg(){
        return IMDatabaseManager.getInstance().searchFilesMsg();
    }

    public JSONArray searchFilesMsgByXmppid(String xmppid){
        return IMDatabaseManager.getInstance().searchFilesMsgByXmppId(xmppid);
    }


    public List<IMMessage> searchMsg(String xmppId, String term, int limit){
        return IMDatabaseManager.getInstance().searchMsg(xmppId, term, limit);
    }

    public List<IMMessage> SelectInitReloadCollectionChatMessage(String of, String ot, String chatType, int count, int size) {
        if (chatType.equals("1")) {
            return IMDatabaseManager.getInstance().SelectHistoryCollectionGroupChatMessage(of, ot, count, size);
        } else {
            return IMDatabaseManager.getInstance().SelectHistoryCollectionChatMessage(of, ot, count, size);
        }

    }

    //??????????????????????????? ???????????????????????????????????????,???????????????
    public List<IMMessage> SelectInitReloadChatMessage(String xmppid, String realjid, String chatType, int count, int size) {
        if ((ConversitionType.MSG_TYPE_CONSULT_SERVER + "").equals(chatType)) {
//            sendSingleAllRead(realjid, MessageStatus.STATUS_SINGLE_READED+"");
            return IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, realjid, count, size);
        } else if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chatType)) {
//            sendSingleAllRead(xmppid,MessageStatus.STATUS_SINGLE_READED+"");
            return IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, xmppid, count, size);
        } else {
//            sendSingleAllRead(realjid,MessageStatus.STATUS_SINGLE_READED+"");
            return IMDatabaseManager.getInstance().SelectHistoryChatMessage(xmppid, realjid, count, size);
        }
//        if(CommonConfig.isQtalk&&(chatType.equals(ConversitionType.MSG_TYPE_CONSULT_SERVER)||chatType.equals(ConversitionType.MSG_TYPE_CONSULT))){
//
//            //???????????????????????????
//
//        }else {
//
//            //???????????????????????????
//
//        }

    }

    //????????????????????????????????? ???????????????????????????????????????,???????????????
    public List<IMMessage> SelectInitReloadGroupChatMessage(String xmppid, String realJid, int count, int size) {
//        sendGroupAllRead(xmppid);
        //???????????????????????????
        return IMDatabaseManager.getInstance().SelectHistoryGroupChatMessage(xmppid, realJid, count, size);
    }

    public List<IMMessage> SelectInitReloadCollectionGroupChatMessage(String of, String ot, int start, int firstLoadCount) {
        return IMDatabaseManager.getInstance().SelectHistoryCollectionGroupChatMessage(of, ot, start, firstLoadCount);
    }


    /**
     * ???????????????????????????????????????
     * @param realJid
     * @param state
     * @param msgId
     */
    public void sendMessageOperation(String realJid,String state,String msgId){
        try {
            JSONArray array = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", msgId);
            array.put(jsonObject);
            IMDatabaseManager.getInstance().UpdateReadState(array, MessageStatus.REMOTE_STATUS_CHAT_OPERATION);
            ProtoMessageOuterClass.ProtoMessage sendMessage = PbAssemblyUtil.getBeenNewReadStateMessage(state,array, realJid, IMLogicManager.getInstance().getMyself());
            //TODO ????????????????????????????????????
//            IMMessage newReadMessage = PbParseUtil.parseReceiveReadMessage(sendMessage);
//            IMNotificaitonCenter.getInstance().postMainThreadNotificationName(QtalkEvent.Chat_Message_Read_State, newReadMessage);
            //????????????
            qtalkSDK.sendMessage(sendMessage);
        }catch (Exception e){
            Logger.i(e.getMessage());
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param realJid
     */
    public void sendSingleAllRead(String xmppid,String realJid,String state) {
        if(!IMLogicManager.getInstance().isAuthenticated()){
            return;
        }
        boolean isHasMore = true;
        while (isHasMore){//???????????? ???????????????????????????????????? ??????limit200
            //??????????????????????????????
            JSONArray jsonArray = IMDatabaseManager.getInstance().SelectUnReadByXmppid(xmppid,realJid,200);
            if (jsonArray != null && jsonArray.length() > 0) {
                if(jsonArray.length() < 200){
                    isHasMore = false;
                }
                IMDatabaseManager.getInstance().UpdateReadState(jsonArray, MessageStatus.REMOTE_STATUS_CHAT_READED);
                //??????pb??????
                ProtoMessageOuterClass.ProtoMessage sendMessage = PbAssemblyUtil.getBeenNewReadStateMessage(state,jsonArray, realJid, IMLogicManager.getInstance().getMyself());
                //????????????
                qtalkSDK.sendMessage(sendMessage);
            } else {
                isHasMore = false;
            }
        }
    }

    /**
     * ???????????????????????????
     *
     * @param xmppid
     */
    public void sendGroupAllRead(String xmppid) {

        if(!IMLogicManager.getInstance().isAuthenticated()){
            return;
        }
        IMMessage imMessage = new IMMessage();
        imMessage.setConversationID(xmppid);
        imMessage.setTime(new Date());

        //??????????????????
        setGroupRead(imMessage);
    }


    /**
     * ??????????????????
     *
     * @param imMessage
     */
    public void setGroupRead(IMMessage imMessage) {
        //home????????????????????????
        if (CommonConfig.leave) {
            return;
        }
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        //????????????????????????id

        String id = imMessage.getConversationID();
        //?????????id
        String groupId = QtalkStringUtils.parseId(id);
        //????????? ??????
        String domain = QtalkStringUtils.parseGroupDomain(id);
        //??????????????????
        long time = imMessage.getTime().getTime();
        String target = QtalkStringUtils.parseBareJid(id);
        try {
            message.put("id", groupId);
            message.put("domain", domain);
            message.put("t", time);
            messages.put(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //????????????????????????????????????????????????????????????readmark response????????????????????????
       IMDatabaseManager.getInstance().updateGroupMessageReadedTag(id,MessageStatus.REMOTE_STATUS_CHAT_READED,time);
        ProtoMessageOuterClass.ProtoMessage sendMessage = PbAssemblyUtil.getGroupBeenReadMessage(messages, target, IMLogicManager.getInstance().getMyself());
        qtalkSDK.sendMessage(sendMessage);
    }

    /**
     * ?????????????????????????????????
     *
     * @param imMessage
     */
    //?????????????????????????????????
    public void setSingleRead(IMMessage imMessage,String state) {

        if(!IMLogicManager.getInstance().isAuthenticated()){
            return;
        }

        //home????????????????????????
        if (CommonConfig.leave) {
            return;
        }
        //??????JSONArray ???????????????????????????id
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        String messageId = imMessage.getMessageId();

        String target = "";

        if("5".equals(imMessage.getQchatid())||"4".equals(imMessage.getQchatid())){
            target = imMessage.getRealfrom();
        }else{
            target = imMessage.getFromID();
        }
//        String from = ProtoMessageParseUtil.getSingleMessageFrom(protoMessage);
        try {
            message.put("id", messageId);
            messages.put(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IMDatabaseManager.getInstance().UpdateReadState(messages, MessageStatus.REMOTE_STATUS_CHAT_READED);
//        ProtoMessageOuterClass.ProtoMessage sendMessage = PbAssemblyUtil.getBeenReadMessage(messages, target, IMLogicManager.getInstance().getMyself());
        ProtoMessageOuterClass.ProtoMessage sendMessage = PbAssemblyUtil.getBeenNewReadStateMessage(state,messages, target, IMLogicManager.getInstance().getMyself());

        qtalkSDK.sendMessage(sendMessage);

    }

    public RecentConversation selectRecentConversationByXmppId(String xmppid) {
        return IMDatabaseManager.getInstance().selectRecentConversationByXmppId(xmppid);
    }

    /**
     * ??????????????????
     */
    public void sendHeartBeat() {

//        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getHeartBeatMessage();
        qtalkSDK.sendHeartMessage();
    }

    /**
     * ????????????????????????
     */
    public void sendVerifyFriend(String target){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getVerifyFriendModeMessage(target);
        qtalkSDK.sendMessage(protoMessage);
    }

    public void verifyFriend(String target,String from){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getVerifyFriendMessage(target,from);
        qtalkSDK.sendMessage(protoMessage);
    }

    public void verifyFriend(String target,String from,String answer){

        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getVerifyFriendMessage(target,from,answer);
        qtalkSDK.sendMessage(protoMessage);
    }


    //???????????????emj????????????,?????????????????????
//    public void sendTextOrEmojiMessage(String str,String toId,String fromId)  {
//        //?????????????????? ???????????????????????????message??????
//        String msg = PbChatTextHelper.textToHTML(str);
//        //?????????????????????protoMessage
//       ProtoMessageOuterClass.ProtoMessage protoMessage =  PbAssemblyUtil.getTextOrEmojiMessage(fromId,toId,msg);
////        Logger.i("?????????????????????:"+protoMessage);
////        try {
////            Logger.i("?????????????????????Message:"+ProtoMessageOuterClass.XmppMessage.parseFrom(protoMessage.getMessage()));
////        } catch (InvalidProtocolBufferException e) {
////            e.printStackTrace();
////        }
//        //???ProtoMessage?????????PBIMMessage
//        PBIMMessage pbimMessage = null;
//        try {
//            pbimMessage = ProtoMessageParseUtil.sendPbMessage2IMMessage(protoMessage);
//        } catch (InvalidProtocolBufferException e) {
//            e.printStackTrace();
//        }
//        //??????sdk??????????????????
//        //???????????????, ??????????????????????????? ????????????2
//
//        //????????????
//        qtalkSDK.sendMessage(protoMessage);
//        //??????PBIMMessage??????????????????????????????!
//
//    }
//------------------------------------------------IQ??????---------------------------------------------

    /**
     * ???????????????IQ????????????
     *
     * @param key
     */
    public void getMembersAfterJoin(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getMembersAfterJoin(key);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ?????????IQ????????????
     *
     * @param key
     */
    public void createGroup(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.createGroup(key);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ????????????????????????
     * @param groupId
     * @param xmppid
     * @param nickName
     * @param isAdmin
     */
    public void setGroupAdmin(String groupId,String xmppid,String nickName,boolean isAdmin){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.setGroupAdmin(groupId,xmppid,(nickName == null ? "" :nickName),isAdmin);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ???????????????(v2)IQ????????????
     *
     * @param groupId
     * @param invitedList
     */
    public void inviteMessageV2(String groupId, List<String> invitedList) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.inviteMessageV2(IMLogicManager.getInstance().getMyself(), groupId, invitedList);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ????????????IQ????????????
     *
     * @param key
     */
    public void regitstInGroup(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.regitstInGroup(key);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ?????????IQ????????????
     *
     * @param key
     */
    public void leaveGroup(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.leaveGroup(key);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ????????????
     *
     * @param key
     */
    public void destroyGroup(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.destroyGroup(key);
        qtalkSDK.sendMessage(protoMessage);
    }


    /**
     * ????????????IQ????????????
     *
     * @param key
     */
    public void getFriends(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getFriends(CurrentPreference.getInstance().getPreferenceUserId());
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ????????????
     * @param jid
     * @param domain
     */
    public void deleteFriend(String jid,String domain){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.deleteFriend(IMLogicManager.getInstance().getMyself(),jid,domain);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ???????????????????????????IQ????????????
     *
     * @param key
     */
    public void getUserMucs(String key) {
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.getUserMucs(CurrentPreference.getInstance().getPreferenceUserId());
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ???????????????
     *
     * @param groupId
     * @param map
     */
    public void delGroupMember(String groupId, Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.delGroupMember(IMLogicManager.getInstance().getMyself(), groupId, entry.getKey(), entry.getValue());
            qtalkSDK.sendMessage(protoMessage);
        }
    }

    public  List<WorkWorldItem> selectHistoryWorkWorldItem(int size,int limit){
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldItem(size, limit,"");
    }

    public List<WorkWorldItem> selectHistoryWorkWorldItem(int size, int limit, String searchId) {
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldItem(size, limit,searchId);
    }


    public List<WorkWorldNewCommentBean> selectHistoryWorkWorldCommentItem(int size,int limit){
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldCommentItem(size, limit);
    }

    public List<WorkWorldNewCommentBean> selectHistoryWorkWorldNewCommentBean(int size,int limit,String uuid){
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldNewCommentBean(size, limit,uuid);
    }

    public List<WorkWorldNoticeItem> selectHistoryWorkWorldNotice(int size,int limit){
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldNotice(size, limit);
    }

    public List<? extends WorkWorldNoticeItem> selectHistoryWorkWorldNoticeByEventType(int size, int limit, List<String> eventType, boolean isAtShow){
        return IMDatabaseManager.getInstance().selectHistoryWorkWorldNoticeByEventType(size, limit,eventType, isAtShow);
    }

    public interface WorkWorldCallBack{
        void callBack(WorkWorldItem item);
        void goToNetWork();
    }


    public boolean SelectWorkWorldPremissions(){
        return IMDatabaseManager.getInstance().SelectWorkWorldPremissions();
    }

    public boolean SelectWorkWorldRemind(){
        return IMDatabaseManager.getInstance().SelectWorkWorldRemind();
    }

    public void getWorkWorldByUUID(String uuid, final WorkWorldCallBack workWorldCallBack){
        WorkWorldItem item = IMDatabaseManager.getInstance().selectWorkWorldItemByUUID(uuid);
        if(item!=null){
            workWorldCallBack.callBack(item);
        }else{
            workWorldCallBack.goToNetWork();
            HttpUtil.getWorkWorldItemByUUID(uuid, new ProtocolCallback.UnitCallback<WorkWorldSingleResponse>() {
                @Override
                public void onCompleted(WorkWorldSingleResponse workWorldSingleResponse) {
                    //??????????????????????????????????????????
                    workWorldSingleResponse.getData().setPostType("1");
                    workWorldCallBack.callBack(workWorldSingleResponse.getData());
                }

                @Override
                public void onFailure(String errMsg) {
                    workWorldCallBack.callBack(null);
                }
            });
        }
    }



    //--------------------------------------------presence??????-------------------------------------------
    public void setUserState(String state){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.setUserState(state);
        qtalkSDK.sendMessage(protoMessage);
    }

    /**
     * ????????????
     * @param json
     * @param from
     * @param target
     */
    public void conversationSynchronizationMessage(String json, String from, String target){
        ProtoMessageOuterClass.ProtoMessage protoMessage = PbAssemblyUtil.conversationSynchronizationMessage(json, from, target);
        qtalkSDK.sendMessage(protoMessage);
    }

    public void lanuchChatVideo(boolean isVideo, String caller, String callee){
        Intent intent = new Intent("com.qunar.im.START_BROWSER");
        intent.setClassName(CommonConfig.globalContext, "com.qunar.im.ui.activity.QunarWebActvity");
        StringBuilder url = new StringBuilder(QtalkNavicationService.getInstance().getVideoHost());
        Map<String,String> params = new HashMap<>();
        params.put("video",String.valueOf(isVideo));
        Protocol.spiltJointUrl(url,params);
        intent.setData(Uri.parse(url.toString()));
        intent.putExtra(Constants.BundleKey.IS_HIDE_BAR, true);
        intent.putExtra(Constants.BundleKey.IS_VIDEO_AUDIO_CALL,true);
        intent.putExtra(Constants.BundleKey.VIDEO_AUODIO_CALLER,caller);
        intent.putExtra(Constants.BundleKey.VIDEO_AUODIO_CALLEE,callee);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CommonConfig.globalContext.startActivity(intent);
    }

    public void lanuchGroupVideo(String roomId,String groupName){
        Intent intent = new Intent("com.qunar.im.START_BROWSER");
        intent.setClassName(CommonConfig.globalContext, "com.qunar.im.ui.activity.QunarWebActvity");
        //???????????????webview??????????????????https???????????????????????????http?????????????????????https
        String videoUrl = QtalkNavicationService.getInstance().getVideoHost();

        StringBuilder url = new StringBuilder(videoUrl + "conference#/login");
        Map<String,String> params = new HashMap<>();
        params.put("userId",CurrentPreference.getInstance().getPreferenceUserId());
        params.put("roomId",roomId);
        params.put("topic", groupName);
        params.put("plat", "2");
        Protocol.spiltJointUrl(url,params);
        intent.setData(Uri.parse(url.toString()));
        intent.putExtra(Constants.BundleKey.IS_HIDE_BAR, true);
        intent.putExtra(Constants.BundleKey.IS_VIDEO_AUDIO_CALL,true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        CommonConfig.globalContext.startActivity(intent);
    }

    /**
     * ????????????????????????????????????
     */
    public void checkAlipayAccount(){
        IMPayManager.getInstance().checkAlipayAccount();
    }

    /**
     * ?????????????????????
     * @param uid
     */
    public void bindAlipayAccount(String uid,String openId){
        IMPayManager.getInstance().bindAlipayAccount(uid,openId);
    }

    /**
     * ????????????????????????
     * @param isOld
     */
    public void switchSearchVersion(boolean isOld){
        IMDatabaseManager.getInstance().insertFocusSearchCacheData(isOld+"");
    }
    /**
     * ????????????????????????
     *
     * @return
     */
    public List<Nick> selectCollectionUser() {
        List<Nick> list = IMDatabaseManager.getInstance().selectCollectionUser();
        return list;
    }

    public void setAllMsgRead(){
        IMDatabaseManager.getInstance().updateAllRead();
    }

    private List<IMMessage> createNoMessage(){
        List<IMMessage> noMessage = new ArrayList<IMMessage>();
        IMMessage imMessage = new IMMessage();
        String uid = UUID.randomUUID().toString();
        imMessage.setId(uid);
        imMessage.setMessageID(uid);
        imMessage.setDirection(2);
        imMessage.setMsgType(MessageType.MSG_TYPE_NO_MORE_MESSAGE);
        imMessage.setBody("?????????????????????");
        noMessage.add(imMessage);
        return noMessage;
    }

    /****************************** TEST ***********************/
    public String queryMessageContent(String msgid){
        return IMDatabaseManager.getInstance().queryMessageContent(msgid);
    }

    public void resetUnreadCount(){
        IMDatabaseManager.getInstance().resetUnreadCount();
    }

    /*************************** For Update ************************/
    public boolean isTableExit(String tableName){
        return IMDatabaseManager.getInstance().isTableExit(tableName);
    }
    public boolean isTriggerExit(String triggerName){
        return IMDatabaseManager.getInstance().isTriggerExit(triggerName);
    }

    public void update_DB_version_20(){
        IMDatabaseManager.getInstance().update_DB_version_20();
    }

    public void update_DB_reduction(){
        IMDatabaseManager.getInstance().update_DB_reduction();
    }
}
