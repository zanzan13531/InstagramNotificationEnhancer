package com.example.instagramnotificationenhancer

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
import androidx.core.graphics.drawable.IconCompat

class GeneralNotificationListener : NotificationListenerService() {

    private val TAG = "NotificationListener"

    // Key: The Shortcut ID (e.g., "shortcut_com.instagram.android_John Doe")
    // Value: A list of recent messages for that conversation.
    private val conversationHistory = mutableMapOf<String, MutableList<Triple<String, Long, String>>>()
    private val MAX_MESSAGES_PER_CONVERSATION = 20

    companion object {
        const val CHANNEL_ID = "reclassified_conversations"
    }

    override fun onCreate() {
        // ... (onCreate is the same)
        super.onCreate()
        val name = "Reclassified Conversations"
        val descriptionText = "Notifications reclassified as conversations"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // ... (onNotificationPosted is the same)
        if (sbn == null) return
        val prefs = getSharedPreferences("ReclassifierPrefs", MODE_PRIVATE)
        if (!prefs.getBoolean(sbn.packageName, false)) { return }

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(NotificationCompat.EXTRA_TITLE)
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()

        if (title != null && text != null && (notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY) == 0) {
            reclassifyAndUpdateNotification(sbn, title, text)
        }
    }

    private fun reclassifyAndUpdateNotification(sbn: StatusBarNotification, title: String, text: String) {
        val packageName = sbn.packageName

        // Parsing Logic (same as before)
        val conversationTitle: String
        val messageSenderName: String
        val messageText: String
        val isGroupChat: Boolean

        val separatorIndex = title.indexOf(": ")
        if (separatorIndex > 0) {
            conversationTitle = title.substring(0, separatorIndex)
            messageSenderName = title.substring(separatorIndex + 2)
            messageText = text
            isGroupChat = true
        } else {
            conversationTitle = title
            messageSenderName = title
            messageText = text
            isGroupChat = false
        }

        // --- REVISED MAPPING: We now use the shortcutId as the key for our history map ---
        val shortcutId = "shortcut_${packageName}_$conversationTitle"

        val history = conversationHistory.getOrPut(shortcutId) { mutableListOf() }
        history.add(Triple(messageText, System.currentTimeMillis(), messageSenderName))

        while (history.size > MAX_MESSAGES_PER_CONVERSATION) {
            history.removeAt(0)
        }

        // ... (MessagingStyle and Builder logic is the same)
        val user = Person.Builder().setName("You").setKey("user_key").build()
        val messagingStyle = NotificationCompat.MessagingStyle(user)
            .setGroupConversation(isGroupChat)
        if (isGroupChat) {
            messagingStyle.setConversationTitle(conversationTitle)
        }
        for (messageData in history) {
            val senderPerson = Person.Builder().setName(messageData.third).build()
            messagingStyle.addMessage(messageData.first, messageData.second, senderPerson)
        }

        val constantNotificationId = conversationTitle.hashCode()
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val shortcutPerson = Person.Builder().setName(conversationTitle).build()
        val shortcut = ShortcutInfoCompat.Builder(this, shortcutId).setLongLived(true).setShortLabel(conversationTitle).setPerson(shortcutPerson).setIntent(launchIntent!!).build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
//        sbn.notification.getLargeIcon()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(IconCompat.createFromIcon(this, sbn.notification.smallIcon)!!)
            .setLargeIcon(sbn.notification.getLargeIcon())
            .setStyle(messagingStyle)
            .setShortcutId(shortcutId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(this).notify(constantNotificationId, builder.build())
//            cancelNotification(sbn.key)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Failed to post notification. Error: ${e.message}")
        }
    }

    // --- THIS IS THE CORRECTED onNotificationRemoved ---
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        // THE FIX: Only clear history if the notification being removed is from OUR app.
        if (sbn.packageName == this.packageName) {
            // Our reclassified notifications have a shortcutId. We use this to find the history.
            val shortcutId = sbn.notification.shortcutId
            if (shortcutId != null) {
                // Remove the history associated with this shortcutId.
                conversationHistory.remove(shortcutId)
                Log.d(TAG, "User dismissed our notification. Cleared history for: $shortcutId")
            }
        }
    }
}