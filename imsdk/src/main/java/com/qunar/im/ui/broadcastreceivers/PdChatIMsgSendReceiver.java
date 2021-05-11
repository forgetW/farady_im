package com.qunar.im.ui.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PdChatIMsgSendReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (onChatMsgClick != null){
            onChatMsgClick.onLeftImageClickEvent(context);
        }
    }

    public OnChatMsgClick onChatMsgClick;

    public void setOnChatMsgClick(OnChatMsgClick onChatMsgClick) {
        this.onChatMsgClick = onChatMsgClick;
    }

    public interface OnChatMsgClick{

        void onLeftImageClickEvent(Context context);

    }

}
