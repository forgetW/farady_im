
package com.qunar.im.ui.activity;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.orhanobut.logger.Logger;
import com.qunar.im.base.common.BackgroundExecutor;
import com.qunar.im.base.common.ConversitionType;
import com.qunar.im.base.common.DailyMindConstants;
import com.qunar.im.base.jsonbean.AtInfo;
import com.qunar.im.base.jsonbean.CapabilityResult;
import com.qunar.im.base.jsonbean.DailyMindMain;
import com.qunar.im.base.jsonbean.DailyMindSub;
import com.qunar.im.base.jsonbean.EncryptBeginMsg;
import com.qunar.im.base.jsonbean.ExtendMessageEntity;
import com.qunar.im.base.jsonbean.HongbaoContent;
import com.qunar.im.base.jsonbean.ImgVideoBean;
import com.qunar.im.base.jsonbean.LogInfo;
import com.qunar.im.base.jsonbean.NoticeBean;
import com.qunar.im.base.jsonbean.QunarLocation;
import com.qunar.im.base.jsonbean.RbtMsgBackupInfo;
import com.qunar.im.base.jsonbean.VideoMessageResult;
import com.qunar.im.base.module.IMMessage;
import com.qunar.im.base.module.ImageItem;
import com.qunar.im.base.module.Nick;
import com.qunar.im.base.module.UserConfigData;
import com.qunar.im.base.protocol.NativeApi;
import com.qunar.im.base.protocol.ProtocolCallback;
import com.qunar.im.base.structs.EncryptMessageType;
import com.qunar.im.base.structs.FuncButtonDesc;
import com.qunar.im.base.structs.MessageStatus;
import com.qunar.im.base.structs.MessageType;
import com.qunar.im.base.util.AESTools;
import com.qunar.im.base.util.BinaryUtil;
import com.qunar.im.base.util.ChatTextHelper;
import com.qunar.im.base.util.Constants;
import com.qunar.im.base.util.DataCenter;
import com.qunar.im.base.util.DataUtils;
import com.qunar.im.base.util.DateTimeUtils;
import com.qunar.im.base.util.EventBusEvent;
import com.qunar.im.base.util.FileUtils;
import com.qunar.im.base.util.IMUserDefaults;
import com.qunar.im.base.util.InternDatas;
import com.qunar.im.base.util.JsonUtils;
import com.qunar.im.base.util.ListUtil;
import com.qunar.im.base.util.LogUtil;
import com.qunar.im.base.util.Utils;
import com.qunar.im.base.util.graphics.MyDiskCache;
import com.qunar.im.base.view.faceGridView.EmoticionMap;
import com.qunar.im.base.view.faceGridView.EmoticonEntity;
import com.qunar.im.common.CommonConfig;
import com.qunar.im.common.CurrentPreference;
import com.qunar.im.core.manager.IMLogicManager;
import com.qunar.im.core.manager.IMPayManager;
import com.qunar.im.core.services.QtalkNavicationService;
import com.qunar.im.core.utils.GlobalConfigManager;
import com.qunar.im.log.LogConstans;
import com.qunar.im.log.LogService;
import com.qunar.im.log.QLog;
import com.qunar.im.other.CacheDataType;
import com.qunar.im.permission.PermissionCallback;
import com.qunar.im.permission.PermissionDispatcher;
import com.qunar.im.permission.PermissionUtils;
import com.qunar.im.protobuf.Event.QtalkEvent;
import com.qunar.im.protobuf.common.ProtoMessageOuterClass;
import com.qunar.im.protobuf.dispatch.DispatchHelper;
import com.qunar.im.thirdpush.core.QPushClient;
import com.qunar.im.ui.R;
import com.qunar.im.ui.adapter.ChatViewAdapter;
import com.qunar.im.ui.adapter.ExtendChatViewAdapter;
import com.qunar.im.ui.broadcastreceivers.ShareReceiver;
import com.qunar.im.ui.imagepicker.ImageDataSourceForRecommend;
import com.qunar.im.ui.imagepicker.ImagePicker;
import com.qunar.im.ui.presenter.IAddEmojiconPresenter;
import com.qunar.im.ui.presenter.IChatingPresenter;
import com.qunar.im.ui.presenter.ICloudRecordPresenter;
import com.qunar.im.ui.presenter.IDailyMindPresenter;
import com.qunar.im.ui.presenter.IPGroupRtc;
import com.qunar.im.ui.presenter.ISendLocationPresenter;
import com.qunar.im.ui.presenter.IShakeMessagePresenter;
import com.qunar.im.ui.presenter.IShowNickPresenter;
import com.qunar.im.ui.presenter.impl.DailyMindPresenter;
import com.qunar.im.ui.presenter.impl.MultipleSessionPresenter;
import com.qunar.im.ui.presenter.impl.SendLocationPresenter;
import com.qunar.im.ui.presenter.impl.SingleSessionPresenter;
import com.qunar.im.ui.presenter.views.IChatView;
import com.qunar.im.ui.presenter.views.IShowNickView;
import com.qunar.im.ui.util.EmotionUtils;
import com.qunar.im.ui.util.GenerateRandomPassword;
import com.qunar.im.ui.util.ImageSelectUtil;
import com.qunar.im.ui.util.ProfileUtils;
import com.qunar.im.ui.util.ReflectUtil;
import com.qunar.im.ui.util.WaterMarkTextUtil;
import com.qunar.im.ui.util.atmanager.AtManager;
import com.qunar.im.ui.util.easyphoto.easyphotos.callback.SelectCallback;
import com.qunar.im.ui.util.easyphoto.easyphotos.models.album.entity.Photo;
import com.qunar.im.ui.view.CommonDialog;
import com.qunar.im.ui.view.IconView;
import com.qunar.im.ui.view.QtNewActionBar;
import com.qunar.im.ui.view.RecommendPhotoPop;
import com.qunar.im.ui.view.TipsFloatView;
import com.qunar.im.ui.view.baseView.ViewPool;
import com.qunar.im.ui.view.camera.CameraActivity;
import com.qunar.im.ui.view.chatExtFunc.FuncItem;
import com.qunar.im.ui.view.chatExtFunc.FuncMap;
import com.qunar.im.ui.view.chatExtFunc.OperationView;
import com.qunar.im.ui.view.emojiconEditView.EmojiconEditText;
import com.qunar.im.ui.view.emoticonRain.EmoticonRainUtil;
import com.qunar.im.ui.view.emoticonRain.EmoticonRainView;
import com.qunar.im.ui.view.faceGridView.EmotionLayout;
import com.qunar.im.ui.view.faceGridView.FaceGridView;
import com.qunar.im.ui.view.kpswitch.util.KPSwitchConflictUtil;
import com.qunar.im.ui.view.kpswitch.util.KeyboardUtil;
import com.qunar.im.ui.view.kpswitch.widget.KPSwitchPanelLinearLayout;
import com.qunar.im.ui.view.kpswitch.widget.KPSwitchRootLinearLayout;
import com.qunar.im.ui.view.medias.play.MediaPlayerImpl;
import com.qunar.im.ui.view.medias.record.RecordView;
import com.qunar.im.ui.view.quickreply.QuickReplyLayout;
import com.qunar.im.ui.view.swipBackLayout.SwipeBackActivity;
import com.qunar.im.utils.CapabilityUtil;
import com.qunar.im.utils.ConnectionUtil;
import com.qunar.im.utils.HttpUtil;
import com.qunar.im.utils.QtalkStringUtils;
import com.qunar.im.utils.QuickReplyUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;


/**
 * Created by hubin on 2017/8/13.
 */

