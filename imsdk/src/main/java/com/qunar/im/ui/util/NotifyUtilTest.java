package com.qunar.im.ui.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.qunar.im.base.common.QunarIMApp;
import com.qunar.im.base.module.Nick;
import com.qunar.im.base.module.RecentConversation;
import com.qunar.im.base.structs.MessageStatus;
import com.qunar.im.ui.R;
import com.qunar.im.ui.broadcastreceivers.NotificationClickReceiver;
import com.qunar.im.utils.ConnectionUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class NotifyUtilTest {

    final static String CHANNEL_ID = "channel_id_1";
    final static String CHANNEL_NAME = "channel_name_1";
    private static long lastMsgTime;

    public static void sendSimpleNotification(Context context) {

        String name = "name";
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //只在Android O之上需要渠道
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            //如果这里用IMPORTANCE_NOENE就需要在系统的设置里面开启渠道，
            //通知才能正常弹出
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        Intent intent = new Intent(context, NotificationClickReceiver.class);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        if (name == null) {
            builder.setSmallIcon(R.drawable.logo)
                    .setContentTitle("recentConversation.getFullname()")
                    .setContentText("lasgMsg")
//                    .setTicker(content)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setOngoing(false)
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                    .setAutoCancel(true);
        } else {
            builder.setSmallIcon(R.drawable.logo)
                    .setContentTitle("name")
                    .setContentText("lasgMsg")
                    .setContentIntent(pendingIntent)
//                    .setTicker(content)
//                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setOngoing(false)
                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                    .setAutoCancel(true);
        }

        mNotificationManager.notify(10, builder.build());
    }

    /**
     * 播放通知声音
     */
    public static void playRingTone(Context context) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone rt = RingtoneManager.getRingtone(context, uri);
        rt.play();
    }

    public static void playVibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        long[] vibrationPattern = new long[]{0, 180, 80, 120};
        // 第一个参数为开关开关的时间，第二个参数是重复次数，振动需要添加权限
        vibrator.vibrate(vibrationPattern, -1);
    }
}
