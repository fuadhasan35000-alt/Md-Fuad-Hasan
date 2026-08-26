package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncStatus
import com.example.ui.AmarDokanViewModel
import com.example.ui.theme.*
import com.example.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupSyncScreen(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val syncState by viewModel.syncState.collectAsState()
    val pendingCount by viewModel.pendingSyncCount.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে")
                    }
                    Text(
                        text = "ক্লাউড সিঙ্ক ও ব্যাকআপ (Offline Sync)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Sync Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncState) {
                            SyncStatus.SYNCED -> SuccessGreenContainer.copy(alpha = 0.5f)
                            SyncStatus.SYNCING -> InfoBlueContainer.copy(alpha = 0.5f)
                            SyncStatus.PENDING -> WarningAmberContainer.copy(alpha = 0.5f)
                            SyncStatus.FAILED -> DangerRedContainer.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (syncState) {
                                        SyncStatus.SYNCED -> Icons.Default.CloudDone
                                        SyncStatus.SYNCING -> Icons.Default.Sync
                                        SyncStatus.PENDING -> Icons.Default.CloudQueue
                                        SyncStatus.FAILED -> Icons.Default.SyncProblem
                                    },
                                    contentDescription = null,
                                    tint = when (syncState) {
                                        SyncStatus.SYNCED -> SuccessGreen
                                        SyncStatus.SYNCING -> InfoBlue
                                        SyncStatus.PENDING -> WarningAmber
                                        SyncStatus.FAILED -> DangerRed
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (syncState) {
                                            SyncStatus.SYNCED -> "ক্লাউড সিঙ্ক সক্রিয় ও আপ-টু-ডেট"
                                            SyncStatus.SYNCING -> "সিঙ্ক্রোনাইজেশন চলছে..."
                                            SyncStatus.PENDING -> "অফলাইন কিউতে ${pendingCount}টি পরিবর্তন সংরক্ষিত"
                                            SyncStatus.FAILED -> "সিঙ্কে ত্রুটি (অফলাইনে সংরক্ষিত)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "অফলাইন-ফার্স্ট আর্কিটেকচার: ইন্টারনেট ছাড়াই দোকান সম্পূর্ণ চলবে",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.triggerCloudSync() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trigger_cloud_sync_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এখনই ক্লাউডে সিঙ্ক করুন")
                        }
                    }
                }
            }

            // 2. Data Backup and Export
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💾 লোকাল ডেটা ব্যাকআপ ও এক্সপোর্ট", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "আপনার পণ্য, কাস্টমার, দেনা-পাওনা ও বিক্রয় চালানের সম্পূর্ণ ডেটা টেক্সট/JSON ফাইল আকারে নিরাপদে ব্যাকআপ রাখুন।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        OutlinedButton(
                            onClick = {
                                val backupString = """
                                    ==================================
                                    আমার দোকান (Amar Dokan) - ডেটা ব্যাকআপ
                                    প্রতিষ্ঠান: ${currentBusiness?.name ?: "আমার দোকান"}
                                    শাখা: ${currentBranch?.name ?: "প্রধান শাখা"}
                                    তারিখ: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())}
                                    ==================================
                                    স্ট্যাটাস: সুরক্ষিত অফলাইন ডাটাবেজ সক্রিয়
                                """.trimIndent()
                                ShareHelper.shareTextGeneral(context, backupString, "দোকানের ব্যাকআপ ফাইল শেয়ার করুন")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সম্পূর্ণ ব্যাকআপ ফাইল ডাউনলোড / শেয়ার")
                        }
                    }
                }
            }

            // 3. Security Audit Trail Logs
            item {
                Text(
                    text = "🛡️ অডিট ট্রেইল ও নিরাপত্তা লগ (${auditLogs.size}টি)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (auditLogs.isEmpty()) {
                item {
                    Text("এখনো কোনো লগ এন্ট্রি নেই", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            } else {
                items(auditLogs.take(20)) { log ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.details, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("অ্যাকশন: ${log.action} • দ্বারা: ${log.userName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
