package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchEntity
import com.example.data.model.BusinessEntity
import com.example.data.model.SyncStatus
import com.example.data.model.UserEntity
import com.example.ui.AppDestination
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokanTopBar(
    currentBusiness: BusinessEntity?,
    currentBranch: BranchEntity?,
    currentUser: UserEntity?,
    syncStatus: SyncStatus,
    pendingSyncCount: Int,
    unreadNotificationCount: Int = 0,
    onMenuClick: () -> Unit,
    onBranchClick: () -> Unit,
    onAiClick: () -> Unit,
    onSyncClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Menu & Business/Branch Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("top_menu_button")
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "মেনু",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(
                        modifier = Modifier.clickable { onBranchClick() }
                    ) {
                        Text(
                            text = currentBusiness?.name ?: "আমার দোকান",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "🏢 ${currentBranch?.name ?: "প্রধান শাখা"}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (currentUser != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = currentUser.role.getDisplayNameBn(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Sync Badge & AI Assistant & Notification
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sync Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (syncStatus) {
                            SyncStatus.SYNCED -> SuccessGreenContainer
                            SyncStatus.SYNCING -> InfoBlueContainer
                            SyncStatus.PENDING -> WarningAmberContainer
                            SyncStatus.FAILED -> DangerRedContainer
                        },
                        modifier = Modifier
                            .clickable { onSyncClick() }
                            .testTag("sync_status_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (syncStatus) {
                                    SyncStatus.SYNCED -> Icons.Default.CloudDone
                                    SyncStatus.SYNCING -> Icons.Default.Sync
                                    SyncStatus.PENDING -> Icons.Default.CloudQueue
                                    SyncStatus.FAILED -> Icons.Default.SyncProblem
                                },
                                contentDescription = "সিঙ্ক অবস্থা",
                                modifier = Modifier.size(16.dp),
                                tint = when (syncStatus) {
                                    SyncStatus.SYNCED -> SuccessGreen
                                    SyncStatus.SYNCING -> InfoBlue
                                    SyncStatus.PENDING -> WarningAmber
                                    SyncStatus.FAILED -> DangerRed
                                }
                            )
                            if (pendingSyncCount > 0) {
                                Text(
                                    text = "$pendingSyncCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                        }
                    }

                    // Notification Bell Button with Badge
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.testTag("top_notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (unreadNotificationCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "বিজ্ঞপ্তি ও অ্যালার্ট",
                                tint = if (unreadNotificationCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // AI Assistant Button
                    IconButton(
                        onClick = onAiClick,
                        modifier = Modifier.testTag("ai_assistant_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "এআই সহকারী",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DokanBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        val items = listOf(
            Triple(AppDestination.HOME, "হোম", Icons.Filled.Store to Icons.Outlined.Store),
            Triple(AppDestination.STOCK, "স্টক", Icons.Filled.Inventory2 to Icons.Outlined.Inventory2),
            Triple(AppDestination.POS, "বিক্রি (POS)", Icons.Filled.PointOfSale to Icons.Outlined.PointOfSale),
            Triple(AppDestination.CUSTOMERS, "কাস্টমার", Icons.Filled.People to Icons.Outlined.PeopleOutline),
            Triple(AppDestination.REPORTS, "রিপোর্ট", Icons.Filled.Analytics to Icons.Outlined.Analytics)
        )

        items.forEach { (destination, label, iconPair) ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) iconPair.first else iconPair.second,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_item_${destination.name.lowercase()}")
            )
        }
    }
}
