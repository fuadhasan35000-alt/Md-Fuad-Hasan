package com.example.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.AppNotificationEntity
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object PushNotificationHelper {

    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_ACTION = "extra_action"
    const val EXTRA_ENTITY_ID = "extra_entity_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    const val DESTINATION_STOCK = "STOCK"
    const val DESTINATION_CUSTOMERS = "CUSTOMERS"
    const val DESTINATION_STAFF = "STAFF"
    const val DESTINATION_POS = "POS"
    const val DESTINATION_REPORTS = "REPORTS"

    const val ACTION_VIEW_STOCK = "VIEW_STOCK"
    const val ACTION_RESTOCK = "RESTOCK"
    const val ACTION_VIEW_DUE = "VIEW_DUE"
    const val ACTION_APPROVE_STAFF = "APPROVE_STAFF"
    const val ACTION_VIEW_STAFF = "VIEW_STAFF"

    /**
     * Display a Low Stock Alert Notification
     */
    fun showLowStockNotification(
        context: Context,
        productName: String,
        currentStock: Double,
        minStock: Double,
        productId: String,
        businessId: String = "",
        branchId: String = ""
    ) {
        val title = "⚠️ কম স্টক সতর্কতা: $productName"
        val message = "বর্তমান স্টক মাত্র ${formatQuantity(currentStock)} টি (সর্বনিম্ন সীমা: ${formatQuantity(minStock)} টি)। দ্রুত স্টক বৃদ্ধির ব্যবস্থা নিন।"

        // Main Tap Intent -> Opens Stock Screen with Product focus
        val contentIntent = createNavigationIntent(
            context = context,
            destination = DESTINATION_STOCK,
            action = ACTION_VIEW_STOCK,
            entityId = productId,
            requestCode = 1001
        )

        // Action 1: "স্টক দেখুন" (View Stock)
        val viewStockAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "স্টক দেখুন",
            contentIntent
        ).build()

        // Action 2: "স্টক যোগ করুন" (Restock / In)
        val restockIntent = createNavigationIntent(
            context = context,
            destination = DESTINATION_STOCK,
            action = ACTION_RESTOCK,
            entityId = productId,
            requestCode = 1002
        )
        val restockAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "স্টক যোগ করুন",
            restockIntent
        ).build()

        sendNotification(
            context = context,
            notificationId = (productId.hashCode() and 0x7FFFFFFF),
            channelId = NotificationChannelManager.CHANNEL_LOW_STOCK,
            title = title,
            message = message,
            contentIntent = contentIntent,
            actions = listOf(viewStockAction, restockAction),
            type = NotificationType.LOW_STOCK,
            businessId = businessId,
            branchId = branchId,
            entityId = productId,
            actionType = ACTION_VIEW_STOCK,
            targetRole = null // All roles can see stock alerts
        )
    }

    /**
     * Display a Due Payment or New Due Sale Notification
     */
    fun showDuePaymentNotification(
        context: Context,
        customerName: String,
        amount: Double,
        invoiceOrNote: String,
        customerId: String,
        isPaymentReceived: Boolean = false,
        businessId: String = "",
        branchId: String = ""
    ) {
        val title = if (isPaymentReceived) {
            "💵 বকেয়া পরিশোধ জমা: $customerName"
        } else {
            "⚠️ নতুন বাকি বিক্রয়: $customerName"
        }

        val message = if (isPaymentReceived) {
            "কাস্টমার $customerName ৳${amount.toInt()} টাকা বকেয়া পরিশোধ করেছেন। ($invoiceOrNote)"
        } else {
            "কাস্টমার $customerName এর নামে ৳${amount.toInt()} টাকা নতুন বাকি যুক্ত হয়েছে। ($invoiceOrNote)"
        }

        val contentIntent = createNavigationIntent(
            context = context,
            destination = DESTINATION_CUSTOMERS,
            action = ACTION_VIEW_DUE,
            entityId = customerId,
            requestCode = 2001
        )

        val viewDueAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "বাকি খাতা দেখুন",
            contentIntent
        ).build()

        sendNotification(
            context = context,
            notificationId = (customerId.hashCode() and 0x7FFFFFFF) + (if (isPaymentReceived) 1 else 0),
            channelId = NotificationChannelManager.CHANNEL_DUE_PAYMENTS,
            title = title,
            message = message,
            contentIntent = contentIntent,
            actions = listOf(viewDueAction),
            type = NotificationType.DUE_PAYMENT,
            businessId = businessId,
            branchId = branchId,
            entityId = customerId,
            actionType = ACTION_VIEW_DUE,
            targetRole = null
        )
    }

    /**
     * Display a Staff Approval Request Notification (Only for Admins / Super Admins)
     */
    fun showStaffApprovalNotification(
        context: Context,
        staffName: String,
        staffEmail: String,
        roleTitle: String,
        userId: String,
        businessId: String = "",
        branchId: String = ""
    ) {
        val title = "👤 নতুন স্টাফ অনুমোদনের অনুরোধ: $staffName"
        val message = "$staffName ($roleTitle - $staffEmail) একাউন্ট অনুমোদনের জন্য অপেক্ষা করছে।"

        // Main Tap Intent -> Opens Staff Management
        val contentIntent = createNavigationIntent(
            context = context,
            destination = DESTINATION_STAFF,
            action = ACTION_VIEW_STAFF,
            entityId = userId,
            requestCode = 3001
        )

        // Action 1: "অনুমোদন করুন" (Direct Approve Staff)
        val approveIntent = createNavigationIntent(
            context = context,
            destination = DESTINATION_STAFF,
            action = ACTION_APPROVE_STAFF,
            entityId = userId,
            requestCode = 3002
        )
        val approveAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "অনুমোদন করুন",
            approveIntent
        ).build()

        // Action 2: "স্টাফ তালিকা"
        val viewStaffAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "স্টাফ দেখুন",
            contentIntent
        ).build()

        sendNotification(
            context = context,
            notificationId = (userId.hashCode() and 0x7FFFFFFF),
            channelId = NotificationChannelManager.CHANNEL_STAFF_APPROVAL,
            title = title,
            message = message,
            contentIntent = contentIntent,
            actions = listOf(approveAction, viewStaffAction),
            type = NotificationType.STAFF_APPROVAL,
            businessId = businessId,
            branchId = branchId,
            entityId = userId,
            actionType = ACTION_APPROVE_STAFF,
            targetRole = UserRole.ADMIN // Targeted to Admins / Super Admins
        )
    }

    /**
     * Display a General Push Notification
     */
    fun showGeneralNotification(
        context: Context,
        title: String,
        message: String,
        destination: String = "",
        entityId: String = "",
        actionType: String = "",
        businessId: String = "",
        branchId: String = ""
    ) {
        val contentIntent = createNavigationIntent(
            context = context,
            destination = destination.ifBlank { DESTINATION_STOCK },
            action = actionType,
            entityId = entityId,
            requestCode = 4001
        )

        sendNotification(
            context = context,
            notificationId = (UUID.randomUUID().hashCode() and 0x7FFFFFFF),
            channelId = NotificationChannelManager.CHANNEL_GENERAL,
            title = title,
            message = message,
            contentIntent = contentIntent,
            actions = emptyList(),
            type = NotificationType.GENERAL,
            businessId = businessId,
            branchId = branchId,
            entityId = entityId,
            actionType = actionType,
            targetRole = null
        )
    }

    /**
     * Handle incoming remote FCM Data Payload and display actionable notification
     */
    fun handleFcmPayload(
        context: Context,
        title: String?,
        message: String?,
        data: Map<String, String>
    ) {
        val typeStr = data["type"] ?: data["notification_type"] ?: "GENERAL"
        val entityId = data["entity_id"] ?: data["entityId"] ?: ""
        val actionType = data["action_type"] ?: data["actionType"] ?: ""
        val businessId = data["business_id"] ?: ""
        val branchId = data["branch_id"] ?: ""
        val targetRoleStr = data["target_role"] ?: ""

        val finalTitle = title ?: data["title"] ?: "আমার দোকান নোটিফিকেশন"
        val finalMessage = message ?: data["message"] ?: data["body"] ?: ""

        when (typeStr.uppercase()) {
            "LOW_STOCK" -> {
                val stock = data["current_stock"]?.toDoubleOrNull() ?: 0.0
                val minStock = data["min_stock"]?.toDoubleOrNull() ?: 5.0
                showLowStockNotification(
                    context = context,
                    productName = data["product_name"] ?: finalTitle,
                    currentStock = stock,
                    minStock = minStock,
                    productId = entityId,
                    businessId = businessId,
                    branchId = branchId
                )
            }
            "DUE_PAYMENT" -> {
                val amount = data["amount"]?.toDoubleOrNull() ?: 0.0
                val isPayment = data["is_payment"]?.toBoolean() ?: false
                showDuePaymentNotification(
                    context = context,
                    customerName = data["customer_name"] ?: "কাস্টমার",
                    amount = amount,
                    invoiceOrNote = data["note"] ?: "",
                    customerId = entityId,
                    isPaymentReceived = isPayment,
                    businessId = businessId,
                    branchId = branchId
                )
            }
            "STAFF_APPROVAL" -> {
                showStaffApprovalNotification(
                    context = context,
                    staffName = data["staff_name"] ?: "নতুন স্টাফ",
                    staffEmail = data["staff_email"] ?: "",
                    roleTitle = data["role_title"] ?: "স্টাফ",
                    userId = entityId,
                    businessId = businessId,
                    branchId = branchId
                )
            }
            else -> {
                showGeneralNotification(
                    context = context,
                    title = finalTitle,
                    message = finalMessage,
                    destination = data["destination"] ?: DESTINATION_STOCK,
                    entityId = entityId,
                    actionType = actionType,
                    businessId = businessId,
                    branchId = branchId
                )
            }
        }
    }

    private fun sendNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        contentIntent: PendingIntent,
        actions: List<NotificationCompat.Action>,
        type: NotificationType,
        businessId: String,
        branchId: String,
        entityId: String,
        actionType: String,
        targetRole: UserRole?
    ) {
        NotificationChannelManager.createNotificationChannels(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted yet, still record in local DB
                persistNotificationLocally(
                    context, type, title, message, businessId, branchId, entityId, actionType, targetRole
                )
                return
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setColor(0xFF0D7A57.toInt()) // Brand Emerald Green

        // Add action buttons
        for (action in actions) {
            builder.addAction(action)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Handled safely
        }

        // Persist to local In-App Notification Center
        persistNotificationLocally(
            context, type, title, message, businessId, branchId, entityId, actionType, targetRole
        )
    }

    private fun persistNotificationLocally(
        context: Context,
        type: NotificationType,
        title: String,
        message: String,
        businessId: String,
        branchId: String,
        entityId: String,
        actionType: String,
        targetRole: UserRole?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val entity = AppNotificationEntity(
                    businessId = businessId,
                    branchId = branchId,
                    type = type,
                    title = title,
                    message = message,
                    targetRole = targetRole,
                    entityId = entityId,
                    actionType = actionType,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )
                db.notificationDao().insertNotification(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNavigationIntent(
        context: Context,
        destination: String,
        action: String,
        entityId: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
            putExtra(EXTRA_ACTION, action)
            putExtra(EXTRA_ENTITY_ID, entityId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            context,
            requestCode + (entityId.hashCode() and 0x0FFF),
            intent,
            flags
        )
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toInt().toString() else "%.1f".format(qty)
    }
}