public class PbChatActivity extends SwipeBackActivity implements AtManager.AtTextChangeListener, View.OnFocusChangeListener, View.OnClickListener, ImageDataSourceForRecommend.OnImagesLoadedListener,
        IChatView, IShowNickView, PermissionCallback {
    public static final String TAG = "PbChatActivity";


    //???????????? ???????????????????????????
    protected static final int MENU1 = 0x01;
    protected static final int MENU2 = 0x02;
    protected static final int MENU4 = 0x04;
    protected static final int MENU5 = 0x05;
    protected static final int MENU6 = 0x06;
    protected static final int MENU7 = 0x07;
    protected static final int MENU8 = 0x08;
    protected static final int MENU9 = 0x09;
    protected static final int MENU10 = 0x10;

    public static final int ACTIVITY_GET_CAMERA_IMAGE = 1;//??????
    public static final int ACTIVITY_SELECT_PHOTO = 2;//????????????
    public static final int ACTIVITY_SELECT_LOCATION = 3;//????????????
    public static final int FILE_SELECT_CODE = 0x12;//????????????
    public static final int AT_MEMBER = 0x13;//forresult???key ????????????????????????????????????,??????@
    public static final int RECORD_VIDEO = 0x14;//????????????
    public static final int TRANSFER_CONVERSATION_REQUEST_CODE = 0x15;//????????????
    public static final int HONGBAO = 0x16;//??????
    public static final int ADD_EMOTICON = 0x32;//????????????
    public static final int ACTIVITY_SELECT_VIDEO = 0x64;//??????????????????
    public static final int UPDATE_NICK = 0x65;
    //??????
    protected final int SHOW_CAMERA = PermissionDispatcher.getRequestCode();
    protected final int SELECT_PIC = PermissionDispatcher.getRequestCode();
    protected final int RECORD = PermissionDispatcher.getRequestCode();
    protected final int SEND_VIDEO = PermissionDispatcher.getRequestCode();
    protected final int READ_FILE = PermissionDispatcher.getRequestCode();
    protected final int READ_LOCATION = PermissionDispatcher.getRequestCode();
    protected final int SELECT_VIDEO = PermissionDispatcher.getRequestCode();
    protected final int REAL_VIDEO = PermissionDispatcher.getRequestCode();
    protected final int REAL_AUDIO = PermissionDispatcher.getRequestCode();
    protected final int REAL_GROUP_VIDEO = PermissionDispatcher.getRequestCode();
    //???????????????
    private ConnectionUtil connectionUtil;
    //??????intent???????????????key
    public static final String KEY_JID = "jid";
    public static final String KEY_IS_CHATROOM = "isFromChatRoom";
    public static final String KEY_REAL_JID = "realJid";
    public static final String KEY_CHAT_TYPE = "chatType";
    public static final String KEY_ENCRYPT_BODY = "encryptBody";
    public static final String KEY_RIGHTBUTTON = "right_button_type";
    public static final String KEY_INPUTTYPE = "input_type";
    public static final String KEY_AUTO_REPLY = "auto_reply";
    public static final String KEY_BUSI_NAME = "busi_name";
    public static final String KEY_SUPPLIER_ID = "supplier_id";
    public static final String KEY_ATMSG_OWN = "atmsg_own";
    public static final String KEY_SHOW_READSTATE = "show_read_state";

    public static final String KEY_UNREAD_MSG_COUNT = "unread_msg_count";

    public static final String KEY_IS_REMIND = "is_remind";

    public static final String KEY_ATMSG_INDEX = "atmsg_index";

    private static final String TAG_UNREAD_VIEW = "tag_unread_view";
    private static final String TAG_NEW_UNREAD_VIEW = "tag_new_unread_view";
    private static final String TAG_ATMSG_VIEW = "tag_atmsg_view";
    private static final String TAG_SEARCH_VIEW = "tag_search_view";


    private LinkedList<Integer> atMsgIndexs = new LinkedList<>();//@???????????????id
    public int atMsgIndex = 0;
    //????????????
    //???????????????????????????????????????????????????id
    protected String autoReply;
    protected String jid;
    protected String realJid;
    protected String busiName;
    protected String supplierId;
    protected String chatType;
    protected String of;
    protected String ot;
    protected boolean input_type;
    protected FuncMap funcMap = new FuncMap();
    //????????????????????????????????????????????????
    private boolean rightbutton = false;

    public boolean searching;//???????????????????????????


    //????????????
    protected boolean isFromChatRoom;

    private boolean isShowSearch;
    private int searchMoreCount = 0;

    //?????????????????????
    protected IChatingPresenter chatingPresenter;
    private ISendLocationPresenter sendLocationPresenter;
    //?????????????????????
//    private ChatroomInfoPresenter chatroomInfoPresenter;
    //???????????????????????? ??????????????????true

    //??????????????????,???????????????????????? ?????????????????? 20???,?????????????????????id????????????readStats?????????1 ??????,
    boolean isFirstInit = true;

    //????????????????????????????????????
    public String titleTempVar;
    private long lastShakeTime;
    //????????????
    private boolean isSnapMsg;
    //??????????????????
    private String imageUrl;
    //
    private AlertDialog mDialog;

    //????????????id
    private String transferId;
    //??????????????????
    private String mTransferConversationContext;

    private WaterMarkTextUtil waterMarkTextUtil;

    //????????????4??????????????????????????????
    private Runnable typingShow = new Runnable() {
        @Override
        public void run() {
            setActionBarTitle(titleTempVar);
//            myActionBar.getTitleTextview().setText(titleTempVar);
        }
    };


    //??????View
    protected LinearLayout edit_region, linearlayout_tab2, outter_msg_prompt, atom_bottom_more, atom_ui_refence_layout;
    protected OperationView linearlayout_tab;
    protected PullToRefreshListView chat_region;
    protected KPSwitchPanelLinearLayout mPanelRoot;
    //listview????????????
//    private VelocityTracker velocityTracker = null;
    //?????????????????????????????????????????????????????????????????????????????????????????????
    protected TextView send_btn, new_msg_prompt, voice_prompt, outter_msg, no_prompt, close_prompt, atom_ui_refence_text;
    protected ImageView atom_ui_refence_close;
    protected IconView left_btn, tv_options_btn, emotion_btn, voice_switch_btn;
    protected ImageView shareMessgeBtn, deleteMessageBtn, collectMsgBtn, emailMsgBtn;
    protected EmojiconEditText edit_msg;
    protected RelativeLayout atom_bottom_frame;
    protected LinearLayout input_container;
    protected LinearLayout total_bottom_layout;
    protected KPSwitchRootLinearLayout relativeLayout;
    protected RecordView record;
    protected RelativeLayout chating_view;
    protected EmotionLayout faceView;
    protected LinearLayout quickreply_tab;
    protected QuickReplyLayout quickReplyLayout;
    protected EmoticonRainView emoticonRainView;
    protected QtNewActionBar qtNewActionBar;
//    private View line;

    //pblist??????Adapter //???????????????,?????????????????????
//    private PbChatViewAdapter pbChatViewAdapter;
//    private PbExtendChatViewAdapter pbChatViewAdapter;
    protected ExtendChatViewAdapter pbChatViewAdapter;
    //????????????
    AtomicInteger unreadMsgCount = new AtomicInteger(0);
    //?????????????????????,????????????????????????,?????????????????????,?????????0
    protected int newMsgCount = 0;
    //??????????????????boolean ??????????????????
    protected boolean canShowAtActivity = true;
    //?????????????????????
//    protected String realUser;
    //?????????????????????
    protected List<IMMessage> selectedMessages = new ArrayList<>();
    //    protected Map<String,String> atMap = new HashMap<>();
    //@????????????
    private AtManager mAtManager;

    //??????????????????
    private IDailyMindPresenter passwordPresenter;//???????????? ?????????
    String encryptBody;
    private AlertDialog encryptChooseDialog;
    private AlertDialog encryptDialog;
    private AlertDialog encryptSessionDialog;
    private String passwordMainStr;//??????????????????
    private String passwordSubString;//??????????????????
    private boolean isEncryptOperate;//????????????????????? true??????false??????
    HandleChatEvent handleChatEvent = new HandleChatEvent();

    private String refrenceString;
    private boolean isRemind = true;

    private ViewTreeObserver.OnGlobalLayoutListener keyBordOnGlobalLayoutListener;

    public PullToRefreshBase.OnRefreshListener onRefresh = new PullToRefreshBase.OnRefreshListener<ListView>() {
        @Override
        public void onRefresh(PullToRefreshBase<ListView> refreshView) {
            //???????????????????????????:
            loadMoreHistory();
        }

    };

    TextWatcher textWatcher = new TextWatcher() {
        @Override //???????????????
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mAtManager.beforeTextChanged(s, start, count, after);
            //????????????????????????????????? ???????????????????????????,??????@????????????
            if (count > 0 && after == 0) {
                canShowAtActivity = false;
            }
        }

        @Override //????????????
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //???????????????????????????
            if (TextUtils.isEmpty(s) || s.toString().trim().length() == 0) {
                //???????????????????????????
                send_btn.setVisibility(View.GONE);
                //??????????????????????????????
                tv_options_btn.setVisibility(View.VISIBLE);
                return;
            }
            mAtManager.onTextChanged(s, start, before, count);
            //?????????????????????????????????
            tv_options_btn.setVisibility(View.GONE);
            //????????????????????????
            send_btn.setVisibility(View.VISIBLE);
        }

        @Override //???????????????
        public void afterTextChanged(Editable s) {
            //?????????????????????
            if (isFromChatRoom) {
                mAtManager.afterTextChanged(s);
                //?????????????????????????????????????????????????????????????????????@??????
                if (!canShowAtActivity) {
                    canShowAtActivity = true;
                    return;
                }
                //???????????????????????? ????????????"@" ????????????AtListActivity
                //?????????????????????????????????
//                    int atIndex = edit_msg.getSelectionStart();
//                    if (atIndex > 0) {
//                        String at = s.subSequence(atIndex - 1, atIndex).toString();
//                        if (at.equals("@")) {
//                            Intent intent = new Intent(PbChatActivity.this, AtListActivity.class);
//                            intent.putExtra("jid", jid);
//                            startActivityForResult(intent, AT_MEMBER);
//                        }
//                    }
            } else {
                //todo:??????????????????????????????????????????
                //????????????????????????0  ????????????????????????,??????msg?????????????????????
                if (s.length() > 0) {
                    chatingPresenter.sendTypingStatus();
                }

//                    //consult???????????????????????????????????????????????? ??????????????????typing
//                    if (TextUtils.isEmpty(realUser)) {
//
////                        chatingPresenter.sendTypingStatus();
//                    }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atom_ui_activity_chat);
        connectionUtil = ConnectionUtil.getInstance();
        waterMarkTextUtil = new WaterMarkTextUtil();
//        EventBus.getDefault().register(this);
        //bugly tag
//        CrashReportUtils.getInstance().setUserTag(55092);
        //???????????????????????????
        handleExtraData(savedInstanceState);
        //??????intent??????
        injectExtras(getIntent());

        //@????????????
        mAtManager = new AtManager(this, jid);
        mAtManager.setTextChangeListener(this);
        //????????????
        new ImageDataSourceForRecommend(PbChatActivity.this, null, PbChatActivity.this);
        //?????????view
        bindViews();
        initViews();

        //????????????????????????
        if (!TextUtils.isEmpty(encryptBody)) {
            sendEncryptMessage(EncryptMessageType.AGREE);
            parseEncryptPassword(encryptBody);
        }

        EventBus.getDefault().register(handleChatEvent);


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
//        shareIntent = intent;
        isFirstInit = true;
        injectExtras(intent);
        if (isFromChatRoom) {
            pbChatViewAdapter.setShowNick(true);
            pbChatViewAdapter.setShowReadState(false);
        } else {
            pbChatViewAdapter.setShowNick(false);
            pbChatViewAdapter.setShowReadState(isShowReadStateView());
        }


        handleExtraData(null);
        initViews();
        //???????????????????????????
        initGridView();
//        //?????????view
//        bindViews();
//        initViews();
//        //??????????????????
//        addEvents();
    }


    @Override
    protected void onResume() {
        super.onResume();
        //push??????
        QPushClient.clearNotification(this);
        Logger.i("??????:isFirstInit" + isFirstInit);
        if (isFirstInit) {
            clearMessage();
            isFirstInit = false;
            initHistoryMsg();
            //???????????? ??????????????????????????? ??????????????? ?????????????????? (??????????????????????????????????????? ??????????????????????????? ???????????????)
//            if (isFromChatRoom) {
//                DispatchHelper.Async("showMembers", new Runnable() {
//                    @Override
//                    public void run() {
//                        chatroomInfoPresenter.showMembers(false);
//                    }
//                });
//            }
        }
        setReadState();

    }

    public void setReadState() {
        //??????????????????????????? ???????????? ???????????????
        DispatchHelper.Async("sendAllRead", false, new Runnable() {
            @Override
            public void run() {
                //home?????????????????????????????????
                if (!rightbutton) {
                    if (isFromChatRoom) {
                        connectionUtil.sendGroupAllRead(jid);
                    } else {
//                        if ("4".equals(chatType) || "5".equals(chatType)) {
                        if ("5".equals(chatType)) {
                            connectionUtil.sendSingleAllRead(jid, realJid, MessageStatus.STATUS_SINGLE_READED + "");
                        } else {
                            connectionUtil.sendSingleAllRead(jid, jid, MessageStatus.STATUS_SINGLE_READED + "");
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //??????????????????presence
        chatingPresenter.sendSyncConversation();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (isFinishing()) {
//
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (vibrator != null) vibrator.cancel();
        if (record != null) record.destroy();
        ViewPool.clear();
        getHandler().removeCallbacks(typingShow);

        if (keyBordOnGlobalLayoutListener != null) {
            KeyboardUtil.detach(this, keyBordOnGlobalLayoutListener);
        }
        CommonConfig.isPlayVoice = false;
        releaseResource();
        MediaPlayerImpl.getInstance().release();
        String draft = edit_msg.getText().toString();
        if (!TextUtils.isEmpty(draft)) {
            InternDatas.putDraft(QtalkStringUtils.parseBareJid(jid) + QtalkStringUtils.parseBareJid(realJid), draft);
        } else {
            InternDatas.removeDraft(QtalkStringUtils.parseBareJid(jid) + QtalkStringUtils.parseBareJid(realJid));
        }
//        releaseResource();
        super.onDestroy();
    }


    /**
     * ??????finish?????????????????????
     */
    private void releaseResource() {
//        if (faceView != null) faceView.removeAllViews();
//        if (quickReplyLayout != null) quickReplyLayout.removeAllViews();
        if (pbChatViewAdapter != null) pbChatViewAdapter.releaseViews();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        getHandler().removeCallbacksAndMessages(null);
        if (encryptRequestCountTimer != null) encryptRequestCountTimer.cancel();
        if (chatingPresenter != null) {
            chatingPresenter.close();
        }
        if (waterMarkTextUtil != null) {
            waterMarkTextUtil.recyleBitmap();
        }
        EventBus.getDefault().unregister(handleChatEvent);
    }


    //????????????????????????,???????????????????????????
    protected void initHistoryMsg() {
        chatingPresenter.propose();
    }

    //??????intent????????????
    protected void injectExtras(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(KEY_JID)) {
                //?????????????????????????????????realjid ????????????jid???realjid??????

                jid = extras.getString(KEY_JID);
                realJid = extras.getString(KEY_JID);
            }
            if (!TextUtils.isEmpty(extras.getString(KEY_REAL_JID))) {//realJid????????? ???????????????realJid
                realJid = extras.getString(KEY_REAL_JID);
            }
            if (!TextUtils.isEmpty(extras.getString(KEY_BUSI_NAME))) {//bu
                busiName = extras.getString(KEY_BUSI_NAME);
            }
            if (!TextUtils.isEmpty(extras.getString(KEY_SUPPLIER_ID))) {//?????????id
                supplierId = extras.getString(KEY_SUPPLIER_ID);
            }

            if (extras.containsKey(KEY_CHAT_TYPE)) {
                chatType = extras.getString(KEY_CHAT_TYPE);
            }
            if (extras.containsKey(KEY_IS_CHATROOM)) {
                isFromChatRoom = extras.getBoolean(KEY_IS_CHATROOM);
            }
            if (isFromChatRoom) {
                atMsgIndex = extras.getInt(KEY_ATMSG_INDEX);
            }
            if (extras.containsKey(KEY_ENCRYPT_BODY)) {
                encryptBody = extras.getString(KEY_ENCRYPT_BODY);
            }
            if (extras.containsKey(KEY_RIGHTBUTTON)) {
                rightbutton = extras.getBoolean(KEY_RIGHTBUTTON);
            }
            if (extras.containsKey(KEY_AUTO_REPLY)) {
                autoReply = extras.getString(KEY_AUTO_REPLY);
            }
            if (extras.containsKey(KEY_INPUTTYPE)) {
                input_type = extras.getBoolean(KEY_INPUTTYPE);
            }
            if (extras.containsKey(KEY_UNREAD_MSG_COUNT)) {
                unreadMsgCount.set(extras.getInt(KEY_UNREAD_MSG_COUNT, -1));
            }
            if (extras.containsKey(KEY_IS_REMIND)) {
                isRemind = extras.getBoolean(KEY_IS_REMIND, true);
            }
            //??????????????????????????????
            if (extras.containsKey("sendLogFile")) {
                final String logfile = extras.getString("sendLogFile");
                final String content = extras.getString("content");
                if (!TextUtils.isEmpty(logfile)) {
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (chatingPresenter != null) {
                                edit_msg.setText(content);
                                chatingPresenter.sendMsg();
                                edit_msg.setText("");
                                chatingPresenter.sendFile(logfile);
                            }
                        }
                    }, 1000);
                }
            }
        }

        if (chatingPresenter != null) {
            chatingPresenter.close();
        }

        if (isFromChatRoom) {
            chatingPresenter = new MultipleSessionPresenter();
            ((IShowNickPresenter) chatingPresenter).setShowNickView(this);
        } else {
            chatingPresenter = new SingleSessionPresenter();
            sendLocationPresenter = new SendLocationPresenter();
            passwordPresenter = new DailyMindPresenter();
            sendLocationPresenter.setView(this);
        }

        chatingPresenter.setView(this);


    }

    /**
     * @???????????????id?????? index
     */
    public void getAtOwnMsgIndexs() {
        if (!isFromChatRoom) {
            return;
        }
        //??????@???????????????id
        Map<String, List<AtInfo>> atMap = connectionUtil.getAtMessageMap();
        if (atMap != null) {
            List<AtInfo> atMsgOwn = atMap.get(jid);
            if (!ListUtil.isEmpty(atMsgOwn)) {
                for (AtInfo atInfo : atMsgOwn) {
                    String msgid = atInfo.msgId;
                    setIndexByMsgid(msgid);
                }
                if (!atMsgIndexs.isEmpty()) {
                    atMsgIndex = atMsgIndexs.getFirst();
                }
            }
        }
    }

    private void setIndexByMsgid(String msgid) {
        List<IMMessage> messages = pbChatViewAdapter.getMessages();
        int count = messages == null ? 0 : messages.size();
        for (int i = 0; i < count; i++) {
            if (messages.get(i).getId().equals(msgid)) {
                atMsgIndexs.addLast(i);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_JID, jid);
        outState.putString(KEY_REAL_JID, realJid);
        outState.putString(KEY_CHAT_TYPE, chatType);
        outState.putBoolean(KEY_IS_CHATROOM, isFromChatRoom);
        outState.putBoolean(KEY_SHOW_READSTATE, isShowReadStateView());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        handleExtraData(savedInstanceState);
    }

    //???????????????????????????
    protected void handleExtraData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            jid = savedInstanceState.getString(KEY_JID);
            realJid = savedInstanceState.getString(KEY_REAL_JID);
            chatType = savedInstanceState.getString(KEY_CHAT_TYPE);
            isFromChatRoom = savedInstanceState.getBoolean(KEY_IS_CHATROOM);
        }
        if (jid != null) {
            jid = QtalkStringUtils.parseBareJid(jid);
            if (isFromChatRoom) {
                jid = QtalkStringUtils.roomId2Jid(jid);
            } else {
                jid = QtalkStringUtils.userId2Jid(jid);
            }
        }


    }

    /**
     * ?????????class???events??????,??????????????????????????????????????????
     */
    public void addEvents() {
//        //????????????????????????

//        connectionUtil.addEvent(this, QtalkEvent.Chat_Message_Text);
        chat_region.getRefreshableView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_UP:
                        switchStatus(CHAT_STATUS_DEFAULT);
                        KPSwitchConflictUtil.hidePanelAndKeyboard(mPanelRoot);
                        break;
                }
                //??????ej edittext ????????????
                edit_msg.clearFocus();
                //???+????????????layout???????????????
                if (linearlayout_tab2.getVisibility() == View.VISIBLE)
                    linearlayout_tab2.setVisibility(View.GONE);
                if (linearlayout_tab.getVisibility() == View.VISIBLE)
                    linearlayout_tab.setVisibility(View.GONE);
                return false;
            }
        });
    }

    /**
     * ?????????????????????
     *
     * @param edit
     */
    public void onEventMainThread(EventBusEvent.NewPictureEdit edit) {
        if (!TextUtils.isEmpty(edit.mPicturePath)) {
            imageUrl = edit.mPicturePath;
            chatingPresenter.sendImage();
        }
    }

    /**
     * ?????????View
     */
    protected void initViews() {

        if (!rightbutton) {

            //?????????ActionBar
            initActionBar();

        }
        if (input_type) {
            edit_region.setVisibility(View.GONE);
        }
        //?????????????????????
        initInputRegion();

        //???????????????????????????
        initKpswitch();

        //?????????????????????list
        initPbChatRegion();
        //???????????????????????????bug
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            //?????????????????????????????????
            //???????????????????????????????????????
//            AndroidBug5497Workaround.assistActivity(this);
        }

        if (ConnectionUtil.getInstance().isHotline(jid)) {
            getQuestionList();
        }
    }

    private void getQuestionList() {
        Map<String, String> map = new HashMap<>();
        map.put("conf", CommonConfig.isDebug ? "beta" : "prod");
        map.put("tag", jid);
        map.put("to", CurrentPreference.getInstance().getUserid());
        map.put("tohost", QtalkNavicationService.getInstance().getXmppdomain());
//
        String url = QtalkNavicationService.getInstance().getHttpUrl() + "/robot/qtalk_robot/sendtips?rexian_id=" + QtalkStringUtils.parseId(jid) + "&m_from=" + jid + "&m_to=" + CurrentPreference.getInstance().getPreferenceUserId();
        HttpUtil.getUrl(url, new ProtocolCallback.UnitCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean aBoolean) {

            }

            @Override
            public void onFailure(String errMsg) {
//                getQuestionList();//????????? ????????? ????????????
            }
        });
    }

    private void initKpswitch() {
        keyBordOnGlobalLayoutListener = KeyboardUtil.attach(this, mPanelRoot,
                // Add keyboard showing state callback, do like this when you want to listen in the
                // keyboard's show/hide change.
                new KeyboardUtil.OnKeyboardShowingListener() {
                    @Override
                    public void onKeyboardShowing(boolean isShowing) {
                        setTranscriptMode(isShowing);
                        if (isShowing) {
                            switchStatus(CHAT_STATUS_DEFAULT);
                        }

                    }
                });
        // If there are several sub-panels in this activity ( e.p. function-panel, emoji-panel).
        KPSwitchConflictUtil.attach(mPanelRoot, edit_msg,
                new KPSwitchConflictUtil.SwitchClickListener() {
                    @Override
                    public void onClickSwitch(View view, boolean switchToPanel) {
                        setTranscriptMode(switchToPanel);
                        if (switchToPanel) {
                            edit_msg.clearFocus();
                            if (view == emotion_btn) {
                                edit_msg.requestFocus();
                                switchStatus(CHAT_STATUS_EMOTION);
                            } else if (view == voice_switch_btn) {
                                switchStatus(CHAT_STATUS_VOICE);
                            } else if (view == tv_options_btn) {
                                switchStatus(CHAT_STATUS_DEFAULT);
                                showRecommendPop();
                            }
                        } else {
                            if (view == emotion_btn) {

                            } else if (view == voice_switch_btn) {
                                switchStatus(CHAT_STATUS_DEFAULT);
                            }
                        }
                    }

                    @Override
                    public boolean beforeClick(View view) {
                        if (view.getId() == R.id.voice_switch_btn) {
                            boolean isGranted = PermissionUtils.checkPermissionGranted(PbChatActivity.this
                                    , PermissionDispatcher.permissions.get(PermissionDispatcher.REQUEST_RECORD_AUDIO));
                            if (isGranted) {
                                return true;
                            }
                            voice_switch_btnClickListener();
                            return false;
                        }
                        return true;
                    }
                },
                new KPSwitchConflictUtil.SubPanelAndTrigger(linearlayout_tab, tv_options_btn),
                new KPSwitchConflictUtil.SubPanelAndTrigger(linearlayout_tab2, emotion_btn),
                new KPSwitchConflictUtil.SubPanelAndTrigger(record, voice_switch_btn));
    }


    private static final int CHAT_STATUS_DEFAULT = 0;
    private static final int CHAT_STATUS_EMOTION = 1;
    private static final int CHAT_STATUS_VOICE = 2;

    private void switchStatus(int status) {
        switch (status) {
            case CHAT_STATUS_DEFAULT:
                quickreply_tab.setVisibility(View.GONE);
                record.setVisibility(View.GONE);
                voice_prompt.setVisibility(View.GONE);
                left_btn.setVisibility(View.GONE);
                voice_switch_btn.setVisibility(View.VISIBLE);
                input_container.setVisibility(View.VISIBLE);
                if (TextUtils.isEmpty(edit_msg.getText()) || edit_msg.getText().toString().trim().length() == 0) {
                    tv_options_btn.setVisibility(View.VISIBLE);
                    //????????????????????????
                    send_btn.setVisibility(View.GONE);
                } else {
                    tv_options_btn.setVisibility(View.GONE);
                    //????????????????????????
                    send_btn.setVisibility(View.VISIBLE);
                }

                emotion_btn.setText(R.string.atom_ui_new_chat_input_emoji);
                break;
            case CHAT_STATUS_EMOTION:
                record.setVisibility(View.GONE);
                voice_prompt.setVisibility(View.GONE);
                left_btn.setVisibility(View.GONE);
                voice_switch_btn.setVisibility(View.VISIBLE);
                input_container.setVisibility(View.VISIBLE);
                quickreply_tab.setVisibility(View.GONE);
                emotion_btn.setText(R.string.atom_ui_new_chat_input_keybord);
                break;
            case CHAT_STATUS_VOICE:
                input_container.setVisibility(View.GONE);
                voice_switch_btn.setVisibility(View.GONE);
                left_btn.setVisibility(View.VISIBLE);
                record.setVisibility(View.VISIBLE);
                voice_prompt.setVisibility(View.VISIBLE);
                tv_options_btn.setVisibility(View.GONE);
                send_btn.setVisibility(View.GONE);
                quickreply_tab.setVisibility(View.GONE);
                emotion_btn.setText(R.string.atom_ui_new_chat_input_emoji);
                break;
        }
    }

    private void quickReplySwitch() {
        linearlayout_tab.setVisibility(View.GONE);
        edit_msg.clearFocus();
        record.setVisibility(View.GONE);
        voice_prompt.setVisibility(View.GONE);
        left_btn.setVisibility(View.GONE);
        voice_switch_btn.setVisibility(View.VISIBLE);
        input_container.setVisibility(View.VISIBLE);
        quickreply_tab.setVisibility(View.VISIBLE);
    }

    /**
     * ???????????????????????????
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void initPbChatRegion() {
        if (pbChatViewAdapter == null)
            pbChatViewAdapter = new ExtendChatViewAdapter(this, jid, getHandler(), isFromChatRoom);

        pbChatViewAdapter.setShowReadState(!isFromChatRoom && isShowReadStateView());
        //??????????????????????????????
        pbChatViewAdapter.setLeftImageClickHandler(new ChatViewAdapter.LeftImageClickHandler() {
            @Override
            public void onLeftImageClickEvent(String jid) {
                NativeApi.openUserCardVCByUserId(jid);
            }
        });
        //??????menu??????
        pbChatViewAdapter.setContextMenuRegister(new ChatViewAdapter.ContextMenuRegister() {
            @Override
            public void registerContextMenu(View v) {
                registerForContextMenu(v);
                //todo:??????menu??????
            }
        });
        pbChatViewAdapter.setGravatarHandler(new ChatViewAdapter.GravatarHandler() {
            @Override
            public void requestGravatarEvent(String jid, final String imageSrc, final SimpleDraweeView view) {
                ProfileUtils.displayGravatarByImageSrc(PbChatActivity.this, imageSrc, view,
                        getResources().getDimensionPixelSize(R.dimen.atom_ui_image_mid_size), getResources().getDimensionPixelSize(R.dimen.atom_ui_image_mid_size));
            }
        });

        pbChatViewAdapter.setRightSendFailureClickHandler(message -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PbChatActivity.this);
            builder.setTitle(R.string.atom_ui_title_resend_message);

            builder.setPositiveButton(R.string.atom_ui_menu_resend, (dialog, which) -> {
                dialog.dismiss();
                selectedMessages.clear();
                selectedMessages.add(message);
                chatingPresenter.resendMessage();
                selectedMessages.clear();
            });

            builder.setNegativeButton(R.string.atom_ui_common_cancel, (dialog, which) -> dialog.dismiss());
            builder.create().show();
        });
        //?????????????????????,????????????????????????@??????
        if (isFromChatRoom) {
            pbChatViewAdapter.setLeftImageLongClickHandler(from -> connectionUtil.getUserCard(from, new IMLogicManager.NickCallBack() {
                @Override
                public void onNickCallBack(final Nick nick) {
                    runOnUiThread(() -> {
                        edit_msg.requestFocus();
                        if (nick != null && !TextUtils.isEmpty(nick.getName())) {
                            mAtManager.insertAitMemberInner(from, nick.getName(), edit_msg.getSelectionStart(), true);
//                                        edit_msg.setText(edit_msg.getText() + "@" + nick.getName() + " ");
                        } else {
//                                        edit_msg.setText(edit_msg.getText() + "@" + from + " ");
                            mAtManager.insertAitMemberInner(from, from, edit_msg.getSelectionStart(), true);
                        }
                    });
                }
            }, false, false));
        }

//        }
        //????????????????????????????????????
        voice_prompt.setVisibility(View.GONE);

        //????????????listview??????adapter
        chat_region.setAdapter(pbChatViewAdapter);
        chat_region.getRefreshableView().setFastScrollEnabled(false);
        //??????????????????,
        chat_region.setOnScrollListener(new AbsListView.OnScrollListener() {
            //            ?????????ListView????????????????????????????????????????????????SCROLL_STATE_TOUCH_SCROLL???SCROLL_STATE_FLING???SCROLL_STATE_IDLE???
//            SCROLL_STATE_TOUCH_SCROLL??????????????????ListView??????
//            SCROLL_STATE_FLING???ListView???????????????
//            SCROLL_STATE_IDLE???ListView???????????????
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
//                    Glide.with(PbChatActivity.this).resumeRequests();
                    //??????????????????????????????????????????????????????????????????,??????????????????
                    if (atMsgIndex > 0 && chat_region.getRefreshableView().getFirstVisiblePosition() <= atMsgIndex - 1) {
                        clearAtmsgTip();
                    }
                    if (unreadMsgCount.intValue() > 0 &&
                            chat_region.getRefreshableView().getFirstVisiblePosition() <= pbChatViewAdapter.getCount() - unreadMsgCount.intValue() - 1) {
                        //todo:??????????????????
                        clearUnread();
                    }
                    //??????????????????textview?????????, ????????????????????????
                    if (chat_region.getRefreshableView().getLastVisiblePosition() == chat_region.getRefreshableView().getCount() - 1) {
                        clearAtmsgTip();
                    }
                } else if (/*scrollState == SCROLL_STATE_TOUCH_SCROLL
                        || */scrollState == SCROLL_STATE_FLING) {
                    //??????????????????????????????????????????????????????????????????
//                    Glide.with(PbChatActivity.this).pauseRequests();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //?????????????????? listview???????????????????????????textview
                if (firstVisibleItem + visibleItemCount == totalItemCount) {
                    new_msg_prompt.setVisibility(View.GONE);
//                    clearNewUnRead();
                }

            }
        });

        //?????????????????????
        chat_region.setOnRefreshListener(onRefresh);

        waterMarkTextUtil.setWaterMarkTextBg(chat_region, this);

    }

    //?????????ActionBar
    @Override
    public void initActionBar() {
        //????????????image????????????

//        myActionBar.getRightImageBtn().setVisibility(View.VISIBLE);
        //?????????????????????????????????
        if (isFinishing()) {
            return;
        }
        if (isFromChatRoom) {
            //??????????????????????????????
            setActionBarRightIcon(R.string.atom_ui_new_more);
            connectionUtil.getMucCard(getToId(), nick -> runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (nick != null) {
                    if (TextUtils.isEmpty(nick.getMark())) {
                        setActionBarTitle(nick.getName());
                    } else {
                        setActionBarTitle(nick.getMark());
                    }

                } else {
                    setActionBarTitle(getToId());
                }
            }), false, false);


        } else {
            //??????????????????????????????
            setActionBarRightIcon(R.string.atom_ui_new_more);
            String str = "";
            if ((ConversitionType.MSG_TYPE_CONSULT + "").equals(chatType)) {
                str = getToId();
            } else {
                str = getRealJid();
            }
            connectionUtil.getUserCard(str, nick -> runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (nick == null) {
                    setActionBarTitle(getToId());
                } else {
                    if (TextUtils.isEmpty(nick.getMark())) {
                        setActionBarTitle(nick.getName());
                    } else {
                        setActionBarTitle(nick.getMark());
                    }
                    setActionBarMood(Html.fromHtml(nick.getMood()).toString());

                }
            }), false, false);
            if ("5".equals(chatType) &&
                    (/*(CommonConfig.isQtalk && connectionUtil.isHotlineMerchant(jid))
                            || */!CommonConfig.isQtalk && CurrentPreference.getInstance().isMerchants())) {
                setActionBarRightSpecial(R.string.atom_ui_new_hungup);
                setActionBarRightIconSpecialClick(v -> {
                    Logger.i("??????????????????  ");
                    showConfirmHungupDialog();
                });
            }
        }

        if (!isRemind) {
            setActionBarTitleRightImage(R.drawable.atom_ui_no_remind);
        }


        //???????????????????????????
