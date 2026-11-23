package app.smssweeper

import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony

// These components exist so Android lets the app become the default SMS app
// (the only way an app may delete messages). While default, we must store
// incoming SMS ourselves or they would be lost.

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, parts[0].originatingAddress)
            put(Telephony.Sms.BODY, parts.joinToString("") { it.messageBody.orEmpty() })
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }
}

// ponytail: incoming MMS is not stored; switch back to your normal SMS app after cleaning.
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
