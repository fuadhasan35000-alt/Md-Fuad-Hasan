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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleEntity
import com.example.ui.AmarDokanViewModel
import com.example.ui.AppDestination
import com.example.ui.components.QuickActionButton
import com.example.ui.components.SummaryMetricCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: AmarDokanViewModel,
    onNavigate: (AppDestination) -> Unit,
    onQuickAddProduct: () -> Unit,
    onQuickAddCustomer: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onQuickCollectDue: () -> Unit,
    onViewSaleInvoice: (SaleEntity) -> Unit,
    onOpenAiAssistant: () -> Unit
) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Quick Actions Row
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ দ্রুত একশন",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionButton(
                            label = "+ বিক্রি (POS)",
                            icon = Icons.Default.PointOfSale,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { onNavigate(AppDestination.POS) }
                        )
                        QuickActionButton(
                            label = "+ পণ্য",
                            icon = Icons.Default.AddBox,
                            containerColor = InfoBlueContainer,
                            iconTint = InfoBlue,
                            onClick = onQuickAddProduct
                        )
                        QuickActionButton(
                            label = "+ কাস্টমার",
                            icon = Icons.Default.PersonAdd,
                            containerColor = PurpleBadgeContainer,
                            iconTint = PurpleBadge,
                            onClick = onQuickAddCustomer
                        )
                        QuickActionButton(
                            label = "+ পেমেন্ট",
                            icon = Icons.Default.PriceCheck,
                            containerColor = SuccessGreenContainer,
                            iconTint = SuccessGreen,
                            onClick = onQuickCollectDue
                        )
                        QuickActionButton(
                            label = "+ খরচ",
                            icon = Icons.Default.ReceiptLong,
                            containerColor = DangerRedContainer,
                            iconTint = DangerRed,
                            onClick = onQuickAddExpense
                        )
                    }
                }
            }
        }

        // 2. AI Smart Insights Carousel
        if (aiInsights.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAiAssistant() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "এআই ব্যবসা পরামর্শ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "বিস্তারিত >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val topInsight = aiInsights.first()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = topInsight.titleBn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = topInsight.descriptionBn,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 3. Low stock warning banner if any
        if (lowStockProducts.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WarningAmberContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(AppDestination.STOCK) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "কম স্টকের সতর্কতা (${lowStockProducts.size}টি পণ্য)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarningAmber
                            )
                            Text(
                                text = "দ্রুত স্টক রিলোড করতে স্টক সেকশনে যান",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = WarningAmber
                        )
                    }
                }
            }
        }

        // 4. Primary Metrics Grid
        item {
            Text(
                text = "📊 ব্যবসায়িক ড্যাশবোর্ড",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    title = "আজকের বিক্রি",
                    value = "৳${"%.0f".format(metrics.todaySalesAmount)}",
                    subtitle = "${metrics.todaySalesCount}টি চালান",
                    icon = Icons.Default.TrendingUp,
                    accentColor = SuccessGreen,
                    containerColor = SuccessGreenContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestination.REPORTS) }
                )
                SummaryMetricCard(
                    title = "মোট বিক্রি",
                    value = "৳${"%.0f".format(metrics.totalSalesAmount)}",
                    subtitle = "সর্বমোট সংগৃহীত",
                    icon = Icons.Default.MonetizationOn,
                    accentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestination.REPORTS) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    title = "মোট বকেয়া (দেনা)",
                    value = "৳${"%.0f".format(metrics.totalDueAmount)}",
                    subtitle = "কাস্টমার বকেয়া খাতা",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = DangerRed,
                    containerColor = DangerRedContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestination.CUSTOMERS) }
                )
                SummaryMetricCard(
                    title = "আজকের খরচ",
                    value = "৳${"%.0f".format(metrics.todayExpensesAmount)}",
                    subtitle = "দোকান ও আনুষঙ্গিক খরচ",
                    icon = Icons.Default.Receipt,
                    accentColor = WarningAmber,
                    containerColor = WarningAmberContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onQuickAddExpense
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    title = "মোট পণ্য সংখ্যা",
                    value = "${metrics.totalProductsCount}টি",
                    subtitle = "কম স্টক: ${metrics.lowStockCount}টি",
                    icon = Icons.Default.Inventory,
                    accentColor = InfoBlue,
                    containerColor = InfoBlueContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestination.STOCK) }
                )
                SummaryMetricCard(
                    title = "আনুমানিক লাভ",
                    value = "৳${"%.0f".format(metrics.estimatedProfit)}",
                    subtitle = "মার্জিন - খরচ",
                    icon = Icons.Default.Savings,
                    accentColor = GoldTertiary,
                    containerColor = GoldTertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestination.REPORTS) }
                )
            }
        }

        // 5. Recent Sales Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧾 সাম্প্রতিক বিক্রি (Sales)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onNavigate(AppDestination.REPORTS) }) {
                    Text("সব দেখুন")
                }
            }
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এখনো কোনো বিক্রি রেকর্ড করা হয়নি। '+ বিক্রি' বাটনে চাপ দিয়ে প্রথম চালান তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(sales.take(5)) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewSaleInvoice(sale) }
                        .testTag("sale_item_${sale.invoiceNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = sale.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${sale.invoiceNumber} • ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "৳${sale.total}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sale.due > 0) {
                                Text(
                                    text = "বকেয়া: ৳${sale.due}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DangerRed
                                )
                            } else {
                                Text(
                                    text = "পরিশোধিত",
                                    fontSize = 11.sp,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
