package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotificationEntity
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import com.example.ui.AmarDokanViewModel
import com.example.ui.AppDestination
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    viewModel: AmarDokanViewModel,
    onDismiss: () -> Unit,
    onNavigateToDestination: (AppDestination) -> Unit,
    onOpenStaffDialog: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        if (selectedFilter == null) {
            notifications
        } else {
            notifications.filter { it.type == selectedFilter }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("notification_center_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "নোটিফিকেশন",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount টি নতুন বিজ্ঞপ্তি" else "সব বিজ্ঞপ্তি পড়া হয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Test Simulator Button
                    IconButton(
                        onClick = { showTestDialog = true },
                        modifier = Modifier.testTag("btn_test_notification")
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = "টেস্ট নোটিফিকেশন",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Mark all as read
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("btn_mark_all_read")
                        ) {
                            Icon(
                                Icons.Outlined.DoneAll,
                                contentDescription = "সব পড়া হয়েছে",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Clear all
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllNotifications() },
                            modifier = Modifier.testTag("btn_clear_all_notifications")
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "মুছে ফেলুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("সব (${notifications.size})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AllInclusive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                item {
                    val count = notifications.count { it.type == NotificationType.LOW_STOCK }
                    FilterChip(
                        selected = selectedFilter == NotificationType.LOW_STOCK,
                        onClick = { selectedFilter = NotificationType.LOW_STOCK },
                        label = { Text("কম স্টক ($count)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                item {
                    val count = notifications.count { it.type == NotificationType.DUE_PAYMENT }
                    FilterChip(
                        selected = selectedFilter == NotificationType.DUE_PAYMENT,
                        onClick = { selectedFilter = NotificationType.DUE_PAYMENT },
                        label = { Text("বকেয়া ও পেমেন্ট ($count)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                item {
                    val count = notifications.count { it.type == NotificationType.STAFF_APPROVAL }
                    FilterChip(
                        selected = selectedFilter == NotificationType.STAFF_APPROVAL,
                        onClick = { selectedFilter = NotificationType.STAFF_APPROVAL },
                        label = { Text("স্টাফ ($count)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            // Notifications List
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "কোনো নোটিফিকেশন নেই",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "নতুন কোনো অ্যালার্ট বা জরুরি আপডেট এলে এখানে দেখতে পাবেন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showTestDialog = true },
                            modifier = Modifier.testTag("btn_empty_send_test")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("টেস্ট পুশ নোটিফিকেশন পাঠান")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notif ->
                        NotificationItemCard(
                            notification = notif,
                            onMarkAsRead = { viewModel.markNotificationAsRead(notif.id) },
                            onDelete = { viewModel.deleteNotification(notif.id) },
                            onActionClick = {
                                viewModel.markNotificationAsRead(notif.id)
                                onDismiss()
                                when (notif.type) {
                                    NotificationType.LOW_STOCK -> onNavigateToDestination(AppDestination.STOCK)
                                    NotificationType.DUE_PAYMENT -> onNavigateToDestination(AppDestination.CUSTOMERS)
                                    NotificationType.STAFF_APPROVAL -> {
                                        if (notif.actionType == "APPROVE_STAFF" && notif.entityId.isNotBlank()) {
                                            viewModel.approveStaff(notif.entityId)
                                        }
                                        onOpenStaffDialog()
                                    }
                                    NotificationType.GENERAL -> onNavigateToDestination(AppDestination.HOME)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Test Notification Dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            icon = { Icon(Icons.Filled.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("FCM পুশ নোটিফিকেশন টেস্ট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "নিচের যেকোনো অপশনে ক্লিক করে অ্যাপে রিয়েল-টাইম পুশ নোটিফিকেশন ও অ্যাকশনেবল বাটনের কার্যকারিতা পরীক্ষা করুন:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            viewModel.triggerTestNotification(NotificationType.LOW_STOCK)
                            showTestDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_trigger_low_stock")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("১. কম স্টক অ্যালার্ট (Low Stock)")
                    }

                    Button(
                        onClick = {
                            viewModel.triggerTestNotification(NotificationType.DUE_PAYMENT)
                            showTestDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_trigger_due_payment")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("২. নতুন বাকি ও পেমেন্ট (Due & Payment)")
                    }

                    Button(
                        onClick = {
                            viewModel.triggerTestNotification(NotificationType.STAFF_APPROVAL)
                            showTestDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        modifier = Modifier.fillMaxWidth().testTag("btn_trigger_staff_approval")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("৩. স্টাফ অনুমোদন অনুরোধ (Staff Approval)")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.triggerTestNotification(NotificationType.GENERAL)
                            showTestDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_trigger_general")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("৪. ক্লাউড সিঙ্ক সফল নোটিফিকেশন")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

@Composable
private fun NotificationItemCard(
    notification: AppNotificationEntity,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onActionClick: () -> Unit
) {
    val (icon, tintColor, badgeBg, actionLabel) = when (notification.type) {
        NotificationType.LOW_STOCK -> Quad(
            Icons.Default.WarningAmber,
            Color(0xFFD32F2F),
            Color(0xFFFFEBEE),
            "স্টক দেখুন"
        )
        NotificationType.DUE_PAYMENT -> Quad(
            Icons.Default.AccountBalanceWallet,
            Color(0xFFE65100),
            Color(0xFFFFF3E0),
            "বাকি খাতা"
        )
        NotificationType.STAFF_APPROVAL -> Quad(
            Icons.Default.PersonAdd,
            Color(0xFF1976D2),
            Color(0xFFE3F2FD),
            "অনুমোদন / স্টাফ"
        )
        NotificationType.GENERAL -> Quad(
            Icons.Default.Info,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            "দেখুন"
        )
    }

    val timeFormatted = remember(notification.createdAt) {
        formatNotificationTime(notification.createdAt)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActionClick() }
            .testTag("notif_item_${notification.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.5.dp else 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Leading Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onActionClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "মুছে ফেলুন",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun formatNotificationTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "এইমাত্র"
        minutes < 60 -> "$minutes মিনিট আগে"
        hours < 24 -> "$hours ঘন্টা আগে"
        days == 1L -> "গতকাল"
        days < 7 -> "$days দিন আগে"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
