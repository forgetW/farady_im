package com.qunar.im.ui.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qunar.im.base.common.ConversitionType;
import com.qunar.im.base.common.QunarIMApp;
import com.qunar.im.base.module.RecentConversation;
import com.qunar.im.ui.activity.PbChatActivity;
import com.qunar.im.ui.activity.RobotChatActivity;
import com.qunar.im.ui.activity.RobotExtendChatActivity;
import com.qunar.im.ui.view.bigimageview.tool.utility.ui.ToastUtil;

public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra("AliMeetingDetailConfig")) {
            Log.i("MiPush --onReceive", "onReceive: AliMeetingDetailConfig");
            String aliMeetingDetailConfig = intent.getStringExtra("AliMeetingDetailConfig");
            Intent intent2 = new Intent("com.broadcasereceiver.PdChatItemClickReceiver");
            intent2.putExtra("aliMeetingDetailConfig", aliMeetingDetailConfig);         //向广播接收器传递数据
            QunarIMApp.getContext().sendBroadcast(intent2);      //发送广播
        }else {
            Log.i("MiPush --onReceive", "onReceive: chatClick");
            RecentConversation recentConversation = (RecentConversation) intent.getSerializableExtra("recentConversation");
            recentConvClick(recentConversation, context);
        }
    }

    void recentConvClick(RecentConversation item, Context context) {
//        readAllConversations = true;
        Intent intent = null;
        switch (item.getConversationType()) {

            case ConversitionType.MSG_TYPE_COLLECTION:
                ToastUtil.getInstance()._long(context,"ConversitionType.MSG_TYPE_COLLECTION");
//                intent = new Intent(context, CollectionActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                context.startActivity(intent);
                break;
            case ConversitionType.MSG_TYPE_CONSULT:
            case ConversitionType.MSG_TYPE_CONSULT_SERVER:
            case ConversitionType.MSG_TYPE_GROUP:
            case ConversitionType.MSG_TYPE_CHAT:

                intent = new Intent(context, PbChatActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(PbChatActivity.KEY_UNREAD_MSG_COUNT, item.getUnread_msg_cont());
//                currentJid = item.getId();
                //设置jid 就是当前会话对象

                intent.putExtra(PbChatActivity.KEY_JID, item.getId());
                if (item.getConversationType() == 4) {

                } else if (item.getConversationType() == 5) {
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, item.getRealUser());
                } else {
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, item.getRealUser());
                }
                intent.putExtra(PbChatActivity.KEY_CHAT_TYPE, item.getConversationType() + "");

                //设 置是否是群聊
                boolean isChatRoom = item.getConversationType() == ConversitionType.MSG_TYPE_GROUP;
                intent.putExtra(PbChatActivity.KEY_IS_CHATROOM, isChatRoom);

                intent.putExtra(PbChatActivity.KEY_IS_REMIND, item.getRemind() == 0);

                context.startActivity(intent);
                break;
            case ConversitionType.MSG_TYPE_HEADLINE://qtalk的系统通知消息
                intent = new Intent(context, RobotExtendChatActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(PbChatActivity.KEY_JID, item.getId());
                //设置真实id
                intent.putExtra(PbChatActivity.KEY_REAL_JID, item.getRealUser());
                //设置是否是群聊
                intent.putExtra(PbChatActivity.KEY_IS_CHATROOM, false);

                intent.putExtra(PbChatActivity.KEY_CHAT_TYPE, item.getConversationType() + "");
                intent.putExtra(PbChatActivity.KEY_UNREAD_MSG_COUNT, item.getUnread_msg_cont());
                context.startActivity(intent);
                break;
            case ConversitionType.MSG_TYPE_SUBSCRIPT://qchat的系统通知公告抢单消息 和 qtalk的订阅号消息
                if (item.getId().contains("rbt-qiangdan")) {//qchat抢单消息、众包消息
                    ToastUtil.getInstance()._long(context,"ConversitionType.MSG_TYPE_SUBSCRIPT--rbt-qiangdan");
//                    intent = new Intent(context, QunarWebActvity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                    intent.setData(Uri.parse(QtalkNavicationService.getInstance().getqGrabOrder()));
//                    intent.putExtra(QunarWebActvity.IS_HIDE_BAR, true);
//                    context.startActivity(intent);
//                    //抢单消息 特殊处理 点击后 设置成已读
//                    ConnectionUtil.getInstance().sendSingleAllRead(item.getId(), item.getId(), MessageStatus.STATUS_SINGLE_READED + "");
                } else if (item.getId().contains("rbt-system") || item.getId().contains("rbt-notice")) {//qchat的系统通知消息
                    intent = new Intent(context, RobotExtendChatActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.putExtra(PbChatActivity.KEY_JID, item.getId());
                    //设置真实id
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, item.getRealUser());
                    //设置是否是群聊
                    intent.putExtra(PbChatActivity.KEY_IS_CHATROOM, false);

                    intent.putExtra(PbChatActivity.KEY_CHAT_TYPE, item.getConversationType() + "");
                    intent.putExtra(PbChatActivity.KEY_UNREAD_MSG_COUNT, item.getUnread_msg_cont());
                    context.startActivity(intent);
                } else {
                    intent = new Intent(context, RobotChatActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.putExtra(RobotChatActivity.ROBOT_ID_EXTRA, item.getId());
                    intent.putExtra(PbChatActivity.KEY_JID, item.getId());
                    //设置真实id
                    intent.putExtra(PbChatActivity.KEY_REAL_JID, item.getRealUser());
                    intent.putExtra(PbChatActivity.KEY_UNREAD_MSG_COUNT, item.getUnread_msg_cont());
                    context.startActivity(intent);
                }
                break;
            case ConversitionType.MSG_TYPE_FRIENDS_REQUEST:
                break;
            default:
                break;
        }

    }
}
