package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPermission
import com.example.ui.AmarDokanViewModel
import com.example.ui.theme.*

@Composable
fun BusinessBranchScreen(
    viewModel: AmarDokanViewModel,
    onBack: () -> Unit
) {
    val businesses by viewModel.businesses.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val currentBusinessId by viewModel.currentBusinessId.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()

    var showAddBusinessDialog by remember { mutableStateOf(false) }
    var showAddBranchDialog by remember { mutableStateOf(false) }

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
                        text = "ব্যবসা ও শাখা ব্যবস্থাপনা",
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
            // 1. Branches Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏢 দোকানের শাখাসমূহ (Branches)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (viewModel.hasPermission(AppPermission.MANAGE_BRANCH)) {
                        TextButton(onClick = { showAddBranchDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন শাখা")
                        }
                    }
                }
            }

            items(branches, key = { it.branchId }) { branch ->
                val isSelected = branch.branchId == currentBranchId
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchBranch(branch.branchId) }
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
                                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                                        Text("বর্তমান শাখা", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("ঠিকানা: ${branch.address.ifBlank { "প্রধান কার্যালয়" }} • ফোন: ${branch.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }

                        if (!isSelected) {
                            OutlinedButton(
                                onClick = { viewModel.switchBranch(branch.branchId) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("সুইচ করুন", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Businesses Section (Multi-business support)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏪 আপনার প্রতিষ্ঠানসমূহ (Businesses)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (viewModel.hasPermission(AppPermission.MANAGE_BRANCH)) {
                        TextButton(onClick = { showAddBusinessDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("নতুন প্রতিষ্ঠান")
                        }
                    }
                }
            }

            items(businesses, key = { it.businessId }) { biz ->
                val isSelected = biz.businessId == currentBusinessId
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PurpleBadgeContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.switchBusiness(biz.businessId) }
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
                                Text(biz.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = PurpleBadge) {
                                        Text("সক্রিয় ব্যবসা", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("মালিক: ${biz.ownerName} • ফোন: ${biz.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }

                        if (!isSelected) {
                            OutlinedButton(
                                onClick = { viewModel.switchBusiness(biz.businessId) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("প্রবেশ করুন", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Branch Dialog
    if (showAddBranchDialog) {
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("নতুন শাখা যুক্ত করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("শাখার নাম *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("শাখার ঠিকানা") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("শাখার ফোন নম্বর") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createBranch(name, address, phone) {
                                showAddBranchDialog = false
                            }
                        }
                    }
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAddBranchDialog = false }) { Text("বাতিল") } }
        )
    }

    // Add Business Dialog
    if (showAddBusinessDialog) {
        var name by remember { mutableStateOf("") }
        var owner by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBusinessDialog = false },
            title = { Text("নতুন ব্যবসা / দোকান তৈরি", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ব্যবসার নাম *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("মালিকের নাম *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("মোবাইল নম্বর *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("ঠিকানা") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && owner.isNotBlank() && phone.isNotBlank()) {
                            viewModel.createBusiness(name, owner, phone, address) {
                                showAddBusinessDialog = false
                            }
                        }
                    }
                ) { Text("তৈরি করুন") }
            },
            dismissButton = { TextButton(onClick = { showAddBusinessDialog = false }) { Text("বাতিল") } }
        )
    }
}
