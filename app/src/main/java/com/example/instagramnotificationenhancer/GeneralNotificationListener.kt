package com.example.instagramnotificationenhancer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat

class GeneralNotificationListener : NotificationListenerService() {

    // A "TAG" for our log messages so we can easily find them in Logcat
    private val TAG = "NotificationListener"

    companion object {
        const val CHANNEL_ID = "reclassified_conversations"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate. Creating notification channel...")
        val name = "Reclassified Conversations"
        val descriptionText = "Notifications reclassified as conversations"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 1. First log: Did we receive the notification?
        Log.d(TAG, "onNotificationPosted received a notification.")

        if (sbn == null) {
            Log.d(TAG, "StatusBarNotification object was null, ignoring.")
            return
        }

        val packageName = sbn.packageName
        // 2. Log the package name
        Log.d(TAG, "Notification is from package: $packageName")

        val prefs = getSharedPreferences("ReclassifierPrefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(packageName, false)
        // 3. Log whether the user has enabled this app
        Log.d(TAG, "Checking prefs for this package. Is it enabled? $isEnabled")

        if (!isEnabled) {
            Log.d(TAG, "App not enabled by user. Ignoring notification.")
            return
        }

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)

        // 4. Log the content we extracted
        Log.d(TAG, "Notification content: Title='$title', Text='$text'")

        if (title != null && text != null && (notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0) {
            // 5. Log that we are proceeding to reclassify
            Log.d(TAG, "Notification has title and text, proceeding to reclassify.")
            reclassifyAsSingleNotification(sbn, title, text.toString())
        } else {
            Log.d(TAG, "Notification did not meet criteria (missing title/text or is a group summary).")
        }
    }

    private fun reclassifyAsSingleNotification(sbn: StatusBarNotification, senderName: String, messageText: String) {
        // 6. Log that we've entered this function
        Log.d(TAG, "Inside reclassifyAsSingleNotification for sender: $senderName")

        val packageName = sbn.packageName

        val senderPerson = Person.Builder().setName(senderName).build()
        val user = Person.Builder().setName("You").setKey("user_key").build()
        val messagingStyle = NotificationCompat.MessagingStyle(user)
            .setConversationTitle(senderName)
            .addMessage(messageText, System.currentTimeMillis(), senderPerson)

        val shortcutId = "shortcut_${packageName}_$senderName"
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setLongLived(true)
            .setShortLabel(senderName)
            .setPerson(senderPerson)
            .setIntent(launchIntent!!)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

//        val smallIconResId = sbn.notification.icon
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(smallIconResId)
            .setSmallIcon(R.drawable.ic_reclassified_notification)
            .setStyle(messagingStyle)
            .setShortcutId(shortcutId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val uniqueNotificationId = System.currentTimeMillis().toInt()

        try {
            // 7. Log right before we attempt to post the notification
            Log.d(TAG, "Attempting to post new notification with id $uniqueNotificationId")
            NotificationManagerCompat.from(this).notify(uniqueNotificationId, builder.build())
            Log.d(TAG, "NotificationManagerCompat.notify() called successfully.")
        } catch (e: SecurityException) {
            Log.e(TAG, "CRITICAL ERROR: SecurityException on posting notification: ${e.message}")
        }

        // add back in later!!!!!!!!!!!!!!!!!!!!!!!
//        cancelNotification(sbn.key)
        // 8. Final log to confirm the entire function completed
        Log.d(TAG, "Original notification cancelled. Function finished.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // This log is very useful to confirm the service started correctly
        Log.d(TAG, "Notification Listener connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // This log tells you if the service was stopped for any reason
        Log.d(TAG, "Notification Listener disconnected.")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // This is less critical for debugging but can be useful
        // Log.d(TAG, "A notification was removed: ${sbn?.packageName}")
    }
}