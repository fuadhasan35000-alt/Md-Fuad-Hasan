package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.model.AppPermission
import com.example.data.model.CustomerEntity
import com.example.ui.AmarDokanViewModel
import com.example.ui.components.AddEditCustomerDialog
import com.example.ui.components.CollectDueDialog
import com.example.ui.theme.*
import com.example.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: AmarDokanViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBusinessId by viewModel.currentBusinessId.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyDue by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var customerForEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForDuePayment by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery, filterOnlyDue) {
        customers.filter { c ->
            val matchesSearch = searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery, ignoreCase = true)
            val matchesDue = !filterOnlyDue || c.totalDue > 0
            matchesSearch && matchesDue
        }
    }

    val totalDueSum = remember(customers) { customers.sumOf { it.totalDue } }

    Scaffold(
        floatingActionButton = {
            if (viewModel.hasPermission(AppPermission.EDIT_CUSTOMERS)) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("নতুন কাস্টমার", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_customer_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Total Due Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalDueSum > 0) DangerRedContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "দোকানের মোট বকেয়া (বাকি খাতা)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = "৳${"%.0f".format(totalDueSum)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (totalDueSum > 0) DangerRed else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${customers.count { it.totalDue > 0 }} জন বাকিদার",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DangerRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("কাস্টমারের নাম বা মোবাইল নম্বর খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !filterOnlyDue,
                    onClick = { filterOnlyDue = false },
                    label = { Text("সকল কাস্টমার (${customers.size})") }
                )
                FilterChip(
                    selected = filterOnlyDue,
                    onClick = { filterOnlyDue = true },
                    label = { Text("⚠️ শুধু বকেয়া আছে") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DangerRedContainer,
                        selectedLabelColor = DangerRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customers List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "কোনো কাস্টমার পাওয়া যায়নি" else "কোনো কাস্টমার যুক্ত করা হয়নি",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers, key = { it.localId }) { customer ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_card_${customer.localId}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customer.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        )
                                        Text(
                                            text = "মোবাইল: ${customer.phone}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        if (customer.address.isNotBlank()) {
                                            Text(
                                                text = "ঠিকানা: ${customer.address}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (customer.totalDue > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = DangerRedContainer
                                            ) {
                                                Text(
                                                    text = "বকেয়া ৳${customer.totalDue}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = DangerRed,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SuccessGreenContainer
                                            ) {
                                                Text(
                                                    text = "পরিশোধিত",
                                                    fontWeight = FontWeight.Bold,
                                                    color = SuccessGreen,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "মোট কেনাকাটা: ৳${customer.totalPurchase}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (customer.totalDue > 0) {
                                            // WhatsApp Due Reminder Button
                                            IconButton(
                                                onClick = {
                                                    val msg = ShareHelper.formatDueReminderText(
                                                        businessName = currentBusiness?.name ?: "আমার দোকান",
                                                        phone = currentBusiness?.phone ?: "",
                                                        customer = customer
                                                    )
                                                    ShareHelper.shareViaWhatsApp(context, customer.phone, msg)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = "হোয়াটসঅ্যাপ তাগাদা", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                            }

                                            // Collect Due Button
                                            Button(
                                                onClick = { customerForDuePayment = customer },
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("collect_due_btn_${customer.localId}")
                                            ) {
                                                Text("জমা নিন", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        IconButton(
                                            onClick = { customerForEdit = customer },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddEditCustomerDialog(
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { showAddDialog = false },
            onSave = { cust ->
                viewModel.saveCustomer(cust) {
                    showAddDialog = false
                }
            }
        )
    }

    if (customerForEdit != null) {
        AddEditCustomerDialog(
            initialCustomer = customerForEdit,
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { customerForEdit = null },
            onSave = { cust ->
                viewModel.saveCustomer(cust) {
                    customerForEdit = null
                }
            }
        )
    }

    if (customerForDuePayment != null) {
        CollectDueDialog(
            customer = customerForDuePayment!!,
            onDismiss = { customerForDuePayment = null },
            onConfirm = { amount, method, note ->
                viewModel.recordDuePayment(customerForDuePayment!!, amount, method, note)
                customerForDuePayment = null
            }
        )
    }
}
