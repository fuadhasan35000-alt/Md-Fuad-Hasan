package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPermission
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.AmarDokanViewModel
import com.example.ui.theme.*

@Composable
fun StaffManagementScreen(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var editingUserForEmail by remember { mutableStateOf<UserEntity?>(null) }

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
                        text = "স্টাফ ও ইউজার ম্যানেজমেন্ট",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewModel.hasPermission(AppPermission.MANAGE_STAFF)) {
                ExtendedFloatingActionButton(
                    onClick = { showAddStaffDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("নতুন স্টাফ যোগ", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_staff_fab")
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Pending Approvals Section
            if (pendingUsers.isNotEmpty()) {
                item {
                    Text(
                        text = "⏳ অনুমোদনের অপেক্ষায় (${pendingUsers.size} জন)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = WarningAmber
                    )
                }

                items(pendingUsers, key = { it.userId }) { user ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmberContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("ইমেইল: ${user.email} | ফোন: ${user.phone}", fontSize = 12.sp)
                                    Text("পদবী: ${user.role.getDisplayNameBn()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Surface(shape = RoundedCornerShape(6.dp), color = WarningAmber) {
                                    Text("অপেক্ষমাণ", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.rejectStaff(user.userId) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                ) {
                                    Text("বাতিল / ব্লক")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.approveStaff(user.userId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অনুমোদন করুন")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Staff List
            item {
                Text(
                    text = "👥 সক্রিয় ইউজার ও স্টাফ তালিকা (${users.size} জন)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(users, key = { it.userId }) { user ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (user.role) {
                                        UserRole.SUPER_ADMIN -> PurpleBadgeContainer
                                        UserRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                                        UserRole.STAFF -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = user.role.getDisplayNameBn(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (user.role) {
                                            UserRole.SUPER_ADMIN -> PurpleBadge
                                            UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                            UserRole.STAFF -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("ইমেইল: ${user.email} • মোবা: ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.hasPermission(AppPermission.MANAGE_STAFF) || user.userId == viewModel.currentUser.collectAsState().value?.userId) {
                                IconButton(onClick = { editingUserForEmail = user }) {
                                    Icon(Icons.Default.Edit, contentDescription = "ইমেইল এডিট", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (user.role != UserRole.SUPER_ADMIN && viewModel.hasPermission(AppPermission.MANAGE_STAFF)) {
                                IconButton(onClick = { viewModel.rejectStaff(user.userId) }) {
                                    Icon(Icons.Default.Block, contentDescription = "ব্লক করুন", tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingUserForEmail?.let { targetUser ->
        var newEmailInput by remember(targetUser) { mutableStateOf(targetUser.email) }
        var emailError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingUserForEmail = null },
            title = { Text("ইমেইল পরিবর্তন করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ইউজার: ${targetUser.name} (${targetUser.role.getDisplayNameBn()})", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newEmailInput,
                        onValueChange = { newEmailInput = it; emailError = null },
                        label = { Text("নতুন ইমেইল অ্যাড্রেস") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (emailError != null) {
                        Text(emailError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmailInput.isBlank() || !newEmailInput.contains("@")) {
                            emailError = "সঠিক ইমেইল ঠিকানা দিন"
                            return@Button
                        }
                        viewModel.updateUserEmail(targetUser.userId, newEmailInput.trim())
                        editingUserForEmail = null
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUserForEmail = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showAddStaffDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(UserRole.STAFF) }
        var branchId by remember { mutableStateOf(currentBranchId) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text("নতুন স্টাফ রেজিস্টার করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = { Text("স্টাফের নাম *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        label = { Text("ইমেইল অ্যাড্রেস *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; error = null },
                        label = { Text("মোবাইল নম্বর *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it; error = null },
                        label = { Text("পাসওয়ার্ড *") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("পদবী নির্বাচন করুন:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == UserRole.STAFF,
                            onClick = { role = UserRole.STAFF },
                            label = { Text("স্টাফ / সেলস") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = role == UserRole.ADMIN,
                            onClick = { role = UserRole.ADMIN },
                            label = { Text("ম্যানেজার / অ্যাডমিন") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (error != null) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || phone.isBlank() || pass.isBlank()) {
                            error = "সকল তথ্য সঠিকভাবে পূরণ করুন"
                            return@Button
                        }
                        viewModel.createStaff(name, email, phone, branchId, role, pass) {
                            showAddStaffDialog = false
                        }
                    }
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
