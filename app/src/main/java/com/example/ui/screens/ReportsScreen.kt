package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMethod
import com.example.data.model.SaleEntity
import com.example.ui.AmarDokanViewModel
import com.example.ui.ReportPeriod
import com.example.ui.components.InvoiceDialog
import com.example.ui.theme.*
import com.example.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AmarDokanViewModel
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
    var selectedBranchFilter by remember { mutableStateOf("") } // "" means all

    val activeInvoiceSale by viewModel.selectedSaleForInvoice.collectAsState()
    val activeInvoiceItems by viewModel.invoiceSaleItems.collectAsState()
    var showInvoiceDialog by remember { mutableStateOf(false) }

    // Compute start & end timestamps for filter
    val (startTime, endTime) = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        when (selectedPeriod) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to (start + 24 * 60 * 60 * 1000)
            }
            ReportPeriod.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to (start + 24 * 60 * 60 * 1000)
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                start to now + (24 * 60 * 60 * 1000)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                val start = cal.timeInMillis
                start to now + (24 * 60 * 60 * 1000)
            }
            ReportPeriod.ALL_TIME -> {
                0L to Long.MAX_VALUE
            }
        }
    }

    // Filter sales and expenses
    val filteredSales = remember(sales, startTime, endTime, selectedBranchFilter) {
        sales.filter { s ->
            val matchesTime = s.createdAt in startTime..endTime
            val matchesBranch = selectedBranchFilter.isBlank() || s.branchId == selectedBranchFilter
            matchesTime && matchesBranch
        }
    }

    val filteredExpenses = remember(expenses, startTime, endTime, selectedBranchFilter) {
        expenses.filter { e ->
            val matchesTime = e.createdAt in startTime..endTime
            val matchesBranch = selectedBranchFilter.isBlank() || e.branchId == selectedBranchFilter
            matchesTime && matchesBranch
        }
    }

    val totalSalesAmount = remember(filteredSales) { filteredSales.sumOf { it.total } }
    val totalCollected = remember(filteredSales) { filteredSales.sumOf { it.paid } }
    val totalDue = remember(filteredSales) { filteredSales.sumOf { it.due } }
    val totalExpenseAmount = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }
    val grossProfit = remember(totalSalesAmount, totalExpenseAmount) {
        (totalSalesAmount * 0.20) - totalExpenseAmount
    }

    val cashSales = remember(filteredSales) {
        filteredSales.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.paid }
    }
    val digitalSales = remember(filteredSales) {
        filteredSales.filter { it.paymentMethod != PaymentMethod.CASH }.sumOf { it.paid }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Selectors
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReportPeriod.values()) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period.titleBn, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("report_period_${period.name.lowercase()}")
                    )
                }
            }
        }

        // Branch filter if multiple branches exist
        if (branches.size > 1) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedBranchFilter.isBlank(),
                            onClick = { selectedBranchFilter = "" },
                            label = { Text("সকল শাখা (${branches.size})") }
                        )
                    }
                    items(branches) { br ->
                        FilterChip(
                            selected = selectedBranchFilter == br.branchId,
                            onClick = { selectedBranchFilter = br.branchId },
                            label = { Text(br.name) }
                        )
                    }
                }
            }
        }

        // Profit & Revenue Big Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "${selectedPeriod.titleBn} বিক্রয় ও লাভের সারসংক্ষেপ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট বিক্রি", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("৳${"%.0f".format(totalSalesAmount)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("মোট খরচ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("৳${"%.0f".format(totalExpenseAmount)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DangerRed)
                        }
                        Column {
                            Text("আনুমানিক নিট লাভ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("৳${"%.0f".format(grossProfit)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (grossProfit >= 0) SuccessGreen else DangerRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("নগদ আদায়: ৳${"%.0f".format(cashSales)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("ডিজিটাল (বিকাশ/নগদ): ৳${"%.0f".format(digitalSales)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("নতুন বকেয়া: ৳${"%.0f".format(totalDue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                }
            }
        }

        // Export Report Button
        item {
            OutlinedButton(
                onClick = {
                    val reportText = StringBuilder()
                    reportText.append("📊 *${currentBusiness?.name ?: "আমার দোকান"} - বিক্রয় রিপোর্ট*\n")
                    reportText.append("শাখা: ${currentBranch?.name ?: "প্রধান শাখা"}\n")
                    reportText.append("সময়কাল: ${selectedPeriod.titleBn}\n")
                    reportText.append("----------------------------\n")
                    reportText.append("মোট চালান: ${filteredSales.size}টি\n")
                    reportText.append("মোট বিক্রি: ৳$totalSalesAmount\n")
                    reportText.append("নগদ আদায়: ৳$totalCollected\n")
                    reportText.append("নতুন বকেয়া: ৳$totalDue\n")
                    reportText.append("মোট খরচ: ৳$totalExpenseAmount\n")
                    reportText.append("আনুমানিক লাভ: ৳$grossProfit\n")
                    reportText.append("----------------------------\n")
                    reportText.append("তারিখ: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n")
                    ShareHelper.shareTextGeneral(context, reportText.toString(), "ব্যবসায়িক রিপোর্ট শেয়ার করুন")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_report_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("রিপোর্ট শেয়ার / এক্সপোর্ট করুন")
            }
        }

        // Sales List for this period
        item {
            Text(
                text = "চালান তালিকা (${filteredSales.size}টি)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (filteredSales.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("এই সময়কালের জন্য কোনো বিক্রয় রেকর্ড পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        } else {
            items(filteredSales) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.loadInvoice(sale)
                            showInvoiceDialog = true
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(sale.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${sale.invoiceNumber} • ${SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("৳${sale.total}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Text(sale.paymentMethod.getDisplayNameBn(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    if (showInvoiceDialog && activeInvoiceSale != null) {
        InvoiceDialog(
            sale = activeInvoiceSale!!,
            items = activeInvoiceItems,
            business = currentBusiness,
            branch = currentBranch,
            onDismiss = { showInvoiceDialog = false }
        )
    }
}