//       boolean showRed = DataUtils.getInstance(this).getPreferences("rightRed",true);
//        if(showRed){
//            BadgeHelper badgeHelperC = new BadgeHelper(this)
//                    .setBadgeType(BadgeHelper.Type.TYPE_POINT)
//                    .setBadgeCenterVertical()
//                    .setBadgeOverlap(false);
//            badgeHelperC.bindToTargetView(qtNewActionBar.getRightIcon());

//        }


        //??????????????????,???????????????Activity,???????????????????????????
        //????????????,????????????????????????jid ??? ???????????????
        setActionBarRightIconClick(v -> {
            DataUtils.getInstance(PbChatActivity.this).putPreferences("rightRed", false);
            if (isFromChatRoom) {
                if (QtalkNavicationService.getInstance().getNavConfigResult().RNAndroidAbility.RNGroupCardView) {
                    NativeApi.openGroupChatInfo(jid);
                } else {
                    Intent intent = new Intent(PbChatActivity.this, ChatroomMembersActivity.class);
                    intent.putExtra(KEY_JID, jid);
                    intent.putExtra(KEY_REAL_JID, realJid);
                    intent.putExtra(KEY_CHAT_TYPE, chatType);
                    intent.putExtra(KEY_IS_CHATROOM, isFromChatRoom);
                    startActivityForResult(intent, UPDATE_NICK);
                }
            } else {

                Intent intent2 = new Intent("com.broadcasereceiver.PdChatIMsgSendReceiver");
                intent2.putExtra("chatjid", jid);         //??????????????????????????????
                intent2.putExtra("type", "onChatRoomClick");         //??????????????????????????????
                intent2.putExtra("isFromChatRoom", isFromChatRoom);         //??????????????????????????????
                sendBroadcast(intent2);      //????????????
//                if (QtalkNavicationService.getInstance().getNavConfigResult().RNAndroidAbility.RNUserCardView) {
//                    NativeApi.openSingleChatInfo(jid, realJid);
//                } else {
//                    Intent intent = new Intent(PbChatActivity.this, ChatroomMembersActivity.class);
//                    intent.putExtra(KEY_JID, jid);
//                    intent.putExtra(KEY_REAL_JID, realJid);
//                    intent.putExtra(KEY_CHAT_TYPE, chatType);
//                    intent.putExtra(KEY_IS_CHATROOM, isFromChatRoom);
//                    startActivityForResult(intent, UPDATE_NICK);
//                }

            }


        });
//        setActionBarRightSpecial(R.string.atom_ui_new_search);
//        setActionBarRightIconSpecialClick(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                com.qunar.im.base.protocol.NativeApi.openLocalSearch(jid,realJid,chatType);
//            }
//        });
    }


    //    @Override
//    public void onActivityReenter(int resultCode, Intent data) {
//        super.onActivityReenter(resultCode, data);
//        if(resultCode==RESULT_OK){
//            pbChatViewAdapter.notifyDataSetChanged();
//        }
//    }

    //?????????????????????
    public void initInputRegion() {
        //???????????????????????????
        record.setVisibility(View.GONE);
        //???????????????layout??????
        input_container.setVisibility(View.VISIBLE);
        //????????????????????????????????????
        left_btn.setVisibility(View.GONE);
        //???????????????????????????
        voice_switch_btn.setVisibility(View.VISIBLE);
        //??????
        String draft = InternDatas.getDraft(QtalkStringUtils.parseBareJid(jid) + QtalkStringUtils.parseBareJid(realJid));
        if (!TextUtils.isEmpty(draft))
            edit_msg.setText(draft);
        //edtext????????????
        edit_msg.clearFocus();
        //edtext ?????? ??????????????????,??????????????????????????????
        edit_msg.setOnEditorActionListener((v, actionId, event) -> {
            //???id???????????????
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                //????????????
                sendMessage();
                return true;//????????????
            }
            return false;
        });
        //????????????????????????
//        edit_msg.removeTextChangedListener();
        edit_msg.removeTextChangedListener(textWatcher);
        edit_msg.addTextChangedListener(textWatcher);
        //todo: ??????????????????????????????,???????????????????????????,???????????????
        //todo: ????????????
        //?????????????????????
        initGridView();
