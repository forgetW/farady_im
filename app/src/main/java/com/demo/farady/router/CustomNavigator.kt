package com.demo.farady.router

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import com.demo.farady.ConversationListActivity
import com.demo.farady.activity.SingleTaskFlutterActivity
import com.demo.farady.activity.TransparentBackgroundFlutterActivity
import com.qunar.im.ui.activity.PbChatActivity
import com.qunar.im.ui.sdk.QIMSdk
import com.yuxiaor.flutter.g_faraday.Faraday
import com.yuxiaor.flutter.g_faraday.FaradayActivity
import com.yuxiaor.flutter.g_faraday.FaradayNavigator
import com.yuxiaor.flutter.g_faraday.Options
import java.io.Serializable

const val KEY_ARGS = "_args"

object CustomNavigator : FaradayNavigator {

    override fun create(name: String, arguments: Serializable?, options: Options): Intent? {
        val context = Faraday.getCurrentActivity() ?: return null

        if (options.isFlutterRoute) {
            // standard
//            Faraday.getCurrentActivity()?.startActivity(FaradayActivity.build(this, name, arguments))

            // singleTop 模式
//            val intent = FaradayActivity.build(this, name, args, activityClass = SingleTopFlutterActivity::class.java, willTransactionWithAnother = true)
//            Faraday.getCurrentActivity()?.startActivity(intent)

            // singleTask 模式
            val builder = FaradayActivity.builder(name, arguments, false)

            // 你看到的绿色的闪屏就是这个
            builder.backgroundColor = Color.WHITE
            builder.activityClass = SingleTaskFlutterActivity::class.java

            return builder.build(context);
        }


        when (name) {
            "flutter2native" -> {
                val intent = Intent(context, PbChatActivity::class.java)
                intent.putExtra(PbChatActivity.KEY_JID, "nmg17723555741")
//                intent.putExtra(PbChatActivity.KEY_JID, "ba14536ee3b011eabf010242ac110002")
                intent.putExtra(PbChatActivity.KEY_CHAT_TYPE, "0")
                intent.putExtra(PbChatActivity.KEY_IS_CHATROOM, false)
                return intent
            }
//            "flutter2native" -> {
//                return Intent(context, ConversationListActivity::class.java)
//            }
//            "tabContainer" -> {
//                return Intent(context, TabContainerActivity::class.java)
//            }"ImMainActivity" -> {
//                return Intent(context, ImMainActivity::class.java)
//            }
            else -> {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(name)
                intent.putExtra(KEY_ARGS, arguments)
                return intent
            }
        }

    }

    override fun pop(result: Serializable?) {
        val activity = Faraday.getCurrentActivity() ?: return
        if (result != null) {
            activity.setResult(Activity.RESULT_OK, Intent().apply { putExtra(KEY_ARGS, result) })
        }
        activity.finish()

        if (activity is TransparentBackgroundFlutterActivity) {
            activity.overridePendingTransition(0, 0)
        }
    }

    override fun enableSwipeBack(enable: Boolean) {

    }
}