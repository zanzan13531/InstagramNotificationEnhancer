package com.example.instagramnotificationenhancer;

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person as PersonCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat

class GeneralNotificationListener : NotificationListenerService() {

    // Define a constant for our channel ID
    companion object {
        const val CHANNEL_ID = "reclassified_conversations"
    }

    override fun onCreate() {
        super.onCreate()
        // IMPORTANT: Create the Notification Channel when the service starts.
        // This is required for Android 8.0 (API 26) and higher.
        val name = "Reclassified Conversations"
        val descriptionText = "Notifications reclassified as conversations"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // --- THIS IS THE KEY CHANGE ---
        // 1. Get the user's saved preferences
        val prefs = getSharedPreferences("ReclassifierPrefs", MODE_PRIVATE)
        val packageName = sbn.packageName

        // 2. Check if the user has enabled this app. If not, do nothing.
        if (!prefs.getBoolean(packageName, false)) {
            return
        }
        // The old "if (sbn.packageName != "com.instagram.android")" check is now gone!
        // --- END OF KEY CHANGE ---


        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)

        if (title != null && text != null && (notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0) {
            reclassifyAsSingleNotification(sbn, title, text.toString())
        }
    }

    private fun reclassifyAsSingleNotification(sbn: StatusBarNotification, senderName: String, messageText: String) {
        val packageName = sbn.packageName
        val senderPerson = Person.Builder().setName(senderName).build()
        val user = Person.Builder().setName("You").setKey("user_key").build()
        val messagingStyle = Notification.MessagingStyle(user)
            .setConversationTitle(senderName)
            .addMessage(messageText, System.currentTimeMillis(), senderPerson)

        val shortcutId = "shortcut_${packageName}_$senderName"
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId)
            .setLongLived(true)
            .setShortLabel(senderName)
            .setPerson(PersonCompat.Builder().setName(senderName).build())
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)

        // Use the CHANNEL_ID we defined and created
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(sbn.notification.smallIcon)
            .setStyle(messagingStyle)
            .setShortcutId(shortcutId)

        val uniqueNotificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(this).notify(uniqueNotificationId, builder.build())
        cancelNotification(sbn.key)
    }

    // onNotificationRemoved can remain empty
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}