//        //?????????imagepicker
//        initImagePicker();
        //???????????????
        initEmoticon();
        // ?????????????????????
        initVoiceView();
        //?????????????????????
        initQuickReply();
    }


    /**
     * ?????????????????????
     */
    private void initVoiceView() {
        final File file = MyDiskCache.getVoiceFile(MyDiskCache.TEMP_VOICE_FILE_NAME);
        /**
         * ???????????????????????????????????????
         */
        record.initView(file.getAbsolutePath());
        record.setCallBack(new RecordView.IRecordCallBack() {

            @Override
            public void recordStart() {
//                originalVal = CurrentPreference.getInstance().isTurnOnMsgSound();
//                CurrentPreference.getInstance().setTurnOnMsgSound(false);
            }

            @Override
            public void recordFinish(long duration) {
//                CurrentPreference.getInstance().setTurnOnMsgSound(originalVal);
                chatingPresenter.sendVoiceMessage(file.getAbsolutePath(), (int) (duration / 1000));

            }

            @Override
            public void recordCancel() {

            }

            boolean originalVal = true;
        });
        record.setStatusView(voice_prompt, getHandler());
    }

    /**
     * ???????????????
     */
    private void initEmoticon() {
        if (!faceView.isInitialize()) {
            faceView.setDefaultOnEmoticionsClickListener(new DefaultOnEmoticionsClickListener());
            faceView.setOthersOnEmoricionsClickListener(new ExtentionEmoticionsClickListener());
            faceView.setAddFavoriteEmoticonClickListener(new OnAddFavoriteEmoticonClickListener());
            faceView.setFavoriteEmojiconOnClickListener(new FavoriteEmoticonOnClickListener());
            faceView.setDeleteImageViewOnClickListener(v -> {
                int keyCode = KeyEvent.KEYCODE_DEL;  //??????????????????s
                KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                KeyEvent keyEventUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                edit_msg.onKeyDown(keyCode, keyEventDown);
                edit_msg.onKeyUp(keyCode, keyEventUp);
            });
            faceView.initFaceGridView(EmotionUtils.getExtEmotionsMap(this, false), EmotionUtils.getDefaultEmotion(this), getFavoriteMap(this));
        }

        //????????????
        EmotionUtils.getDefaultEmotion(this.getApplicationContext());
        EmotionUtils.getExtEmotionsMap(this.getApplicationContext(), false);
    }

    /**
     * ????????????
     */
    private void initQuickReply() {
        if (CommonConfig.isQtalk) {
            return;
        }
        if (!quickReplyLayout.isInitialize()) {
            quickReplyLayout.setOnQuickReplyClickListenter(content -> {
                edit_msg.setText(content);
                chatingPresenter.sendMsg();
                edit_msg.setText("");
            });
            quickReplyLayout.initFaceGridView(QuickReplyUtils.quickRepliesMerchant);
        }
    }

    /*
     * ??????????????????????????????????????????Map???
     * ??????????????????????????????????????????????????????????????????,???????????????????????????????????????,??????EmoticonEntity????????????
     * */
    public static final String FAVORITE_ID = "favorite";

    public static EmoticionMap getFavoriteMap(Context context) {
//        File dir = EMOJICON_DIR;//new File(context.getFilesDir(), Constants.SYS.EMOTICON_FAVORITE_DIR);
        UserConfigData userConfigData = new UserConfigData();
        userConfigData.setKey(CacheDataType.kCollectionCacheKey);
        List<UserConfigData> list = ConnectionUtil.getInstance().selectUserConfigValueInString(userConfigData);
        EmoticionMap map = new EmoticionMap("0", list.size() + 1, 0, 0, "");
        EmoticonEntity entity = new EmoticonEntity();
        entity.id = FAVORITE_ID;
        map.pusEntity(entity.id, entity);
        try {
            for (UserConfigData f : list) {
                EmoticonEntity tmpEntity = new EmoticonEntity();
                tmpEntity.fileFiexd = f.getValue();
                tmpEntity.fileOrg = f.getValue();
                map.pusEntity(f.getSubkey(), tmpEntity);
            }
        } catch (Exception e) {
            Logger.e("getFavoriteMap:" + e.getMessage());
        }
        //?????????????????? 8*2
        map.showAll = 0;
        map.packgeId = FAVORITE_ID;
        return map;
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    private FuncMap initGridData() {
        FuncMap funcMap = new FuncMap();

        FuncItem item = new FuncItem();
        item.id = FuncMap.PHOTO;
        item.icon = "res:///" + R.drawable.atom_ui_sharemore_picture;
        item.textId = getString(R.string.atom_ui_function_photo);
        item.hanlder = () -> {
            checkShowGallary();
            saveChatWindowActLog(FuncMap.PHOTO,"??????","???????????????-????????????");
        };
        funcMap.regisger(item);

        item = new FuncItem();
        item.id = FuncMap.CAMERA;
        item.icon = "res:///" + R.drawable.atom_ui_sharemore_camera;
        item.textId = getString(R.string.atom_ui_user_camera);
        item.hanlder = () -> {
            checkShowCamera();
            saveChatWindowActLog(FuncMap.CAMERA,"??????","???????????????-????????????");
        };
        funcMap.regisger(item);
        if (CurrentPreference.getInstance().isMerchants() && GlobalConfigManager.isQchatPlat()
                && String.valueOf(ConversitionType.MSG_TYPE_CONSULT_SERVER).equals(chatType)) {
            item = new FuncItem();
            item.id = FuncMap.QUICKREPLY;
            item.icon = "res:///" + R.drawable.atom_ui_quickreply;
            item.textId = getString(R.string.atom_ui_function_quickreply);
            item.hanlder = () -> {
                quickReplySwitch();
                saveChatWindowActLog(FuncMap.QUICKREPLY,"????????????","???????????????-????????????");
            };
            funcMap.regisger(item);
        }

        item = new FuncItem();
        item.id = FuncMap.FILE;
        item.icon = "res:///" + R.drawable.atom_ui_sharemore_file;
        item.textId = getString(R.string.atom_ui_function_file);
        item.hanlder = () -> {
            PermissionDispatcher.
                    requestPermissionWithCheck(PbChatActivity.this, new int[]{
                                    PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE,
                                    PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE}, PbChatActivity.this,
                            READ_FILE);
            saveChatWindowActLog(FuncMap.FILE,"??????","???????????????-????????????");
        };
        funcMap.regisger(item);

        item = new FuncItem();
        item.id = FuncMap.LOCATION;
        item.icon = "res:///" + R.drawable.atom_ui_ic_sharelocation;
        item.textId = getString(R.string.atom_ui_function_location);
        item.hanlder = () -> {
            PermissionDispatcher.
                    requestPermissionWithCheck(PbChatActivity.this, new int[]{
                                    PermissionDispatcher.REQUEST_ACCESS_COARSE_LOCATION,
                                    PermissionDispatcher.REQUEST_ACCESS_FINE_LOCATION}, PbChatActivity.this,
                            READ_LOCATION);
            saveChatWindowActLog(FuncMap.LOCATION,"????????????","???????????????-????????????");
        };
        funcMap.regisger(item);

        item = new FuncItem();
        item.id = FuncMap.VIDEO;
        item.icon = "res:///" + R.drawable.atom_ui_icon_send_video;
        item.textId = getString(R.string.atom_ui_function_video);
        item.hanlder = () -> {
            chooseVideoSource();
            saveChatWindowActLog(FuncMap.VIDEO,"????????????","???????????????-????????????");
        };
        funcMap.regisger(item);
        //????????????
//        if (CommonConfig.fireafterread) {
//            item = new FuncItem();
//            item.id = FuncMap.FIREAFTERREAD;
//            item.icon = "res:///" + R.drawable.atom_ui_ic_fire_msg;
//            item.textId = getString(R.string.atom_ui_textbar_button_burn_after_reading);
//            item.hanlder = new FuncHanlder() {
//                @Override
//                public void handelClick() {
////                if(isEncrypt()){
////                    Toast.makeText(PbChatActivity.this, "?????????????????????????????????", Toast.LENGTH_SHORT).show();
////                    return;
////                }
//                    isSnapMsg = !isSnapMsg;
//                    if (!isSnapMsg) {
//                        edit_msg.setCompoundDrawables(null, null, null, null);
//                    } else {
//                        Drawable drawable = getResources().getDrawable(R.drawable.atom_ui_ic_fire);
//                        drawable.setBounds(0, 0, 48, 48);
//                        edit_msg.setCompoundDrawables(drawable, null, null, null);
//                    }
//                    ((ISnapPresenter) chatingPresenter).changeSnapStatus(isSnapMsg);
//                }
//            };
//            funcMap.regisger(item);
//        }

        //qchat?????????????????? ??????????????????chattype???5
        if (!isFromChatRoom &&
                String.valueOf(ConversitionType.MSG_TYPE_CONSULT_SERVER).equals(chatType)) {
            item = new FuncItem();
            item.id = FuncMap.TRANSFER;
            item.icon = "res:///" + R.drawable.atom_ui_transfer;
            item.textId = getString(R.string.atom_ui_button_transfer_tip);
            item.hanlder = () -> {
                transferConversation();
                saveChatWindowActLog(FuncMap.TRANSFER,"????????????","???????????????-????????????");
            };
            funcMap.regisger(item);
        }
//        if (!GlobalConfigManager.isStartalkPlat()) {
//
//        }
        //??????
        item = new FuncItem();
        item.id = FuncMap.HONGBAO;
        item.icon = "res:///" + R.drawable.atom_ui_ic_lucky_money;
        item.textId = getString(R.string.atom_ui_textbar_button_red_package);
        item.hanlder = () -> {
            giveLuckyMoney(false);
//                chooseRtcType();
            saveChatWindowActLog(FuncMap.HONGBAO,"??????","???????????????-????????????");
        };
        funcMap.regisger(item);

//        if (!GlobalConfigManager.isStartalkPlat()) {
//
//        }
        //aa??????
        if (!TextUtils.isEmpty(QtalkNavicationService.AA_PAY_URL) && CommonConfig.isQtalk) {
            item = new FuncItem();
            item.id = FuncMap.AA;
            item.icon = "res:///" + R.drawable.atom_ui_ic_aa_pay_black;
            item.textId = getString(R.string.atom_ui_textbar_button_aa);
            item.hanlder = () -> {
                giveLuckyMoney(true);
                saveChatWindowActLog(FuncMap.AA,"AA??????","???????????????-????????????");
            };
            funcMap.regisger(item);
        }

        //????????????
        if (!isFromChatRoom) {
            item = new FuncItem();
            item.id = FuncMap.Shock;
            item.icon = "res:///" + R.drawable.atom_ui_icon_shake_window;
            item.textId = getString(R.string.atom_ui_textbar_button_shake);
            item.hanlder = () -> {
                shake();
                saveChatWindowActLog(FuncMap.Shock,"????????????","???????????????-????????????");
            };
            funcMap.regisger(item);
        }
        //????????????
        if (!isFromChatRoom && CommonConfig.showVideoCommunication) {
            item = new FuncItem();
            item.id = FuncMap.VIDEO_CALL;
            item.icon = "res:///" + R.drawable.atom_ui_video;
            item.textId = getString(R.string.atom_ui_function_video_call);
            item.hanlder = () -> {
                chooseRtcType();
                saveChatWindowActLog(FuncMap.VIDEO_CALL,"????????????","???????????????-????????????");
            };
            funcMap.regisger(item);
        }

        if (!GlobalConfigManager.isQchatPlat() && !isFromChatRoom) {
            item = new FuncItem();
            item.id = FuncMap.ENCRYPT;
            item.icon = "res:///" + R.drawable.atom_ui_box_key;
            item.textId = getString(R.string.atom_ui_function_encrypt);
            item.hanlder = () -> {
                if (!isSnapMsg) {//??????????????????????????????
                    encryptConversation();
                    saveChatWindowActLog(FuncMap.ENCRYPT,"????????????","???????????????-????????????");
                }
            };
            funcMap.regisger(item);
        }

        if (GlobalConfigManager.isQtalkPlat() && isFromChatRoom()) {
            item = new FuncItem();
            item.id = FuncMap.ACTIVITY;
            item.icon = "res:///" + R.drawable.atom_ui_send_activity;
            item.textId = getString(R.string.atom_ui_send_activity);
            item.hanlder = () -> {
                sendActivity();
                saveChatWindowActLog(FuncMap.ACTIVITY,"??????","???????????????-????????????");

            };
            funcMap.regisger(item);
        }

        return funcMap;
    }

    /**
     * ?????????????????????
     */
    protected void initGridView() {
        FuncMap defaultFunMap = initGridData();
        int SUPPORT = 0;
        if (chatType == null) {
            chatType = String.valueOf(ConversitionType.MSG_TYPE_CHAT);
        }
        switch (chatType) {
            case ConversitionType.MSG_TYPE_CHAT + "":
                SUPPORT = 1;
                break;
            case ConversitionType.MSG_TYPE_GROUP + "":
                SUPPORT = 2;
                break;
            case ConversitionType.MSG_TYPE_CONSULT + "":
                SUPPORT = 8;
                break;
            case ConversitionType.MSG_TYPE_CONSULT_SERVER + "":
                SUPPORT = 16;
                break;
            case ConversitionType.MSG_TYPE_SUBSCRIPT + "":
                SUPPORT = 32;
                break;
        }
        if (isRobot() || isOtherManager() || isSelf()) {
            SUPPORT = 4;
        }
        int SCOPE = CurrentPreference.getInstance().isMerchants() ? 2 : 1;
        CapabilityResult capabilityResult = CapabilityUtil.getInstance().getCurrentCapabilityData();
        Logger.i("initGridView:" + SUPPORT + SCOPE + JsonUtils.getGson().toJson(capabilityResult));
        if (capabilityResult != null && capabilityResult.trdextendmsg != null) {
            funcMap.clear();
            for (final FuncButtonDesc funcButtonDesc : capabilityResult.trdextendmsg) {
                if ((funcButtonDesc.support & SUPPORT) == SUPPORT
                        && (funcButtonDesc.scope & SCOPE) == SCOPE) {

                    final FuncItem item = new FuncItem();
                    item.id = funcButtonDesc.trdextendId;//tmap.genNewId();
                    item.icon = funcButtonDesc.icon;
                    item.textId = funcButtonDesc.title;
                    if (funcButtonDesc.linkType == 0) {//?????????????????????
                        if (defaultFunMap.getItem(item.id) != null) {
                            item.hanlder = defaultFunMap.getItem(item.id).hanlder;
                        }
                    } else if (funcButtonDesc.linkType == 1) {
                        item.hanlder = () -> {
                            if (TextUtils.isEmpty(CurrentPreference.getInstance().getVerifyKey()))
                                return;
                            StringBuilder builder = new StringBuilder(funcButtonDesc.linkurl);
                            if (builder.indexOf("?") > -1) {
                                builder.append("&");
                            } else {
                                builder.append("?");
                            }
                            builder.append("username=");
                            builder.append(CurrentPreference.getInstance().getUserid());
                            builder.append("&rk=");
                            builder.append(CurrentPreference.getInstance().getVerifyKey());
                            if(chatType.equals(String.valueOf(ConversitionType.MSG_TYPE_CONSULT_SERVER))){
                                builder.append("&qchatid=5&type=consult");
                                builder.append("&user_id=");
                                builder.append(getToId());
                                builder.append("&realfrom=");
                                builder.append(CurrentPreference.getInstance().getPreferenceUserId());
                                builder.append("&realto=");
                                builder.append(getRealJid());
                            }else if(chatType.equals(String.valueOf(ConversitionType.MSG_TYPE_GROUP))){
                                builder.append("&group_id=");
                                builder.append(getToId());
                                builder.append("&type=groupchat");
                            }else {
                                builder.append("&user_id=");
                                builder.append(getToId());
                                builder.append("&type=chat");
                            }
                            builder.append("&company=");
                            builder.append(QtalkNavicationService.COMPANY);
                            builder.append("&domain=");
                            builder.append(QtalkNavicationService.getInstance().getXmppdomain());
                            builder.append("&q_d=");
                            builder.append(QtalkNavicationService.getInstance().getXmppdomain());
                            Intent intent = new Intent(PbChatActivity.this, QunarWebActvity.class);
                            intent.setData(Uri.parse(builder.toString()));
                            intent.putExtra(WebMsgActivity.IS_HIDE_BAR, true);
                            startActivity(intent);
                            saveChatWindowActLog(item.id,item.textId,"???????????????-????????????");
                        };
                    } else if (funcButtonDesc.linkType == 2) {//request
                        item.hanlder = () -> {
                            try {
                                JSONObject requestBody = new JSONObject();
                                requestBody.put("from", getFromId());
                                requestBody.put("to", getToId());
                                requestBody.put(KEY_REAL_JID, getRealJid());
                                requestBody.put(KEY_CHAT_TYPE, chatType);
                                HttpUtil.sendCapabilityRequest(funcButtonDesc.linkurl, requestBody);
                            } catch (JSONException e) {
                                Logger.i(e.getMessage());
                            }
                            saveChatWindowActLog(item.id,item.textId,"???????????????-????????????");
                        };
                    } else if (funcButtonDesc.linkType == 4) {//schema
                        item.hanlder = () -> {
                            Intent i = new Intent("android.intent.action.VIEW", Uri.parse(funcButtonDesc.linkurl));
                            startActivity(i);
                        };
                        saveChatWindowActLog(item.id, item.textId, "???????????????-????????????");
                    }
                    funcMap.regisger(item);
                }
            }
        } else {
            funcMap = defaultFunMap;
        }
        if (linearlayout_tab != null) {
            linearlayout_tab.init(this, funcMap);
        }
    }

    private void setTranscriptMode(boolean hasFocus) {
        chat_region.getRefreshableView().setTranscriptMode(hasFocus ? AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL : AbsListView.TRANSCRIPT_MODE_NORMAL);
    }

    /**
     * ????????????
     */
    protected void shake() {
        long time = Calendar.getInstance().getTimeInMillis();
        if (time - lastShakeTime >= 30000) {
            lastShakeTime = time;
            ((IShakeMessagePresenter) chatingPresenter).setShakeMessage();
            shakeWindow();
        } else {
            Toast.makeText(this, R.string.atom_ui_tip_too_frequent, Toast.LENGTH_LONG).show();
        }
    }

    //????????????
    public void sendMessage() {
        //??????????????????
        String str = edit_msg.getText().toString();
        //????????????????????????,
        if (TextUtils.isEmpty(str)) {
            //?????????????????????
            return;
        }

        //?????????
        String emop = EmoticonRainUtil.getEmoPath(str.toLowerCase());
        if (!TextUtils.isEmpty(emop))
            EmoticonRainUtil.startRain(emoticonRainView, emop);

        chatingPresenter.sendMsg();
//        //?????????????????????PBIMMessage????????????,????????????????????????????????????????????????
//       connectionUtil.sendTextOrEmojiMessage(str, jid, CurrentPreference.getInstance().getPreferenceUserId(), new ConnectionUtil.OnGetPBIMMessageSuccess() {
//           @Override
//           public void OnGetPBIMMessageSuccess(PBIMMessage pbimMessage) {
//               //??????????????????,????????????????????????,????????????????????????????????????
//               setNewMsg2DialogueRegion(pbimMessage);
//           }
//       });


        //?????????????????????,?????????edittext?????????""
        //???@??????????????????
        //??????????????????
        mAtManager.reset();
//        atMap.clear();
        edit_msg.setText("");

        //??????????????????
        if (!TextUtils.isEmpty(refrenceString)) {
            atom_ui_refence_text.setText("");
            atom_ui_refence_layout.setVisibility(View.GONE);
            refrenceString = "";
        }
    }

    //?????????????????????????????????
    public String getInputMsg() {
        return edit_msg.getText().toString();
    }

    public String getRefenceString() {
        return refrenceString;
    }

    @Override
    public void setInputMsg(String text) {
        edit_msg.setText(text);
        edit_msg.requestFocus();
        edit_msg.setSelection(text.length());
        KPSwitchConflictUtil.showKeyboard(mPanelRoot, edit_msg);
    }

    //???????????????????????????
    public void loadMoreHistory() {
        //todo:????????????????????????????????????,pb?????????????????????????????????????????????????????????event
        searchMoreCount++;
        if (searchMoreCount > 2 && !isShowSearch) {
            showSearchView();
            isShowSearch = true;
        }
        ((ICloudRecordPresenter) chatingPresenter).showMoreOldMsg(isFromChatRoom);
    }


    //??????????????????textview
    private void resetNewMsgCount() {
        if (chat_region.getRefreshableView().getCount() > 0) {
            //mListView.getRefreshableView().setSelection(mListView.getRefreshableView().getCount() - 1); //???vivo???????????????????????????????????????
            //???????????????????????????
            chat_region.getRefreshableView().smoothScrollToPosition(chat_region.getRefreshableView().getCount() - 1);
        }
        //????????????textview???????????????
        new_msg_prompt.setVisibility(View.GONE);
        //????????????????????????  ?????????0
        newMsgCount = 0;
    }

    /**
     * ??????View
     */
    protected void bindViews() {
        edit_region = (LinearLayout) findViewById(R.id.edit_region);
        left_btn = (IconView) findViewById(R.id.left_btn);
        voice_switch_btn = (IconView) findViewById(R.id.voice_switch_btn);
        voice_prompt = (TextView) findViewById(R.id.voice_prompt);
        input_container = (LinearLayout) findViewById(R.id.input_container);
        total_bottom_layout = (LinearLayout) findViewById(R.id.total_bottom_layout);
        atom_bottom_frame = (RelativeLayout) findViewById(R.id.atom_bottom_frame);
        edit_msg = (EmojiconEditText) findViewById(R.id.edit_msg);
        tv_options_btn = (IconView) findViewById(R.id.tv_options_btn);
        send_btn = (TextView) findViewById(R.id.send_btn);

        chating_view = (RelativeLayout) findViewById(R.id.chating_view);
        chat_region = (com.handmark.pulltorefresh.library.PullToRefreshListView) findViewById(R.id.chat_region);
        new_msg_prompt = (TextView) findViewById(R.id.new_msg_prompt);//???????????????????????????
        emotion_btn = (IconView) findViewById(R.id.tv_emojicon);
        outter_msg_prompt = (LinearLayout) findViewById(R.id.outter_msg_prompt);
        outter_msg = (TextView) findViewById(R.id.outter_msg);

        emoticonRainView = (EmoticonRainView) findViewById(R.id.emoticonRainView);


        no_prompt = (TextView) findViewById(R.id.no_prompt);
        close_prompt = (TextView) findViewById(R.id.close_prompt);
        relativeLayout = (KPSwitchRootLinearLayout) findViewById(R.id.resizelayout);
//        line = findViewById(line);
        new_msg_prompt.setOnClickListener(this);

        tv_options_btn.setOnClickListener(this);
        left_btn.setOnClickListener(this);
        voice_switch_btn.setOnClickListener(this);
        send_btn.setOnClickListener(this);
        emotion_btn.setOnClickListener(this);
        edit_msg.setOnFocusChangeListener(this);
        no_prompt.setOnClickListener(this);
        close_prompt.setOnClickListener(this);
        outter_msg.setOnClickListener(this);
        chating_view.setOnClickListener(this);
        qtNewActionBar = (QtNewActionBar) this.findViewById(R.id.my_action_bar);
        setNewActionBar(qtNewActionBar);

        mPanelRoot = (KPSwitchPanelLinearLayout) findViewById(R.id.panel_root);
        linearlayout_tab = (OperationView) findViewById(R.id.linearlayout_tab);
        record = (RecordView) findViewById(R.id.record);
        linearlayout_tab2 = (LinearLayout) findViewById(R.id.linearlayout_tab2);
        atom_bottom_more = (LinearLayout) findViewById(R.id.atom_bottom_more);
        faceView = (EmotionLayout) findViewById(R.id.faceView);
        quickreply_tab = (LinearLayout) findViewById(R.id.quickreply_tab);
        quickReplyLayout = (QuickReplyLayout) findViewById(R.id.quickreplyView);

        shareMessgeBtn = (ImageView) findViewById(R.id.txt_share_message);
        deleteMessageBtn = (ImageView) findViewById(R.id.txt_del_msgs);
        collectMsgBtn = (ImageView) findViewById(R.id.txt_collect_msg);
        emailMsgBtn = (ImageView) findViewById(R.id.txt_email_msg);

        atom_ui_refence_layout = (LinearLayout) findViewById(R.id.atom_ui_refence_layout);
        atom_ui_refence_text = (TextView) findViewById(R.id.atom_ui_refence_text);
        atom_ui_refence_close = (ImageView) findViewById(R.id.atom_ui_refence_close);
        atom_ui_refence_close.setOnClickListener((view)-> {
            refrenceString = "";
            atom_ui_refence_text.setText("");
            atom_ui_refence_layout.setVisibility(View.GONE);
        });

        shareMessgeBtn.setOnClickListener(this);
        deleteMessageBtn.setOnClickListener(this);
        emailMsgBtn.setOnClickListener(this);
        collectMsgBtn.setOnClickListener(this);

        //+??????????????????????????????????????????
        linearlayout_tab.setVisibility(View.GONE);
        //+??????????????????????????????????????????
        linearlayout_tab2.setVisibility(View.GONE);

        addEvents();


    }

    /**
     * ????????????
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        //??????????????????????????????switch????????????,??????if??????
        int i = v.getId();
        //??????????????????textview
        if (i == R.id.new_msg_prompt) {
            resetNewMsgCount();
            //?????????????????? ????????????
        } else if (i == R.id.send_btn) {
            sendMessage();
            //???????????????,?????????????????????
        } else if (i == R.id.chating_view) {
//            hintKbTwo();
            KPSwitchConflictUtil.hidePanelAndKeyboard(mPanelRoot);

        } else if (i == R.id.tv_options_btn) {
//            if(linearlayout_tab.getVisibility() == View.GONE)
//                showRecommendPop();
//            //??????+????????????
//            mOptionButtonClickListener();
            switchStatus(CHAT_STATUS_DEFAULT);
        } else if (i == R.id.left_btn) {
            //????????????
            switchStatus(CHAT_STATUS_DEFAULT);
            KPSwitchConflictUtil.showKeyboard(mPanelRoot, edit_msg);
//            switchButtonClickListener();
        } else if (i == R.id.voice_switch_btn) {
            //????????????
//            voice_switch_btnClickListener();
            switchStatus(CHAT_STATUS_VOICE);
        } else if (i == R.id.tv_emojicon) {
            //??????
//            mEmiticonButtonClickListener();
            switchStatus(CHAT_STATUS_EMOTION);
        } else if (i == R.id.txt_share_message) {
            if (pbChatViewAdapter.getSharingMsg().size() > 0) {
                Intent selUser = new Intent(this, SearchUserActivity.class);
                selUser.putExtra(Constants.BundleKey.IS_TRANS, true);
                selUser.putExtra(Constants.BundleKey.TRANS_MSG, "share");
                startActivity(selUser);
            }
        } else if (i == R.id.txt_collect_msg) {
            cancelMore();
        } else if (i == R.id.txt_del_msgs) {
            if (pbChatViewAdapter.getSharingMsg().size() == 0) return;
            selectedMessages.clear();
            selectedMessages.addAll(pbChatViewAdapter.getSharingMsg());
            chatingPresenter.deleteMessge();
            selectedMessages.clear();
            cancelMore();
        } else if (i == R.id.txt_email_msg) {
            if (pbChatViewAdapter.getSharingMsg().size() == 0) return;
            StringBuilder emailContent = new StringBuilder();
            for (IMMessage message : pbChatViewAdapter.getSharingMsg()) {
                String nick = ConnectionUtil.getInstance().getNickById(QtalkStringUtils.userId2Jid(message.getFromID())).getName();//ProfileUtils.getNickByKey(QtalkStringUtils.parseResource(message.getFromID()));
                String content = ChatTextHelper.showContentType(message.getBody(), message.getMsgType());
                emailContent.append(nick);
                emailContent.append(" ");
                emailContent.append(message.getTime().toString());
                emailContent.append("\n");
                emailContent.append(content);
                emailContent.append("\n");
            }
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.putExtra(Intent.EXTRA_SUBJECT, CommonConfig.isQtalk ? "qtalk" : "qchat" + "????????????");
            intent.putExtra(Intent.EXTRA_TEXT, emailContent.toString());
            startActivity(Intent.createChooser(intent, getString(R.string.atom_ui_common_email)));
        }
    }

    protected void cancelMore() {
        pbChatViewAdapter.changeShareStatus(false);
        pbChatViewAdapter.notifyDataSetChanged();
        atom_bottom_frame.setVisibility(View.VISIBLE);
        atom_bottom_more.setVisibility(View.GONE);
//        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) line.getLayoutParams();
//        layoutParams.addRule(RelativeLayout.ABOVE, R.id.atom_bottom_frame);
//        line.setLayoutParams(layoutParams);
    }

    /**
     * ??????????????????
     *
     * @param v
     * @param hasFocus
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {

    }

    /**
     * ????????????????????????
     *
     * @return
     */
    protected boolean isShowReadStateView() {
        return !isFromChatRoom && !isRobot() && !isOtherManager();
    }

    protected boolean isRobot() {
        String[] configs = getResources().getStringArray(R.array.atom_ui_robot_config);
        List<String> readStateConfig = Arrays.asList(configs);
        return readStateConfig != null && readStateConfig.contains(QtalkStringUtils.parseId(jid));
    }

    protected boolean isOtherManager() {
        String[] configs = getResources().getStringArray(R.array.atom_ui_otehr_manager);
        List<String> readStateConfig = Arrays.asList(configs);
        return readStateConfig != null && readStateConfig.contains(QtalkStringUtils.parseId(jid));
    }

    protected boolean isSelf() {
        return CurrentPreference.getInstance().getPreferenceUserId().equals(jid);
    }

    @Override
    public void setNewMsg2DialogueRegion(final IMMessage newMsg) {

        runOnUiThread(() -> {
            if(isFinishing()){
                return;
            }
            if (unreadMsgCount.intValue() > 0) {
                unreadMsgCount.incrementAndGet();
            }
            //?????????????????????????????????
            pbChatViewAdapter.addNewMsg(newMsg);
            //????????????????????????,???????????????
            if (newMsg.getDirection() == IMMessage.DIRECTION_RECV || newMsg.getDirection() == IMMessage.DIRECTION_SEND) {
                newMsgCount++;
            }
            //??????listview??????????????????
            if (chat_region.getRefreshableView().getCount() > 0) {
                //???????????????????????????
                if (newMsg.getDirection() == IMMessage.DIRECTION_SEND ||
                        //???????????????????????????
                        edit_msg.isFocused() ||
                        //??????????????????View?????????
                        (linearlayout_tab != null && linearlayout_tab.getVisibility() == View.VISIBLE) ||
                        //????????????????????????????????????5???
                        chat_region.getRefreshableView().getLastVisiblePosition() >= chat_region.getRefreshableView().getCount() - 5) {
                    //?????????listview???????????????
//                        chat_region.getRefreshableView().setSelection(chat_region.getRefreshableView().getCount() - 1);
                    chat_region.getRefreshableView().smoothScrollToPosition(chat_region.getRefreshableView().getCount() - 1);
                    //???????????????textview ??????????????????
                    new_msg_prompt.setVisibility(View.GONE);
                    //???????????????????????????0
                    newMsgCount = 0;
                } else {
                    //???????????????????????????,?????????????????????????????????
                    if (newMsg.getDirection() == IMMessage.DIRECTION_RECV || newMsg.getDirection() == IMMessage.DIRECTION_SEND) {
                        //?????? ????????????????????? ????????????
//                            showUnreadView(newMsgCount,true);
                        //???????????????textview???????????????
                        String msg = MessageFormat.format(getString(R.string.atom_ui_tip_new_msg_prompt), newMsgCount);
                        new_msg_prompt.setText(msg);
                        //???????????????textview???????????????
                        new_msg_prompt.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    @Override
    public void showUnReadCount(final int count) {
        if (!isFinishing())
            runOnUiThread(() -> setActionBarLeftCount(count));

    }

    @Override
    public String getAutoReply() {
        return autoReply;
    }

    @Override
    public String getOf() {
        return of;
    }

    @Override
    public String getOt() {
        return ot;
    }

    @Override
    public String getRealJid() {
        return realJid;
    }

    //???????????????????????????id
    @Override
    public String getFromId() {
        return CurrentPreference.getInstance().getPreferenceUserId();
    }

    @Override
    public String getFullName() {
        return null;
    }

    @Override
    public String getToId() {
        return jid;
    }

    @Override
    public String getChatType() {
        return chatType;
    }

    @Override
    public String getUserId() {
        return CurrentPreference.getInstance().getPreferenceUserId();
    }

    @Override
    public String getUploadImg() {
        return imageUrl;
    }

    @Override
    public String getTransferId() {
        return transferId;
    }

    @Override
    public List<IMMessage> getSelMessages() {
        return selectedMessages;
    }

    @Override
    public boolean isMessageExit(String msgId) {
        List<IMMessage> messages = pbChatViewAdapter.getMessages();
        if (messages == null || messages.size() == 0) {
            return false;
        } else {
            for (IMMessage imMessage : messages) {
                if (msgId.equals(imMessage.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void refreshDataset() {
        getHandler().post(() -> pbChatViewAdapter.notifyDataSetChanged());
    }

    @Override
    public void setCurrentStatus(String status) {

    }

    @Override
    public void setHistoryMessage(final List<IMMessage> historyMessage, final int unread) {
        runOnUiThread(() -> {
            if(isFinishing()){
                return;
            }
            unreadMsgCount.set(unread);
            chat_region.onRefreshComplete();
            if (historyMessage != null && historyMessage.size() > 0) {

                pbChatViewAdapter.setMessages(historyMessage);
                if (chat_region.getRefreshableView().getCount() > 0)
                chat_region.getRefreshableView().setSelection(chat_region.getRefreshableView().getCount() - 1);
            }
            // TODO: 2017/9/5 ???????????????
            handlerReceivedData();
            getAtOwnMsgIndexs();
            if (unread > 5) {
                showUnreadView(unread);
                if (isFromChatRoom && atMsgIndex > 0) {
                    showAtmsgView();
                }
            }
        });
//        getHandler().post(new Runnable() {
//            @TargetApi(Build.VERSION_CODES.M)
//            @Override
//            public void run() {
//
//            }
//        });
    }

    /**
     * ?????????????????????
     * isNew ??????????????????
     */
    @SuppressLint("ObjectAnimatorBinding")
    public void showUnreadView(int unread) {
        if (unread <= 0 || isFinishing()) {
            return;
        }

        ImageView tipImage = new ImageView(PbChatActivity.this);
        tipImage.setImageResource(R.drawable.atom_ui_chat_unread_tip);

        final TextView textView = new TextView(PbChatActivity.this);
        int padding = Utils.dipToPixels(PbChatActivity.this, 4);
        int size = Utils.dipToPixels(PbChatActivity.this, 30);
        int topMargin = Utils.dipToPixels(PbChatActivity.this, 30);
        final LinearLayout linearLayout = new LinearLayout(PbChatActivity.this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setBackgroundResource(R.drawable.atom_ui_float_tab);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                size);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.setMargins(0, topMargin, 0, 0);
        linearLayout.setPadding(padding * 2, padding, padding * 2, padding);
        linearLayout.setLayoutParams(layoutParams);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        textView.setTextColor(Color.parseColor("#666666"));

        String msg = unread + (String) getText(R.string.atom_ui_tip_unread_message);
        textView.setText(msg);
        textView.setPadding(padding*2, 0, 0, 0);
        textView.setOnClickListener((view) -> {
            chat_region.getRefreshableView().smoothScrollToPosition(pbChatViewAdapter.getCount() - unreadMsgCount.intValue() - 1);
            clearUnread();
        });
        linearLayout.addView(tipImage);
        linearLayout.addView(textView);
        linearLayout.setTag(TAG_UNREAD_VIEW);
        final LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, ObjectAnimator.ofFloat(this, "scaleX", 0, 1));
        getHandler().postDelayed(() -> {
            if(!isFinishing()){
                chating_view.setLayoutTransition(layoutTransition);
                chating_view.addView(linearLayout);
            }
        }, 500);
    }

    /**
     * ??????????????????
     */
    @SuppressLint("ObjectAnimatorBinding")
    private void showSearchView() {
        if (isFinishing()) {
            return;
        }
        final TipsFloatView tipsFloatView = new TipsFloatView(this);
        tipsFloatView.setTag(TAG_SEARCH_VIEW);
        tipsFloatView.setClickListener(v -> {
            Intent intent = ReflectUtil.getQtalkServiceRNActivityIntent(PbChatActivity.this);
            if(intent == null){
                return;
            }
            intent.putExtra("module", "Search");
            intent.putExtra("Version", "1.0.0");
            intent.putExtra("xmppid", jid);
            intent.putExtra("realjid", realJid);
            intent.putExtra("chatType", chatType);
            intent.putExtra("Screen", "LocalSearch");
            startActivity(intent);
            clearSearchTip();
        });

        final LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, ObjectAnimator.ofFloat(this, "scaleX", 0, 1));
        getHandler().postDelayed(() -> {
            if(!isFinishing()){
                chating_view.setLayoutTransition(layoutTransition);
                chating_view.addView(tipsFloatView);
            }
        }, 500);

    }

    /**
     * at????????????
     */
    @SuppressLint("ObjectAnimatorBinding")
    public void showAtmsgView() {
        if (isFinishing()) {
            return;
        }
        ImageView tipImage = new ImageView(PbChatActivity.this);
        tipImage.setImageResource(R.drawable.atom_ui_chat_unread_tip);

        final TextView textView = new TextView(PbChatActivity.this);
        int padding = Utils.dipToPixels(PbChatActivity.this, 4);
        int size = Utils.dipToPixels(PbChatActivity.this, 30);
        int topMargin = Utils.dipToPixels(PbChatActivity.this, 70);
        final LinearLayout linearLayout = new LinearLayout(PbChatActivity.this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER_VERTICAL);
        linearLayout.setBackgroundResource(R.drawable.atom_ui_float_tab);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                size);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams.setMargins(0, topMargin, 0, 0);
        linearLayout.setPadding(padding * 2, padding, padding * 2, padding);
        linearLayout.setLayoutParams(layoutParams);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        String msg = MessageFormat.format(getString(R.string.atom_ui_tip_somebody_at_you), atMsgIndexs.size());
        textView.setText(msg);
        textView.setTextColor(Color.parseColor("#EB524A"));
        textView.setPadding(padding * 2, 0, 0, 0);
        textView.setOnClickListener(v -> {
            chat_region.getRefreshableView().smoothScrollToPosition(atMsgIndex);
            clearAtmsgTip();
        });
        linearLayout.addView(tipImage);
        linearLayout.addView(textView);
        linearLayout.setTag(TAG_ATMSG_VIEW);
        final LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setAnimator(LayoutTransition.APPEARING, ObjectAnimator.ofFloat(this, "scaleX", 0, 1));
        getHandler().postDelayed(() -> {
            if (!isFinishing()) {
                chating_view.setLayoutTransition(layoutTransition);
                chating_view.addView(linearLayout);
            }
        }, 500);
    }

    protected void clearUnread() {
        unreadMsgCount.set(0);
        chating_view.setLayoutTransition(null);
        if (chating_view != null && chating_view.getChildCount() > 0) {
            for (int i = 0; i <= chating_view.getChildCount() - 1; i++) {
                View v = chating_view.getChildAt(i);
                if (TAG_UNREAD_VIEW.equals(v.getTag())) {
                    chating_view.removeView(v);
                    break;
                }
            }
        }
    }

    protected void clearNewUnRead() {
        newMsgCount = 0;
        chating_view.setLayoutTransition(null);
        if (chating_view != null && chating_view.getChildCount() > 0) {
            for (int i = 0; i <= chating_view.getChildCount() - 1; i++) {
                View v = chating_view.getChildAt(i);
                if (TAG_NEW_UNREAD_VIEW.equals(v.getTag())) {
                    chating_view.removeView(v);
                    break;
                }
            }
        }
    }

    protected View isHasShowNewMsgTip() {
        if (chating_view != null && chating_view.getChildCount() > 0) {
            for (int i = 0; i <= chating_view.getChildCount() - 1; i++) {
                View v = chating_view.getChildAt(i);
                if (TAG_NEW_UNREAD_VIEW.equals(v.getTag())) {
                    return v;
                }
            }
        }
        return null;
    }

    /**
     * ??????@??????
     */
    private void clearAtmsgTip() {
        if (!atMsgIndexs.isEmpty()) {
            atMsgIndexs.removeFirst();
        }
        if (atMsgIndexs.isEmpty()) {
            atMsgIndex = 0;
            chating_view.setLayoutTransition(null);
            if (chating_view != null && chating_view.getChildCount() > 0) {
                for (int i = 0; i <= chating_view.getChildCount() - 1; i++) {
                    View v = chating_view.getChildAt(i);
                    if (TAG_ATMSG_VIEW.equals(v.getTag())) {
                        chating_view.removeView(v);
                        break;
                    }
                }
            }
        } else {
            atMsgIndex = atMsgIndexs.getFirst();
        }

    }

    private void clearSearchTip() {
        chating_view.setLayoutTransition(null);
        if (chating_view != null && chating_view.getChildCount() > 0) {
            for (int i = 0; i <= chating_view.getChildCount() - 1; i++) {
                View v = chating_view.getChildAt(i);
                if (TAG_SEARCH_VIEW.equals(v.getTag())) {
                    chating_view.removeView(v);
                    break;
                }
            }
        }
    }


    @Override
    public void addHistoryMessage(final List<IMMessage> historyMessage) {
        getHandler().post(() -> {
            chat_region.onRefreshComplete();
            if (historyMessage == null || historyMessage.size() == 0) {
                return;
            }
            if (historyMessage.size() == 1) {
                if (TextUtils.isEmpty(historyMessage.get(0).getBody())) {
                    historyMessage.get(0).setBody(getString(R.string.atom_ui_cloud_record_prompt));
                    pbChatViewAdapter.addOldMsg(historyMessage);
                    return;
                }
                //??????????????????????????????????????????
                IMMessage firstMessage = pbChatViewAdapter.getFirstMessage();
                if (firstMessage != null && firstMessage.getMsgType() == MessageType.MSG_TYPE_NO_MORE_MESSAGE &&
                        historyMessage.get(0).getMsgType() == MessageType.MSG_TYPE_NO_MORE_MESSAGE) {
                    return;
                }
            }
            pbChatViewAdapter.addOldMsg(historyMessage);
            chat_region.getRefreshableView().setSelection(historyMessage.size());
        });
    }

    @Override
    public void addHistoryMessageLast(final List<IMMessage> historyMessage) {

    }

    @Override
    public void showNoticePopupWindow(NoticeBean noticeBean) {
        if (isFinishing()) {
            return;
        }
        super.showNoticePopupWindow(noticeBean);
    }

    public void setTitleState(String titleState) {
        if (isFinishing()) {
            return;
        }
        String title = mNewActionBar.getTextTitle().getText().toString();
        if (title.equals(titleState))
            return;
        titleTempVar = title;
        setActionBarTitle(titleState);
        getHandler().postDelayed(typingShow, 5000);

    }

    //???????????????????????????????????????
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v instanceof LinearLayout) {
            v.setTag(R.string.atom_ui_voice_hold_to_talk, "longclick");
            Object obj = v.getTag();
            if (obj == null) return;
            IMMessage message = (IMMessage) obj;
            if (message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeRedPack_VALUE)
                return;
            Intent intent = new Intent();
            intent.putExtra(Constants.BundleKey.MESSAGE, message);
            if (message.getDirection() == IMMessage.DIRECTION_SEND && MessageStatus.isExistStatus(message.getMessageState(), MessageStatus.LOCAL_STATUS_SUCCESS)) {
                menu.add(0, MENU8, 0, R.string.atom_ui_menu_revoke).setIntent(intent);
            }
            if (message.getMsgType() != ProtoMessageOuterClass.MessageType.MessageTypeBurnAfterRead_VALUE) {
                if (message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypePhoto_VALUE ||
                        message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeText_VALUE ||
                        message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeImageNew_VALUE) {
                    List<Map<String, String>> list = ChatTextHelper.getObjList(message.getBody());
                    if (list != null && list.size() == 1) {
                        Map<String, String> map = list.get(0);
                        if ("image".equals(map.get("type"))) {
                            String url = map.get("value");
                            url = QtalkStringUtils.addFilePath(url);
                            intent.putExtra(Constants.BundleKey.IMAGE_URL, url);
                            menu.add(0, MENU7, 0, getString(R.string.atom_ui_menu_add_stickers)).setIntent(intent);
                        }
                    }
                }
                if (message.getDirection() == IMMessage.DIRECTION_SEND && (message.getMessageState() == 0)//){
                        && (message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeText_VALUE)) {

                    menu.add(0, MENU2, 0, R.string.atom_ui_menu_resend).setIntent(intent);
                } else if (MessageStatus.isExistStatus(message.getMessageState(), MessageStatus.LOCAL_STATUS_SUCCESS)) {
                    menu.add(0, MENU1, 0, R.string.atom_ui_menu_translate).setIntent(intent);
                }
                if (message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeText_VALUE || message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeGroupAt_VALUE) {
                    menu.add(0, MENU5, 0, R.string.atom_ui_menu_copy).setIntent(intent);
                }
//                menu.add(0, MENU6, 0, "??????").setIntent(intent);
            }
            if (message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeText_VALUE
                    || message.getMsgType() == ProtoMessageOuterClass.MessageType.MessageTypeGroupAt_VALUE) {
                menu.add(0, MENU10, 0, R.string.atom_ui_menu_reference).setIntent(intent);
            }
            menu.add(0, MENU4, 0, R.string.atom_ui_common_delete).setIntent(intent);
            menu.add(0, MENU9, 0, R.string.atom_ui_common_more).setIntent(intent);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final IMMessage message = (IMMessage) item.getIntent().getSerializableExtra(Constants.BundleKey.MESSAGE);

        switch (item.getItemId()) {
            //??????
            case MENU1:
                KPSwitchConflictUtil.hidePanelAndKeyboard(mPanelRoot);
                Intent selUser = new Intent(this, SearchUserActivity.class);
                selUser.putExtra(Constants.BundleKey.IS_TRANS, true);
                selUser.putExtra(Constants.BundleKey.TRANS_MSG, message);
                startActivity(selUser);
                break;
            //??????
            case MENU2:
                if (message != null && !TextUtils.isEmpty(message.getBody())) {
                    selectedMessages.clear();
                    selectedMessages.add(message);
                    chatingPresenter.resendMessage();
                    selectedMessages.clear();
//                    Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.atom_ui_tip_resend_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            //??????
            case MENU4:
                //// TODO: 2017/9/27 ??????????????????????????????,??????????????????,??????????????????,???????????????????????????
                if (message != null && !TextUtils.isEmpty(message.getBody())) {
                    selectedMessages.clear();
                    selectedMessages.add(message);
                    chatingPresenter.deleteMessge();
                    selectedMessages.clear();
//                    Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.atom_ui_tip_delete_failed, Toast.LENGTH_SHORT).show();
                }


                break;
            //??????
            case MENU5:
                if (message != null && !TextUtils.isEmpty(message.getBody())) {
                    String content = ChatTextHelper.showContentType(message.getBody(), message.getMsgType());
                    Utils.dropIntoClipboard(content, this);
                    Toast.makeText(this, R.string.atom_ui_tip_copied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.atom_ui_tip_copy_no_content, Toast.LENGTH_SHORT).show();
                }
                break;
            //??????
            case MENU6:

                break;
            //???????????????
            case MENU7:
                imageUrl = item.getIntent().getStringExtra(Constants.BundleKey.IMAGE_URL);
                ((IAddEmojiconPresenter) chatingPresenter).addEmojicon();
                imageUrl = null;
                //???????????????????????????
//                faceView.resetFavoriteTab();
                break;
            //??????
            case MENU8:
                selectedMessages.clear();
                selectedMessages.add(message);
                chatingPresenter.revoke();
                selectedMessages.clear();
//                Toast.makeText(this,"???????????????"+message.getNick().getName(),Toast.LENGTH_LONG).show();
                break;
            //??????
            case MENU9:
                KPSwitchConflictUtil.hidePanelAndKeyboard(mPanelRoot);
                atom_bottom_frame.setVisibility(View.GONE);
                atom_bottom_more.setVisibility(View.VISIBLE);
                pbChatViewAdapter.changeShareStatus(true);
                pbChatViewAdapter.notifyDataSetChanged();
                break;
            //????????????
            case MENU10:
                connectionUtil.getUserCard(message.getFromID(), nick -> runOnUiThread(() -> {
                    String s = "";
                    String time = DateTimeUtils.getTime(message.getTime().getTime(), true, true) + "\n";
                    List<Map<String, String>> list = ChatTextHelper.getObjList(message.getBody());
                    if (ListUtil.isEmpty(list)) {
                        return;
                    } else {
                        for (Map<String, String> map : list) {
                            switch (map.get("type")) {
                                case "image":
                                    s += "????????????";
                                    break;
                                case "emoticon":
                                    s += "????????????";
                                    break;
                                case "url":
                                    s += "????????????";
                                    break;
                                case "text":
                                    s += map.get("value");
                                    break;
                            }
                        }
                    }
                    if (nick != null && !TextUtils.isEmpty(nick.getName())) {
                        s = time + "???" + nick.getName() + "???" + s + "???";
                        refrenceString = time + "???" + nick.getName() + "???" + message.getBody() + "???" + "\n" + "- - - - - - - - - - - - - - -" + "\n";
                    } else {
                        s = time + "???" + message.getFromID() + "???" + s + "???";
                        refrenceString = time + "???" + message.getFromID() + "???" + message.getBody() + "???" + "\n" + "- - - - - - - - - - - - - - -" + "\n";
                    }

                    edit_msg.setFocusable(true);
                    edit_msg.setFocusableInTouchMode(true);
                    edit_msg.requestFocus();

                    atom_ui_refence_layout.setVisibility(View.VISIBLE);
                    atom_ui_refence_text.setText(s);
                }), false, false);
                break;

        }
        return true;
    }

    @Override
    public void isEmotionAdd(boolean flag) {
        if (flag) {
            runOnUiThread(() -> faceView.resetFavoriteEmotion(getFavoriteMap(PbChatActivity.this)));
        }

        toast(flag ? "??????????????????" : "??????????????????");
    }

    //????????????
    @Override
    public void revokeItem(IMMessage imMessage) {
        if (isFinishing()) {
            return;
        }
        pbChatViewAdapter.replaceItem(imMessage);
    }

    @Override
    public void deleteItem(IMMessage imMessage) {
        pbChatViewAdapter.deleteItem(imMessage);
    }

    @Override
    public boolean isFromChatRoom() {
        return isFromChatRoom;
    }

    @Override
    public void replaceItem(IMMessage imMessage) {
        if (isFinishing()) {
            return;
        }
        pbChatViewAdapter.replaceItem(imMessage);
    }

    @Override
    public void sendEditPic(String path) {
        imageUrl = path;
//        imageUrl = edit.mPicturePath;
        chatingPresenter.sendImage();

    }

    @Override
    public Map<String, String> getAtList() {
        return mAtManager.getAtBlocks();
//        return atMap;
    }

    @Override
    public void clearAndAddMsgs(final List<IMMessage> historyMessage, final int unread) {
        Logger.i("??????:clearAndAddMsgs");
        getHandler().post(() -> {
            if (isFinishing()) {
                return;
            }
            unreadMsgCount.set(unread);
            chat_region.onRefreshComplete();
            if (historyMessage != null) {

                pbChatViewAdapter.clearAndAddMsgs(historyMessage);
                if (chat_region.getRefreshableView().getCount() > 0)
                    chat_region.getRefreshableView().setSelection(chat_region.getRefreshableView().getCount() - 1);
            }
            // TODO: 2017/9/5 ???????????????
            handlerReceivedData();
            if (unread > 5) {
                showUnreadView(unread);
            }
        });

        setReadState();

    }

    public void handlerReceivedData() {
        Intent shareIntent = getIntent();

        boolean fromShare = shareIntent.getBooleanExtra(Constants.BundleKey.IS_FROM_SHARE, false);
        boolean isMultiImg = shareIntent.getBooleanExtra(Constants.BundleKey.IS_TRANS_MULTI_IMG, false);
        Logger.i("??????:fromShare" + fromShare);
        if (fromShare) {
            if (shareIntent.getExtras() != null && shareIntent.getBooleanExtra(ShareReceiver.SHARE_TAG, false)) {
                String content = shareIntent.getStringExtra(ShareReceiver.SHARE_TEXT);
                ArrayList<String> icons = shareIntent.getStringArrayListExtra(ShareReceiver.SHARE_IMG);
                ArrayList<String> videos = shareIntent.getStringArrayListExtra(ShareReceiver.SHARE_VIDEO);
                ArrayList<String> files = shareIntent.getStringArrayListExtra(ShareReceiver.SHARE_FILE);
                if (!TextUtils.isEmpty(content)) {
                    edit_msg.setText(content);
                    chatingPresenter.sendMsg();
                    edit_msg.setText("");
                }
                if (!ListUtil.isEmpty(icons)) {
                    for (String img : icons) {
                        imageUrl = img;
                        chatingPresenter.sendImage();
                        imageUrl = "";
                    }
                }
                if (!ListUtil.isEmpty(videos)) {
                    for (String video : videos) {
                        chatingPresenter.sendVideo(video);
                    }
                }
                if (!ListUtil.isEmpty(files)) {
                    for (String file : files) {
                        chatingPresenter.sendFile(file);
                    }
                }
            } else if (shareIntent.getExtras() != null && shareIntent.getExtras().containsKey(ShareReceiver.SHARE_EXTRA_KEY)) {
                try {
                    String jsonStr = shareIntent.getExtras().getString(ShareReceiver.SHARE_EXTRA_KEY);
                    ExtendMessageEntity entity = JsonUtils.getGson().fromJson(jsonStr, ExtendMessageEntity.class);
                    chatingPresenter.sendExtendMessage(entity);
                } catch (Exception ex) {
                    LogUtil.e(TAG, "ERROR", ex);
                }
            }
            Toast.makeText(PbChatActivity.this, R.string.atom_ui_notice_already_share, Toast.LENGTH_SHORT).show();
            shareIntent.putExtra(Constants.BundleKey.IS_FROM_SHARE, false);//?????????false??????????????????
        } else if (isMultiImg) {
            ArrayList<ImgVideoBean> beans = shareIntent.getParcelableArrayListExtra(Constants.BundleKey.TRANS_MULTI_IMG);
            try {
                if (beans != null && !beans.isEmpty()) {
                    for (ImgVideoBean bean : beans) {
                        if (bean.type == ImgVideoBean.IMG) {
                            imageUrl = QtalkStringUtils.addFilePathDomain(bean.url, true);
                            chatingPresenter.sendImage();
                        } else if (bean.type == ImgVideoBean.VIDEO) {
                            chatingPresenter.sendVideo(VideoMessageResult.createInstance(bean));
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.e(TAG, "ERROR", e);
            }
        }

    }


    @Override
    public void closeActivity() {
//        this.onBackPressed();
        //??????backPreseed???????????????Can not perform this action after onSaveInstanceState?????? ??????????????????finish
        finish();
    }

    @Override
    public void setTitle(final String title) {
        runOnUiThread(() -> setActionBarTitle(title));
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public int getListSize() {
        return pbChatViewAdapter.getCount();
    }

    //?????????????????????
    @Override
    public boolean getShowNick() {
        int i = 0;
        return false;
    }

    //?????????????????????
    @Override
    public void setShowNick(boolean showNick) {

    }

    //?????????????????????
    @Override
    public String getJid() {
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            //??????????????????????????????
            case UPDATE_NICK:
                pbChatViewAdapter.notifyDataSetChanged();
                break;
            case ADD_EMOTICON:
                handleEmojiconResult(data);
                break;
            case ACTIVITY_GET_CAMERA_IMAGE:
                getCameraImageResult(data);
                break;
            case ACTIVITY_SELECT_PHOTO:
                selectPhotoResult(data);
                break;
            case FILE_SELECT_CODE:
                selectFileResult(data);
                break;
            case AT_MEMBER:
                edit_msg.requestFocus();
                mAtManager.onActivityResult(requestCode, resultCode, data);
//                String fm = data.getStringExtra("atName");
//                String fj = data.getStringExtra("atJid");
//                if (!TextUtils.isEmpty(fm)) {
//                    //???????????????????????????
//                    int index = edit_msg.getSelectionStart();
//                    //?????????????????????,getText???????????????
//                    Editable edit = edit_msg.getEditableText();
//                    if (index <= 0 || index >= edit.length()) {
//                        edit.append(fm);
//                        edit.append(" ");
//                    } else {
//                        edit.insert(index, fm + " ");
//                    }
//                }
//                atMap.put(fj,fm);
                break;
            case ACTIVITY_SELECT_LOCATION:
                selectLocationResult(data);
                break;
            case RECORD_VIDEO:
                String filePath = null;
                Uri _uri = data.getData();
                if (_uri != null && "content".equals(_uri.getScheme())) {
                    Cursor cursor = null;
                    try {
                        cursor = this
                                .getContentResolver()
                                .query(_uri,
                                        new String[]{android.provider.MediaStore.Video.VideoColumns.DATA},
                                        null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();
                            filePath = cursor.getString(0);
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }

                } else if (_uri != null) {
                    filePath = _uri.getPath();
                }
                if (!TextUtils.isEmpty(filePath)) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        chatingPresenter.sendVideo(filePath);
                    } else {
                        Toast.makeText(this, R.string.atom_ui_file_not_exist, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case ACTIVITY_SELECT_VIDEO:
                String path = data.getStringExtra("filepath");
                if (!TextUtils.isEmpty(path)) {
                    File file = new File(path);
                    if (file.exists()) {
                        chatingPresenter.sendVideo(path);
                    } else {
                        Toast.makeText(this, R.string.atom_ui_file_not_exist, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case TRANSFER_CONVERSATION_REQUEST_CODE:
                transferId = data.getStringExtra("userid");
                String origin = edit_msg.getText().toString();
                edit_msg.setText(mTransferConversationContext);
                chatingPresenter.transferConversation();
                edit_msg.setText(origin);
//                mTransferConversationContext = null;
//                transferId = null;
                break;
            case HONGBAO:
                String content = data.getStringExtra(Constants.BundleValue.HONGBAO);
                if (!TextUtils.isEmpty(content)) {
                    try {
                        String jsonStr = new String(Base64.decode(content, Base64.DEFAULT));
                        HongbaoContent hongbao = JsonUtils.getGson().fromJson(jsonStr, HongbaoContent.class);
                        chatingPresenter.hongBaoMessage(hongbao);
                    } catch (Exception ex) {
                        LogUtil.e(TAG, "ERROR", ex);
                    }
                }
                break;
            default:
                break;

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void getCameraImageResult(Intent data) {
        if (data != null) {
            //??????????????? ????????????????????? 2018-01-23
            String videopath = data.getStringExtra("videopath");
            if (!TextUtils.isEmpty(videopath)) {
                File file = new File(videopath);
                if (file.exists()) {
                    chatingPresenter.sendVideo(videopath);
                } else {
                    Toast.makeText(this, R.string.atom_ui_file_not_exist, Toast.LENGTH_SHORT).show();
                }
            } else {
                imageUrl = data.getStringExtra("path");
                if (!TextUtils.isEmpty(imageUrl)) {
                    chatingPresenter.sendImage();
                }
            }

            //??????
//            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
//            if (images != null && images.size() > 0) {
//                imageUrl = images.get(0).path;
//            }
            //??????
            /*imageUrl = data.getStringExtra(ImageClipActivity.KEY_CAMERA_PATH);
            //????????????????????????????????????????????????
            //Bitmap bitmap = ImageUtils.transformRotation(imageUrl);
            //ImageUtils.saveBitmap(bitmap, new File(imageUrl));
            //bitmap.recycle();*/
        }
    }

    public void selectPhotoResult(Intent data) {
        try {
            if (data != null) {
                //?????????????????????
                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                if (images.size() > 0) {
                    for (ImageItem image : images) {
                        imageUrl = image.path;
                        chatingPresenter.sendImage();
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "ERROR", e);
        }
    }

    public void selectFileResult(Intent data) {
        Uri uri = data.getData();
        // String path = FileUtils.getRealPath(uri);
        String path = FileUtils.getPath(this, uri);
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
//            if(file.exists()){
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                long size = fis.available();
                if (size > 50 * 1024 * 1024) {
                    Toast.makeText(this, R.string.atom_ui_tip_largefile, Toast.LENGTH_SHORT).show();
                    return;
                }
                chatingPresenter.sendFile(path);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            }
        }
    }

    public void selectLocationResult(Intent data) {
        QunarLocation location = new QunarLocation();
        location.latitude = data.getDoubleExtra(Constants.BundleKey.LATITUDE, 0);
        location.longitude = data.getDoubleExtra(Constants.BundleKey.LONGITUDE, 0);
        location.addressDesc = data.getStringExtra(Constants.BundleKey.ADDRESS);
        location.fileUrl = data.getStringExtra(Constants.BundleKey.FILE_NAME);
        location.name = data.getStringExtra(Constants.BundleKey.LOCATION_NAME);
        chatingPresenter.sendLocation(location);
    }

    public void checkShowCamera() {
        PermissionDispatcher.
                requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA,
                                PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE, PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE, PermissionDispatcher.REQUEST_RECORD_AUDIO}, PbChatActivity.this,
                        SHOW_CAMERA);


    }

    public void checkShowGallary() {
        PermissionDispatcher.
                requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE,
                        PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE}, PbChatActivity.this, SELECT_PIC);

    }

    public void choosePictrueSource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_choose_picture, (ViewGroup) this.getWindow().getDecorView(), false);
        TextView tv_change_gravtar_photos = (TextView) view.findViewById(R.id.tv_change_gravtar_photos);
        TextView tv_change_gravtar_camera = (TextView) view.findViewById(R.id.tv_change_gravtar_camera);
        tv_change_gravtar_photos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkShowGallary();
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            }
        });
        tv_change_gravtar_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkShowCamera();
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            }
        });

        builder.setView(view);
        mDialog = builder.show();
        mDialog.setCanceledOnTouchOutside(true);
    }

    public void chooseVideoSource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_choose_picture, (ViewGroup) this.getWindow().getDecorView(), false);
        TextView tv_change_gravtar_photos = (TextView) view.findViewById(R.id.tv_change_gravtar_photos);
        TextView tv_change_gravtar_camera = (TextView) view.findViewById(R.id.tv_change_gravtar_camera);
        tv_change_gravtar_photos.setText(R.string.atom_ui_function_choose_file);
        tv_change_gravtar_camera.setText(R.string.atom_ui_function_use_camera);
        tv_change_gravtar_photos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionDispatcher.
                        requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE,
                                PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE}, PbChatActivity.this, SELECT_VIDEO);
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            }
        });
        tv_change_gravtar_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PermissionDispatcher.
                        requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA, PermissionDispatcher.REQUEST_RECORD_AUDIO,
                                        PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE, PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE}, PbChatActivity.this,
                                SEND_VIDEO);
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
            }
        });

        builder.setView(view);
        mDialog = builder.show();
        mDialog.setCanceledOnTouchOutside(true);
    }

    public void chooseRtcType() {
        if(isFromChatRoom){
            PermissionDispatcher.
                    requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA,
                                    PermissionDispatcher.REQUEST_RECORD_AUDIO}, PbChatActivity.this,
                            REAL_GROUP_VIDEO);
        }else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_choose_picture, (ViewGroup) this.getWindow().getDecorView(), false);
            TextView tv_change_gravtar_photos = (TextView) view.findViewById(R.id.tv_change_gravtar_photos);
            TextView tv_change_gravtar_camera = (TextView) view.findViewById(R.id.tv_change_gravtar_camera);
            tv_change_gravtar_photos.setText(R.string.atom_ui_rtc_call);
            tv_change_gravtar_camera.setText(R.string.atom_ui_rtc_video_call);
            tv_change_gravtar_photos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PermissionDispatcher.
                            requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_RECORD_AUDIO},
                                    PbChatActivity.this, REAL_AUDIO);
                    if (mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                }
            });
            tv_change_gravtar_camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PermissionDispatcher.
                            requestPermissionWithCheck(PbChatActivity.this, new int[]{PermissionDispatcher.REQUEST_CAMERA,
                                            PermissionDispatcher.REQUEST_RECORD_AUDIO}, PbChatActivity.this,
                                    REAL_VIDEO);
                    if (mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                }
            });

            builder.setView(view);
            mDialog = builder.show();
            mDialog.setCanceledOnTouchOutside(true);
        }
    }

    private void chooseLocationType() {
        sendLocation();
        // TODO: 2017/12/27 ??????????????????????????????
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_choose_location, (ViewGroup) this.getWindow().getDecorView(), false);
//        TextView sendLocation = (TextView) view.findViewById(R.id.send_location);
//        final TextView shareLocation = (TextView) view.findViewById(R.id.share_current_location);
//        sendLocation.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sendLocation();
//                if (mDialog != null && mDialog.isShowing()) {
//                    mDialog.dismiss();
//                }
//            }
//        });
//        shareLocation.setOnClickListener(new View.OnClickListener() {
//
//            //??????????????????
//            @Override
//            public void onClick(View v) {
//                shareMyLocation();
//                if (mDialog != null && mDialog.isShowing()) {
//                    mDialog.dismiss();
//                }
//            }
//        });
//
//        builder.setView(view);
//        mDialog = builder.show();
//        mDialog.setCanceledOnTouchOutside(true);
    }

    private void shareMyLocation() {
        if (isFromChatRoom) {
            //???????????????????????????????????????,????????????????????????
            Toast.makeText(this, R.string.atom_ui_tip_group_not_support, Toast.LENGTH_SHORT).show();
            return;
        }
        //???????????????????????????????????????????????????Activity
        String shareId = UUID.randomUUID().toString();
        sendLocationPresenter.sendShareLocationMessage(shareId);
        Intent intent = new Intent();
        intent.setClass(this, ShareLocationActivity.class);
        intent.putExtra(ShareLocationActivity.FROM_ID, QtalkStringUtils.userId2Jid(CurrentPreference.getInstance().getPreferenceUserId()));
        intent.putExtra(ShareLocationActivity.SHARE_ID, shareId);
        startActivity(intent);
    }

    void sendLocation() {
        Intent intentLocation = new Intent(this, LocationActivity.class);
        intentLocation.putExtra(Constants.BundleKey.LOCATION_TYPE, LocationActivity.TYPE_SEND);
        startActivityForResult(intentLocation, ACTIVITY_SELECT_LOCATION);
    }

    void sendFile() {
        /** ??????????????????????????????????????? **/
        Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
        intentFile.setType("*/*");
        intentFile.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intentFile, (String) getText(R.string.atom_ui_tip_select_file_upload)),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(PbChatActivity.this, R.string.atom_ui_tip_install_manager, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void sendActivity() {
        StringBuilder sb = new StringBuilder();
        String username = CurrentPreference.getInstance().getUserid();
        sb.append(QtalkNavicationService.SEND_ACTIVITY)
                .append("?username=").append(username).append("&sign=")
                .append(BinaryUtil.MD5(username + Constants.SIGN_SALT))
                .append("&company=qunar&")
                .append("group_id=")
                .append(jid)
                .append("&q_d=").append(QtalkNavicationService.getInstance().getXmppdomain())
                .append("&rk=" + CurrentPreference.getInstance().getVerifyKey())
                .append("&action=event");
        Uri uri = Uri.parse(sb.toString());
        Intent intent = new Intent(this, QunarWebActvity.class);
        intent.putExtra(QunarWebActvity.IS_HIDE_BAR, true);
        intent.setData(uri);
        startActivity(intent);
    }

    public void giveLuckyMoney(boolean isAA) {
        if(TextUtils.isEmpty(QtalkNavicationService.getInstance().getPayurl())){
            StringBuilder sb = new StringBuilder();
            String username = CurrentPreference.getInstance().getUserid();
            sb.append(isAA ? QtalkNavicationService.AA_PAY_URL : QtalkNavicationService.HONGBAO_URL)
                    .append("?username=").append(username).append("&sign=")
                    .append(BinaryUtil.MD5(username + Constants.SIGN_SALT))
                    .append("&company=qunar&")
                    .append(isFromChatRoom ? "group_id=" : "user_id=")
                    .append(jid)
                    .append("&q_d=").append(QtalkNavicationService.getInstance().getXmppdomain())
                    .append("&rk=" + CurrentPreference.getInstance().getVerifyKey());
            Uri uri = Uri.parse(sb.toString());
            Intent intent = new Intent(this, QunarWebActvity.class);
            intent.putExtra(Constants.BundleKey.WEB_FROM, Constants.BundleValue.HONGBAO);
            intent.putExtra(QunarWebActvity.IS_HIDE_BAR, true);
            intent.setData(uri);
            startActivityForResult(intent, HONGBAO);
        }else {
            chatingPresenter.checkAlipayAccount();
        }
    }

    /**
     * ???????????? ??????????????????
     */
    public void transferConversation() {

        Intent intent = ReflectUtil.getQtalkServiceRNActivityIntent(PbChatActivity.this);
        if (intent == null) {
            return;
        }
        intent.putExtra("module", Constants.RNKey.MERCHANT);
        intent.putExtra("Screen", "Seats");

        intent.putExtra("shopJid", QtalkStringUtils.parseId(jid));
        intent.putExtra("customerName", QtalkStringUtils.parseId(realJid));
        startActivity(intent);
    }

    void sendVideo() {
        try {
            Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // ????????????0????????????
            startActivityForResult(videoIntent, RECORD_VIDEO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void selectPic() {
//        Intent intent1 = new Intent(this, PictureSelectorActivity.class);
//        intent1.putExtra("isMultiSel", true);
//        intent1.putExtra(PictureSelectorActivity.SHOW_EDITOR, true);
//        startActivityForResult(intent1, ACTIVITY_SELECT_PHOTO);
        //?????????????????????
        ImageSelectUtil.startSelectPhotos(this, new SelectCallback() {
            @Override
            public void onResult(ArrayList<Photo> photos, ArrayList<String> paths, boolean isOriginal) {
//                new String();
                try {
                    if (photos != null) {
                        //?????????????????????
//                        ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                        if (paths.size() > 0) {
                            for (Photo image : photos) {
                                if (image.type.startsWith("image")) {
                                    imageUrl = image.path;
                                    chatingPresenter.sendImage();
                                } else if (image.type.startsWith("video")) {


                                    String time = IMUserDefaults.getStandardUserDefaults().getStringValue(CommonConfig.globalContext,
                                            CurrentPreference.getInstance().getUserid()
                                                    + QtalkNavicationService.getInstance().getXmppdomain()
                                                    + CommonConfig.isDebug
                                                    + "videoMaxTime");
                                    if (TextUtils.isEmpty(time) || time.equals("0")) {
                                        time = (300 * 1000) + "";
                                    }
                                    if (image.duration > Long.parseLong(time)) {
                                        boolean flag = DataUtils.getInstance(PbChatActivity.this).getPreferences("notRemindVideoToFile", false);
                                        if (flag) {
                                            chatingPresenter.sendFile(image.path);
                                        } else {
                                            commonDialog = new CommonDialog.Builder(PbChatActivity.this);
                                            commonDialog.setMessage("??????????????????,????????????????????????!");
                                            commonDialog.setPositiveButton(getString(R.string.atom_ui_common_confirm), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(final DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                    chatingPresenter.sendFile(image.path);

                                                }
                                            });
                                            commonDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(final DialogInterface dialog, int which) {
                                                    dialog.dismiss();
//                            finish();

                                                }
                                            });
                                            commonDialog.setNeutralButton(getString(R.string.atom_ui_btn_not_remind), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int i) {
                                                    dialog.dismiss();
                                                    DataUtils.getInstance(PbChatActivity.this).putPreferences("notRemindVideoToFile", true);
                                                    chatingPresenter.sendFile(image.path);
                                                }
                                            });
                                            commonDialog.create().show();
                                        }

                                    } else {
                                        chatingPresenter.sendVideo(image.path);
                                    }


                                } else {
                                    Toast.makeText(PbChatActivity.this, "????????????,????????????", Toast.LENGTH_SHORT).show();
                                }

                            }
                        }

                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "ERROR", e);
                }
            }
//        ImagePicker.getInstance().setSelectLimit(9);
//        Intent intent1 = new Intent(this, ImageGridActivity.class);
//        /* ???????????????????????????????????????????????????????????????
//         * ???????????????ImagePickerActivity
//         * */
////                                intent1.putExtra(ImageGridActivity.EXTRAS_IMAGES,images);
//        startActivityForResult(intent1, ACTIVITY_SELECT_PHOTO);
        });
    }

    void selectVideo() {
        Intent intent = new Intent(this, VideoSelectorActivity.class);
        intent.putExtra("isMultiSel", false);
        startActivityForResult(intent, ACTIVITY_SELECT_VIDEO);
    }

    void showCamera() {
//        Intent intent = new Intent(this, ImageClipActivity.class);
//        intent.putExtra(ImageClipActivity.KEY_CLIP_ENABLE, false);
//        File file = new File(MyDiskCache.getDirectory(),
//                UUID.randomUUID().toString() + ".jpg");
//        intent.putExtra(ImageClipActivity.KEY_CAMERA_PATH, Uri.fromFile(file));
//        startActivityForResult(intent, ACTIVITY_GET_CAMERA_IMAGE);
        Logger.i("????????????");
        //??????????????? ?????????????????????
        startActivityForResult(new Intent(this, CameraActivity.class), ACTIVITY_GET_CAMERA_IMAGE);
        //?????????????????????
//        Intent intent = new Intent(this, ImageGridActivity.class);
//        intent.putExtra(ImageGridActivity.EXTRAS_TAKE_PICKERS, true); // ???????????????????????????
//        startActivityForResult(intent, ACTIVITY_GET_CAMERA_IMAGE);
    }


    RecommendPhotoPop photoPop;

    void showRecommendPop() {
        atom_bottom_frame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isFinishing()) {
                    return;
                }
                if (photoPop != null) {
                    return;
                }
                if (linearlayout_tab.getVisibility() == View.GONE)
                    return;
                if (latestImage != null && (System.currentTimeMillis() - latestImage.addTime * 1000) > 30000) {
                    return;
                }
                if (latestImage == null || TextUtils.isEmpty(latestImage.path)) {
                    return;
                }
                photoPop = RecommendPhotoPop.recommendPhoto(PbChatActivity.this, total_bottom_layout, latestImage.path, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        imageUrl = latestImage.path;
                        chatingPresenter.sendImage();
                        if (photoPop.isShowing())
                            photoPop.dismiss();
                    }
                });
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        if (photoPop != null && photoPop.isShowing()) {
                            photoPop.dismiss();
                        }
                    }
                }, 15 * 1000);
            }
        });
    }


    void voice_switch_btnClickListener() {
        PermissionDispatcher.requestPermissionWithCheck(this, new int[]{PermissionDispatcher.REQUEST_READ_EXTERNAL_STORAGE,
                PermissionDispatcher.REQUEST_WRITE_EXTERNAL_STORAGE, PermissionDispatcher.REQUEST_RECORD_AUDIO}, this, RECORD);
    }


    void handleEmojiconResult(Intent data) {
        //?????????????????????,????????????
        faceView.resetFavoriteEmotion(getFavoriteMap(this));
        faceView.resetFavoriteTab();
    }

    /**
     * ??????????????????dialog
     */
    private void showConfirmHungupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        commonDialog.setTitle(getString(R.string.atom_ui_open_permission_title));
        commonDialog.setMessage(getString(R.string.atom_ui_tip_server_hungup));
        commonDialog.setPositiveButton(getString(R.string.atom_ui_common_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
                HttpUtil.serverCloseSession(getRealJid(), getFromId(), getToId(), new ProtocolCallback.UnitCallback<String>() {
                    @Override
                    public void onCompleted(String s) {
                        toast(s);
                        finish();
                    }

                    @Override
                    public void onFailure(String errMsg) {
                        toast("??????????????????");
                    }
                });

            }
        });
        commonDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        commonDialog.create().show();
    }

    /**
     * ????????????????????????
     */
    private void showCameraPermissionDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        commonDialog.setTitle(getString(R.string.atom_ui_open_permission_title));
        commonDialog.setMessage(getString(R.string.atom_ui_open_permission_camera_message));
        commonDialog.setPositiveButton(getString(R.string.atom_ui_setting_title), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
                Uri packageURI = Uri.parse("package:" + PbChatActivity.this.getApplicationInfo().packageName);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                startActivity(intent);

            }
        });
        commonDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        commonDialog.create().show();
    }

    /**
     * ????????????????????????
     */
    private void showVoicePermissionDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        commonDialog.setTitle(getString(R.string.atom_ui_open_permission_title));
        commonDialog.setMessage(getString(R.string.atom_ui_open_permission_voice_message));
        commonDialog.setPositiveButton(getString(R.string.atom_ui_setting_title), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
                Uri packageURI = Uri.parse("package:" + PbChatActivity.this.getApplicationInfo().packageName);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                startActivity(intent);

            }
        });
        commonDialog.setNegativeButton(getString(R.string.atom_ui_common_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        commonDialog.create().show();
    }

    @Override
    public void responsePermission(int requestCode, boolean granted) {
        if (!granted) {
            if (requestCode == SHOW_CAMERA) {
                showCameraPermissionDialog();
            } else if (requestCode == RECORD) {
                showVoicePermissionDialog();
            }
            return;
        }
        if (requestCode == SHOW_CAMERA) {
            showCamera();
        } else if (requestCode == SELECT_PIC) {
            selectPic();
        } else if (requestCode == READ_LOCATION) {
            chooseLocationType();
        } else if (requestCode == SEND_VIDEO) {
            sendVideo();
        } else if (requestCode == RECORD) {
//            showRecordView();
        } else if (requestCode == READ_FILE) {
            sendFile();
        } else if (requestCode == SELECT_VIDEO) {
            selectVideo();
        } else if (requestCode == REAL_VIDEO) {
            boolean isconsult = (Integer.valueOf(getChatType()) == ConversitionType.MSG_TYPE_CONSULT || Integer.valueOf(getChatType()) == ConversitionType.MSG_TYPE_CONSULT_SERVER) ? true : false;
            Intent i = new Intent("android.intent.action.VIEW",
                    Uri.parse(CommonConfig.schema + "://qcrtc/webrtc?fromid="
                            + CurrentPreference.getInstance().getPreferenceUserId()
                            + "&toid=" + (isconsult ? getRealJid() : getToId())
                            + "&chattype=" + getChatType()
                            + "&realjid=" + getRealJid()
                            + "&isFromChatRoom=" + isFromChatRoom
                            + "&offer=false&video=true"));
            startActivity(i);
//            connectionUtil.lanuchChatVideo(true, CurrentPreference.getInstance().getUserid(), (isconsult ? getRealJid() : getToId()));
//            ((IP2pRTC) chatingPresenter).startVideoRtc();
        } else if (requestCode == REAL_AUDIO) {
            boolean isconsult = (Integer.valueOf(getChatType()) == ConversitionType.MSG_TYPE_CONSULT || Integer.valueOf(getChatType()) == ConversitionType.MSG_TYPE_CONSULT_SERVER) ? true : false;
            Intent i = new Intent("android.intent.action.VIEW",
                    Uri.parse(CommonConfig.schema + "://qcrtc/webrtc?fromid=" +
                            QtalkStringUtils.userId2Jid(CurrentPreference.getInstance().getPreferenceUserId())
                            + "&toid=" + (isconsult ? getRealJid() : getToId())
                            + "&chattype=" + getChatType()
                            + "&realjid=" + getRealJid()
                            + "&isFromChatRoom=" + isFromChatRoom
                            + "&offer=false&video=false"));
            startActivity(i);
//            connectionUtil.lanuchChatVideo(false, CurrentPreference.getInstance().getUserid(), (isconsult ? getRealJid() : getToId()));
//            ((IP2pRTC) chatingPresenter).startAudioRtc();
        } else if (requestCode == REAL_GROUP_VIDEO){
            connectionUtil.lanuchGroupVideo(jid,qtNewActionBar.getTextTitle().getText().toString());
            ((IPGroupRtc) chatingPresenter).startGroupVideoRtc();
        }
    }

    @Override
    public void onTextAdd(String content, int start, int length) {
        Editable edit = edit_msg.getEditableText();
        edit.insert(start, content);
    }

    @Override
    public void onTextDelete(int start, int length) {
        Editable edit = edit_msg.getEditableText();
        edit.delete(start, start + length);
    }

    private final class OnAddFavoriteEmoticonClickListener implements FaceGridView.AddFavoriteEmojiconClickListener {

        @Override
        public void onAddFavoriteEmojiconClick() {
            Intent intent = new Intent(PbChatActivity.this, ManageEmojiconActivity.class);
            intent.putExtra(PictureSelectorActivity.TYPE, PictureSelectorActivity.TYPE_EMOJICON);
            startActivityForResult(intent, ADD_EMOTICON);
        }
    }

    private final class FavoriteEmoticonOnClickListener implements FaceGridView.OnEmoticionsClickListener {

        @Override
        public void onEmoticonClick(EmoticonEntity entity, String pkgId) {
            imageUrl = entity.fileFiexd;
            if (imageUrl == null) {
                imageUrl = entity.fileOrg;
            }
            if (imageUrl != null) {
                chatingPresenter.sendImage();
            }
        }
    }

    private final class DefaultOnEmoticionsClickListener implements FaceGridView.OnEmoticionsClickListener {
        @Override
        public void onEmoticonClick(EmoticonEntity entity, String pkgId) {
            StringBuilder text = new StringBuilder();
            text.append((char) 0).
                    append(pkgId).
                    append((char) 1).
                    append(entity.shortCut).append((char) 255);
            int index = edit_msg.getSelectionStart();
            Editable edit = edit_msg.getEditableText();
            if (index < 0 || index > edit.length()) {
                edit.append(text);
            } else {
                edit.insert(index, text);
            }
        }
    }

    private final class ExtentionEmoticionsClickListener implements FaceGridView.OnEmoticionsClickListener {
        @Override
        public void onEmoticonClick(EmoticonEntity entity, String pkgId) {
            String originText = edit_msg.getText().toString();
            edit_msg.setText(((char) 0) + pkgId + ((char) 1) + entity.shortCut + ((char) 255));
            chatingPresenter.sendMsg();
            edit_msg.setText(originText);

        }
    }

    //?????????????????????????????? 60s????????? ??????????????????????????????
    CountDownTimer encryptRequestCountTimer = new CountDownTimer(60 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            sendEncryptMessage(EncryptMessageType.CANCEL);
            if (encryptSessionDialog != null && encryptSessionDialog.isShowing())
                encryptSessionDialog.dismiss();
        }
    };

    void sendEncryptMessage(int type) {
        String body = "";
        if (EncryptMessageType.BEGIN == type) {
            EncryptBeginMsg encryptBeginMsg = new EncryptBeginMsg();
            encryptBeginMsg.type = DailyMindConstants.PASSOWRD;
            encryptBeginMsg.pwd = passwordSubString;
            body = JsonUtils.getGson().toJson(encryptBeginMsg);
            ((SingleSessionPresenter) chatingPresenter).sendEncryptSignalMsg(body, type);
            showEncryptSessionDialog(type);
            encryptRequestCountTimer.start();
        } else {
            if (EncryptMessageType.AGREE == type) {
                body = (String) getText(R.string.atom_ui_body_open_encrypted);
            } else if (EncryptMessageType.REFUSE == type)
                body = (String) getText(R.string.atom_ui_tip_refused_open_encrypted);
            else if (EncryptMessageType.CANCEL == type)
                body = (String) getText(R.string.atom_ui_btn_cancel_encrypted);
            else if (EncryptMessageType.CLOSE == type)
                body = (String) getText(R.string.atom_ui_tip_close_encrypted);
            ((SingleSessionPresenter) chatingPresenter).sendEncryptSignalMsg(body, type);
            toast(body);
        }

    }

    //????????????
    public void encryptConversation() {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_encrypt_choose, (ViewGroup) this.getWindow().getDecorView(), false);
        ImageView atom_ui_app_icon = (ImageView) view.findViewById(R.id.atom_ui_app_icon);
        atom_ui_app_icon.setImageResource(getApplicationInfo().icon);
        Button cancel_btn = (Button) view.findViewById(R.id.cancel_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (encryptChooseDialog != null && encryptChooseDialog.isShowing())
                    encryptChooseDialog.dismiss();
            }
        });
        Button encrypt_btn = (Button) view.findViewById(R.id.encrypt_btn);
        if (isEncrypt()) {
            encrypt_btn.setText(R.string.atom_ui_btn_close_encryption);
            encrypt_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (encryptChooseDialog != null && encryptChooseDialog.isShowing())
                        encryptChooseDialog.dismiss();
                    sendEncryptMessage(EncryptMessageType.CLOSE);
                    DataCenter.encryptUsers.remove(getToId());
                    DataCenter.decryptUsers.remove(getToId());
                }
            });
        } else {
            encrypt_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (encryptChooseDialog != null && encryptChooseDialog.isShowing())
                        encryptChooseDialog.dismiss();
                    isEncryptOperate = true;
                    DailyMindMain pbm = passwordPresenter.getDailyMainByTitleFromDB();
                    if (pbm == null)
                        getCloudPasswordBoxMain();
                    else {
                        DailyMindSub dailyMindSub = passwordPresenter.getDailySubByTitleFromDB(getToId(), String.valueOf(pbm.qid));
                        if (dailyMindSub == null) {
                            getCloudPasswordBoxSub(String.valueOf(pbm.qid));
                        } else showPasswordBoxDialog(pbm);
                    }
                }
            });
        }
        Button decrypt_btn = (Button) view.findViewById(R.id.decrypt_btn);
        decrypt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (encryptChooseDialog != null && encryptChooseDialog.isShowing())
                    encryptChooseDialog.dismiss();
                if (DataCenter.decryptUsers.containsKey(getToId())) {
                    toast((String) getText(R.string.atom_ui_tip_decrypted));
                    return;
                }
                isEncryptOperate = false;
                DailyMindMain pbm = passwordPresenter.getDailyMainByTitleFromDB();
                if (pbm == null) {
                    getCloudPasswordBoxMain();
                } else {
                    DailyMindSub dailyMindSub = passwordPresenter.getDailySubByTitleFromDB(getToId(), String.valueOf(pbm.qid));
                    if (dailyMindSub == null) {
                        getCloudPasswordBoxSub(String.valueOf(pbm.qid));
                    } else showPasswordBoxDialog(pbm);
                }
            }
        });
        decrypt_btn.setVisibility(isEncrypt() ? View.GONE : View.VISIBLE);

        builder.setView(view);
        encryptChooseDialog = builder.show();
        encryptChooseDialog.setCanceledOnTouchOutside(true);
    }

    private void createConversationMainPassword(String title, String desc) {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("type", DailyMindConstants.CHATPASSWORD + "");
            params.put("title", title);
            params.put("desc", desc);
            params.put("content", passwordMainStr);
            String content = AESTools.encodeToBase64(passwordMainStr, JsonUtils.getGson().toJson(params));
            params.put("content", content);
            passwordPresenter.operateDailyMindFromHttp(DailyMindConstants.SAVE_TO_MAIN, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createConversationSubPassword(DailyMindMain dailyMindMain, String
            passwordSubString) {
        try {
            if (dailyMindMain != null)//?????????????????????
            {
                Map<String, String> params = new HashMap<>();
                params.put("qid", dailyMindMain.qid + "");
                params.put("type", DailyMindConstants.PASSOWRD + "");
                params.put("title", getToId());
                params.put("U", getToId());
                params.put("P", TextUtils.isEmpty(passwordSubString) ? GenerateRandomPassword.creatGenerateRandomPassword(8, 6, 4, 2) : passwordSubString);
                params.put("desc", "");
                String content = AESTools.encodeToBase64(passwordMainStr, JsonUtils.getGson().toJson(params));
                params.put("content", content);
                passwordPresenter.operateDailyMindFromHttp(DailyMindConstants.SAVE_TO_SUB, params);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCloudPasswordBoxMain() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("version", "0");
        params.put("type", String.valueOf(DailyMindConstants.CHATPASSWORD));
        passwordPresenter.operateDailyMindFromHttp(DailyMindConstants.GET_CLOUD_MAIN, params);
    }

    private void getCloudPasswordBoxSub(String qid) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("qid", qid);
        params.put("version", "0");
        params.put("type", String.valueOf(DailyMindConstants.PASSOWRD));
        passwordPresenter.operateDailyMindFromHttp(DailyMindConstants.GET_CLOUD_SUB, params);
    }

    /**
     * ?????????????????????dialog
     *
     * @param dailyMindMain ?????????bean
     */
    private void showPasswordBoxDialog(final DailyMindMain dailyMindMain) {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_encrypt_conversation, (ViewGroup) this.getWindow().getDecorView(), false);
        final EditText password_main = (EditText) view.findViewById(R.id.password_main);
        Button cancel_encrypt = (Button) view.findViewById(R.id.cancel_encrypt);
        Button open_encrypt = (Button) view.findViewById(R.id.open_encrypt);
        if (dailyMindMain == null && isEncryptOperate)
            open_encrypt.setText(R.string.atom_ui_tip_new_lockbox);
        final EditText password_box_title = (EditText) view.findViewById(R.id.password_box_title);
        final EditText password_box_desc = (EditText) view.findViewById(R.id.password_box_desc);
        open_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordMainStr = password_main.getText().toString();
                if (TextUtils.isEmpty(passwordMainStr)) {
                    toast((String) getText(R.string.atom_ui_tip_master_password_cannot_empty));
                    return;
                }

                if (dailyMindMain == null)
                    createConversationMainPassword(password_box_title.getText().toString(), password_box_desc.getText().toString());
                else {
                    try {
                        String content = AESTools.decodeFromBase64(passwordMainStr, dailyMindMain.content);
                        if (TextUtils.isEmpty(content)) {
                            toast((String) getText(R.string.atom_ui_tip_master_password_not_correct));
                            return;
                        }
                        DailyMindSub dailyMindSub = passwordPresenter.getDailySubByTitleFromDB(getToId(), String.valueOf(dailyMindMain.qid));
                        if (isEncryptOperate && (dailyMindSub == null || TextUtils.isEmpty(dailyMindSub.content))) {
                            createConversationSubPassword(dailyMindMain, passwordSubString);
                            return;
                        } else if (isEncryptOperate) {//??????????????????
                            DailyMindSub pbs = JsonUtils.getGson().fromJson(AESTools.decodeFromBase64(passwordMainStr, dailyMindSub.content), DailyMindSub.class);
                            if (pbs != null && !isEncrypt()) {
                                passwordSubString = pbs.P;
                                sendEncryptMessage(EncryptMessageType.BEGIN);
                            }
                        } else if (!isEncryptOperate && dailyMindSub != null) {//????????????
                            DailyMindSub pbs = JsonUtils.getGson().fromJson(AESTools.decodeFromBase64(passwordMainStr, dailyMindSub.content), DailyMindSub.class);
                            if (pbs != null) {
                                DataCenter.decryptUsers.put(getToId(), pbs.P);
                                pbChatViewAdapter.notifyDataSetChanged();
                                toast((String) getText(R.string.atom_ui_tip_decrypted));
                            }
                        } else {
                            toast((String) getText(R.string.atom_ui_tip_operation_failed));
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        toast((String) getText(R.string.atom_ui_tip_master_password_not_correct));
                    }

                }
                if (encryptDialog != null && encryptDialog.isShowing())
                    encryptDialog.dismiss();
            }
        });

        cancel_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (encryptDialog != null && encryptDialog.isShowing())
                    encryptDialog.dismiss();
            }
        });

        builder.setView(view);
        encryptDialog = builder.show();
        encryptDialog.setCanceledOnTouchOutside(true);
        return;
    }

    private void showEncryptSessionDialog(int type) {
        showEncryptSessionDialog(type, null);
    }

    private void showEncryptSessionDialog(int type, final IMMessage message) {
        if (isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.atom_ui_dialog_encrypt_seesion, (ViewGroup) this.getWindow().getDecorView(), false);
        TextView encrypt_text = (TextView) view.findViewById(R.id.encrypt_text);
        Button encrypt_button1 = (Button) view.findViewById(R.id.encrypt_button1);
        Button encrypt_button2 = (Button) view.findViewById(R.id.encrypt_button2);
        if (EncryptMessageType.BEGIN == type) {
            encrypt_text.setText(R.string.atom_ui_tip_requesting_open_encrypted);
            encrypt_button1.setVisibility(View.GONE);
            encrypt_button2.setText(R.string.atom_ui_btn_cancel_encrypted);
            encrypt_button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    encryptRequestCountTimer.cancel();
                    dismissEncryptSessionDialog();
                    sendEncryptMessage(EncryptMessageType.CANCEL);
                }
            });
        } else if (EncryptMessageType.WAIT == type) {
            encrypt_text.setText(R.string.atom_ui_tip_request_encrypt);
            encrypt_button1.setText(getString(R.string.atom_ui_btn_refuse_encrypt));
            encrypt_button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissEncryptSessionDialog();
                    sendEncryptMessage(EncryptMessageType.REFUSE);
                }
            });
            encrypt_button2.setText(getString(R.string.atom_ui_btn_open_encrypt));
            encrypt_button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissEncryptSessionDialog();
                    sendEncryptMessage(EncryptMessageType.AGREE);
                    if (message != null) {
                        parseEncryptPassword(message.getBody());
                    }
                }
            });
        }

        builder.setView(view);
        encryptSessionDialog = builder.show();
        encryptSessionDialog.setCanceledOnTouchOutside(false);
        return;
    }

    private void parseEncryptPassword(String body) {
        EncryptBeginMsg encryptBeginMsg = JsonUtils.getGson().fromJson(body, EncryptBeginMsg.class);
        if (encryptBeginMsg != null) {
            passwordSubString = encryptBeginMsg.pwd;
            DataCenter.encryptUsers.put(getToId(), passwordSubString);
            isEncryptOperate = true;
            DailyMindMain pbm = passwordPresenter.getDailyMainByTitleFromDB();
            if (pbm == null) {
                getCloudPasswordBoxMain();
            } else {
                DailyMindSub pbs = passwordPresenter.getDailySubByTitleFromDB(getToId(), String.valueOf(pbm.qid));
                if (pbs == null) getCloudPasswordBoxSub(String.valueOf(pbm.qid));
//                else {
//                    try {
//                        if (!TextUtils.isEmpty(pbs.content)) {
//                            String value = AESTools.decodeFromBase64(passwordSubString, pbs.content);
//                            if (TextUtils.isEmpty(value)) {//????????????  ???????????????????????? ????????? ??????????????????
//                                pbs.P = passwordSubString;
//                                pbs.state = DailyMindConstants.UPDATE;
//                                pbs.content = "";
//                                pbs.content = AESTools.encodeToBase64(passwordSubString, JsonUtils.getGson().toJson(pbs));
//                                passwordPresenter.operateDailyMindFromHttp(DailyMindConstants.UPDATE_SUB, pbs);
//                            }
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        }
    }

    private void dismissEncryptSessionDialog() {
        if (encryptSessionDialog != null && encryptSessionDialog.isShowing())
            encryptSessionDialog.dismiss();
    }

    //?????????????????????????????????
    private boolean isEncrypt() {
        return DataCenter.encryptUsers.containsKey(getToId());
    }

    protected class HandleChatEvent {
        /**
         * ????????????????????????
         *
         * @param downEmojComplete
         */
        public void onEventMainThread(EventBusEvent.DownEmojComplete downEmojComplete) {
//            faceView.initFaceGridView(EmotionUtils.getExtEmotionsMap(this), EmotionUtils.getDefaultEmotion(this),EmotionUtils.getDefaultEmotion1(this), EmotionUtils.getFavoriteMap(this));
//            map = EmotionUtils.getExtEmotionsMap()
            faceView.resetTabLayout();
        }

        public void onEventMainThread(EventBusEvent.PasswordBox passwordBox) {
            try {
                if (passwordBox.dailyMindMain != null)//?????????????????????
                {
                    createConversationSubPassword(passwordBox.dailyMindMain, passwordSubString);

                } else if (passwordBox.dailyMindSub != null) {//???????????????????????????
                    DailyMindSub pbs = JsonUtils.getGson().fromJson(AESTools.decodeFromBase64(passwordMainStr, passwordBox.dailyMindSub.content), DailyMindSub.class);
                    if (!isEncrypt()) {
                        passwordSubString = pbs.P;//passwordBox.dailyMindSub.content;
                        sendEncryptMessage(EncryptMessageType.BEGIN);
                    }
                } else if (passwordBox.dailyMindMains != null) {
                    DailyMindMain pbm = passwordPresenter.getDailyMainByTitleFromDB();
                    if (pbm == null) {
                        if (isEncryptOperate)
                            showPasswordBoxDialog(pbm);
                        else toast(getString(R.string.atom_ui_tip_no_passwordbox));
                    } else getCloudPasswordBoxSub(String.valueOf(pbm.qid));
                } else if (passwordBox.dailyMindSubs != null) {
                    DailyMindMain pbm = passwordPresenter.getDailyMainByTitleFromDB();
                    showPasswordBoxDialog(pbm);
                } else if (passwordBox.dailyMindMains == null) {
                    toast(getString(R.string.atom_ui_tip_pull_passwordbox_failed));
                } else if (passwordBox.dailyMindSubs == null) {
                    toast(getString(R.string.atom_ui_tip_pull_passwordbox_failed));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * ????????????
         *
         * @param sendTransMsg
         */
        public void onEventMainThread(EventBusEvent.SendTransMsg sendTransMsg) {
            final Serializable imMessage = sendTransMsg.msg;
            final String transJid = sendTransMsg.transId;
            if (imMessage == null) return;
            // TODO: 2017/8/24 ????????????
            if (IMMessage.class.isInstance(imMessage)) {
                BackgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        PbChatActivity.this.transferId = transJid;
                        IMMessage selectedMessage = (IMMessage) imMessage;
                        selectedMessages.clear();
                        selectedMessages.add(selectedMessage);
                        chatingPresenter.transferMessage();
                        PbChatActivity.this.transferId = null;
//                        if (jid.equals(transJid)) {
//                            chatingPresenter.receiveMsg(selectedMessage);
//                        }
                        selectedMessages.clear();
                    }
                });
            } else if (String.class.isInstance(imMessage)) {
                if (imMessage.equals("share")) {
                    PbChatActivity.this.transferId = transJid;
                    chatingPresenter.shareMessage(pbChatViewAdapter.getSharingMsg());
                    PbChatActivity.this.transferId = null;
                    cancelMore();
                }
            }
            Toast.makeText(PbChatActivity.this, getString(R.string.atom_ui_tip_send_success), Toast.LENGTH_LONG).show();
        }

        /**
         * ????????????
         *
         * @param
         */
//        public void onEventMainThread(EventBusEvent.SendShareMsg sendShareMsg) {
//            final String extMsg = sendShareMsg.msg;
//            final String transJid = sendShareMsg.shareId;
//            if (TextUtils.isEmpty(extMsg)) return;
//
//            BackgroundExecutor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    PbChatActivity.this.transferId = transJid;
//                    IMMessage imMessage = new IMMessage();
//                    imMessage.setExt(extMsg);
//                    imMessage.setMsgType(ProtoMessageOuterClass.MessageType.MessageTypeCommonTrdInfo_VALUE);
//                    imMessage.setBody("[??????]:??????????????????app");
//                    List<IMMessage> list = new ArrayList<IMMessage>();
//                    list.add(imMessage);
//                    PbChatActivity.this.transferId = transJid;
//                    chatingPresenter.shareMessage(list);
//                    PbChatActivity.this.transferId = null;
//                }
//            });
//        }
    }

    @Override
    public void parseEncryptSignal(final IMMessage message) {
        if (message != null) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (message.isCarbon()) {
                        dismissEncryptSessionDialog();
                        Toast.makeText(PbChatActivity.this, getString(R.string.atom_ui_tip_deal_other_client), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (message.getMsgType() == EncryptMessageType.BEGIN) {//????????????
                        showEncryptSessionDialog(EncryptMessageType.WAIT, message);
                    } else if (message.getMsgType() == EncryptMessageType.AGREE) {//????????????
                        encryptRequestCountTimer.cancel();
                        dismissEncryptSessionDialog();
                        DataCenter.encryptUsers.put(getToId(), passwordSubString);
                        toast(getString(R.string.atom_ui_btn_open_encrypt));
                    } else if (message.getMsgType() == EncryptMessageType.CANCEL) {//??????
                        dismissEncryptSessionDialog();
                        toast(getString(R.string.atom_ui_common_cancel));
                    } else if (message.getMsgType() == EncryptMessageType.REFUSE) {//??????
                        encryptRequestCountTimer.cancel();
                        dismissEncryptSessionDialog();
                        toast(getString(R.string.atom_ui_tip_refused_open_encrypted));
                    } else if (message.getMsgType() == EncryptMessageType.CLOSE) {//??????
                        dismissEncryptSessionDialog();
                        DataCenter.encryptUsers.remove(getToId());
                        pbChatViewAdapter.notifyDataSetChanged();
                        toast(getString(R.string.atom_ui_tip_close_encrypted));
                    }
                }
            });
        }

    }

    @Override
    public void updateUploadProgress(final IMMessage message, final int progress,
                                     final boolean isDone) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbChatViewAdapter.notifyDataSetChanged();
                LogUtil.i("lex pbactivity  updateUploadProgress  progress = " + message.getProgress() + "   status = " + message.getReadState());
