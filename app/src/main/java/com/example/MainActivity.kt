package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AmarDokanRepository
import com.example.notification.DokanFirebaseMessagingService
import com.example.notification.NotificationChannelManager
import com.example.notification.PushNotificationHelper
import com.example.ui.AmarDokanViewModel
import com.example.ui.MainApp
import com.example.ui.theme.AmarDokanTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AmarDokanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Notification Channels
        NotificationChannelManager.createNotificationChannels(applicationContext)

        // 2. Auto-subscribe to default FCM broadcast topics
        DokanFirebaseMessagingService.subscribeToDefaultTopics()

        // 3. Initialize Database, Repository, and ViewModel
        val database = AppDatabase.getInstance(applicationContext)
        val repository = AmarDokanRepository(database, applicationContext)
        
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return AmarDokanViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[AmarDokanViewModel::class.java]

        // 4. Handle Notification Intent if app launched from notification tap
        handleNotificationIntent(intent)

        setContent {
            AmarDokanTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val destination = intent.getStringExtra(PushNotificationHelper.EXTRA_DESTINATION)
        val action = intent.getStringExtra(PushNotificationHelper.EXTRA_ACTION)
        val entityId = intent.getStringExtra(PushNotificationHelper.EXTRA_ENTITY_ID)

        if (!destination.isNullOrBlank() || !action.isNullOrBlank()) {
            viewModel.handleNotificationNavigation(destination, action, entityId)
        }
    }
}
