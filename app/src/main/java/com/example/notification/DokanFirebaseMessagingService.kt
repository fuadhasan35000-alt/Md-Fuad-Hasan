package com.example.notification

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.UserRole
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DokanFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        
        // Save FCM token in SharedPreferences and update topic subscriptions
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        // Auto-subscribe to global updates
        subscribeToDefaultTopics()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val notificationTitle = remoteMessage.notification?.title
        val notificationBody = remoteMessage.notification?.body
        val dataMap = remoteMessage.data

        Log.d(TAG, "Message Notification Body: $notificationBody, Data: $dataMap")

        // Role-relevance check (if target_role is specified in FCM data payload)
        val targetRoleStr = dataMap["target_role"]
        if (!targetRoleStr.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val activeUser = db.userDao().getActiveUserSync()
                    if (activeUser != null) {
                        val targetRole = try { UserRole.valueOf(targetRoleStr) } catch (e: Exception) { null }
                        if (targetRole != null) {
                            val isRelevant = when (targetRole) {
                                UserRole.SUPER_ADMIN -> activeUser.role == UserRole.SUPER_ADMIN
                                UserRole.ADMIN -> activeUser.role == UserRole.SUPER_ADMIN || activeUser.role == UserRole.ADMIN
                                UserRole.STAFF -> true // All roles can receive staff-level notifications
                            }
                            if (!isRelevant) {
                                Log.d(TAG, "Notification skipped because targetRole=$targetRole does not match userRole=${activeUser.role}")
                                return@launch
                            }
                        }
                    }

                    // Process actionable push notification
                    PushNotificationHelper.handleFcmPayload(
                        context = applicationContext,
                        title = notificationTitle,
                        message = notificationBody,
                        data = dataMap
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error evaluating role relevance: ${e.message}", e)
                    PushNotificationHelper.handleFcmPayload(
                        context = applicationContext,
                        title = notificationTitle,
                        message = notificationBody,
                        data = dataMap
                    )
                }
            }
        } else {
            // General or non-role-restricted notification
            PushNotificationHelper.handleFcmPayload(
                context = applicationContext,
                title = notificationTitle,
                message = notificationBody,
                data = dataMap
            )
        }
    }

    companion object {
        private const val TAG = "DokanFCM"
        const val PREFS_NAME = "dokan_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"

        const val TOPIC_ALL = "all_users"
        const val TOPIC_LOW_STOCK = "low_stock_alerts"
        const val TOPIC_DUE_PAYMENTS = "due_payment_alerts"
        const val TOPIC_STAFF_APPROVALS = "staff_approval_requests"

        fun subscribeToDefaultTopics() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL)
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_LOW_STOCK)
                FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_DUE_PAYMENTS)
            } catch (e: Throwable) {
                Log.w(TAG, "Could not subscribe to default topics: ${e.message}")
            }
        }

        fun updateRoleTopicSubscriptions(role: UserRole) {
            try {
                when (role) {
                    UserRole.SUPER_ADMIN, UserRole.ADMIN -> {
                        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_STAFF_APPROVALS)
                        FirebaseMessaging.getInstance().subscribeToTopic("role_admin")
                    }
                    UserRole.STAFF -> {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC_STAFF_APPROVALS)
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("role_admin")
                        FirebaseMessaging.getInstance().subscribeToTopic("role_staff")
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed updating topic subscriptions for role $role: ${e.message}")
            }
        }

        fun subscribeToBusinessTopics(businessId: String, branchId: String) {
            try {
                if (businessId.isNotBlank()) {
                    FirebaseMessaging.getInstance().subscribeToTopic("biz_$businessId")
                }
                if (branchId.isNotBlank()) {
                    FirebaseMessaging.getInstance().subscribeToTopic("branch_$branchId")
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed subscribing to business topics: ${e.message}")
            }
        }
    }
}
