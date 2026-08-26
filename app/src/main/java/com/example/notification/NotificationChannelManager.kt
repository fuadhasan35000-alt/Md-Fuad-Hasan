package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * Creates and registers high-priority notification channels for Amar Dokan.
 */
object NotificationChannelManager {

    const val CHANNEL_LOW_STOCK = "channel_low_stock"
    const val CHANNEL_DUE_PAYMENTS = "channel_due_payments"
    const val CHANNEL_STAFF_APPROVAL = "channel_staff_approvals"
    const val CHANNEL_GENERAL = "channel_general"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Low Stock Alert Channel
            val lowStockChannel = NotificationChannel(
                CHANNEL_LOW_STOCK,
                "কম স্টক সতর্কতা (Low Stock Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "দোকানের পণ্যের স্টক কমে গেলে জরুরি নোটিফিকেশন"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            // 2. Due Payments Channel
            val dueChannel = NotificationChannel(
                CHANNEL_DUE_PAYMENTS,
                "বকেয়া ও পেমেন্ট অ্যালার্ট (Due & Payments)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নতুন বকেয়া বিক্রি ও কাস্টমার দেনা পরিশোধের নোটিফিকেশন"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            // 3. Staff Approval Channel
            val staffChannel = NotificationChannel(
                CHANNEL_STAFF_APPROVAL,
                "স্টাফ অনুমোদন অনুরোধ (Staff Approval)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নতুন স্টাফ রেজিস্ট্রেশন ও অ্যাডমিন অনুমোদনের নোটিফিকেশন"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            // 4. General Notifications Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "সাধারণ বিজ্ঞপ্তি (General Updates)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "সিস্টেম ও দৈনন্দিন ব্যবসায়ের সাধারণ আপডেট"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(lowStockChannel, dueChannel, staffChannel, generalChannel)
            )
        }
    }
}