//                pbChatViewAdapter.updateMessgesUpdate(chat_region.f(), message, progress, isDone);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (photoPop != null && photoPop.isShowing()) {
            photoPop.dismiss();
        }
        if (mPanelRoot != null && mPanelRoot.getVisibility() == View.VISIBLE) {
            KPSwitchConflictUtil.hidePanelAndKeyboard(mPanelRoot);
            return;
        }
        if (pbChatViewAdapter != null && pbChatViewAdapter.isShareStatus()) {
            atom_bottom_frame.setVisibility(View.VISIBLE);
            atom_bottom_more.setVisibility(View.GONE);
            pbChatViewAdapter.changeShareStatus(false);
            pbChatViewAdapter.notifyDataSetChanged();
            return;
        }
        super.onBackPressed();
    }


    ImageItem latestImage;

    @Override
    public void onImagesLoaded(ImageItem imageItem) {
        latestImage = imageItem;
    }

    @Override
    public void popNotice(NoticeBean noticeBean) {
        showNoticePopupWindow(noticeBean);
    }

    @Override
    public void clearMessage() {
        pbChatViewAdapter.clearAndAddMsgs(new ArrayList<IMMessage>());
    }

    @Override
    public void sendRobotMsg(String msg) {
        chatingPresenter.sendRobotMsg(msg);
    }

    @Override
    public void payRedEnvelopChioce(String type,String rid) {
        Intent intent = ReflectUtil.getQtalkServiceRNActivityIntent(PbChatActivity.this);
        if(intent == null){
            return;
        }
        switch (type){
            case Constants.Alipay.RED_ENVELOP_SEND:
                intent.putExtra("module", Constants.RNKey.PAY);
                intent.putExtra("Screen", "SendRedPack");

                intent.putExtra("xmppid", jid);
                intent.putExtra("isChatRoom",isFromChatRoom);
                startActivity(intent);
                break;
            case Constants.Alipay.RED_ENVELOP_DETAIL:
                intent.putExtra("module", Constants.RNKey.PAY);
                intent.putExtra("Screen", "RedPackDetail");
                intent.putExtra("rid", rid);//??????ID
                intent.putExtra("xmppid", jid);
                intent.putExtra("isChatRoom",isFromChatRoom);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void payAuth(final String authInfo) {
        IMPayManager.getInstance().payAuth(this,authInfo,resultStatus -> {
            if(TextUtils.equals(resultStatus, "6001")){
                toast(getString(R.string.atom_ui_user_cancel));
            } else {
                toast(getString(R.string.atom_ui_auth_fail));
            }
        });
    }

    @Override
    public void payOrder(String orderInfo) {
        IMPayManager.getInstance().payOrder(this, orderInfo, resultStatus -> {
            if (TextUtils.equals(resultStatus, "9000")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    connectionUtil.sendEvent(QtalkEvent.PAY_SUCCESS,new Object());
                    toast(getString(R.string.atom_ui_send_red_packet_success));
                }
            } else if(TextUtils.equals(resultStatus, "6001")){
                toast(getString(R.string.atom_ui_user_cancel));
            } else {
                toast(getString(R.string.atom_ui_pay_fail));
            }
        });
    }

    @Override
    public String getBackupInfo() {
        List<RbtMsgBackupInfo> list = new ArrayList<>();

        RbtMsgBackupInfo info = new RbtMsgBackupInfo();
        info.type = 5 * 10000 + 10;
        RbtMsgBackupInfo.Data data = new RbtMsgBackupInfo.Data();
//        if(params != null){
        if (!TextUtils.isEmpty(busiName)) {
            data.bu = busiName;//params.bu;
        }
        if (!TextUtils.isEmpty(supplierId)) {
            data.bsid = supplierId;//sId
        }
//            if(!TextUtils.isEmpty(params.productid)){
//                data.pid = params.productid;//pid
//            }
//        }
        info.data = data;
        // {\"type\":10002,\"data\":{\"rbtMsg\":1}
        RbtMsgBackupInfo info1 = new RbtMsgBackupInfo();
        info1.type = 10002;
        RbtMsgBackupInfo.Data data1 = new RbtMsgBackupInfo.Data();
        data1.rbtMsg = 1;
        info1.data = data1;

        list.add(info);
        list.add(info1);
        return JsonUtils.getGson().toJson(list);
    }

    @Override
    public IMMessage getListFirstItem() {
        if (pbChatViewAdapter.getFirstMessage().getMsgType() == -99) {
            return pbChatViewAdapter.getItem(1);
        } else {
            return pbChatViewAdapter.getFirstMessage();
        }
    }

    @Override
    public IMMessage getListLastItem() {
        return pbChatViewAdapter.getLastMessage();
    }

    @Override
    public void onRefreshComplete() {
        chat_region.onRefreshComplete();
    }

    @Override
    public boolean getSearching() {
        return searching;
    }

    @Override
    public void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }

    @Override
    public int getUnreadMsgCount() {
        return unreadMsgCount.get();
    }

    private void saveChatWindowActLog(String eventId, String desc, String currentPage) {
        LogInfo logInfo = QLog.build(LogConstans.LogType.ACT, LogConstans.LogSubType.CLICK).eventId(eventId).describtion(desc).currentPage(currentPage);
        LogService.getInstance().saveLog(logInfo);
    }

}
