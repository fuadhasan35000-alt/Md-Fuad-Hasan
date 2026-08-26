package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppPermission
import com.example.data.model.ProductEntity
import com.example.data.model.StockTransactionType
import com.example.ui.AmarDokanViewModel
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: AmarDokanViewModel
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currentBusinessId by viewModel.currentBusinessId.collectAsState()
    val currentBranchId by viewModel.currentBranchId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var productForStockChange by remember { mutableStateOf<ProductEntity?>(null) }
    var productForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory, showOnlyLowStock) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory.isBlank() || p.category == selectedCategory
            val matchesLowStock = !showOnlyLowStock || p.currentStock <= p.minimumStock
            matchesSearch && matchesCat && matchesLowStock
        }
    }

    Scaffold(
        floatingActionButton = {
            if (viewModel.hasPermission(AppPermission.ADD_PRODUCT)) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("নতুন পণ্য যোগ", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab")
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("পণ্যের নাম, SKU বা বারকোড খুঁজুন...") },
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
                    .testTag("stock_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category and Low-stock filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = { Text("⚠️ কম স্টক") },
                        leadingIcon = {
                            if (showOnlyLowStock) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WarningAmberContainer,
                            selectedLabelColor = WarningAmber
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedCategory.isBlank() && !showOnlyLowStock,
                        onClick = {
                            selectedCategory = ""
                            showOnlyLowStock = false
                        },
                        label = { Text("সকল পণ্য (${products.size})") }
                    )
                }

                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat.name) "" else cat.name
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "কোনো পণ্য পাওয়া যায়নি" else "স্টক খালি! নতুন পণ্য যোগ করুন",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isLowStock = product.currentStock <= product.minimumStock
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_card_${product.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        )
                                        Text(
                                            text = "${product.category} • SKU: ${product.sku}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Stock pill
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isLowStock) WarningAmberContainer else SuccessGreenContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isLowStock) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = WarningAmber,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = "${product.currentStock} ${product.unit}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isLowStock) WarningAmber else SuccessGreen
                                            )
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text("বিক্রি মূল্য", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text("৳${product.salePrice}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column {
                                            Text("ক্রয় মূল্য", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text("৳${product.purchasePrice}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        }
                                    }

                                    // Action buttons (Adjust Stock & Edit)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { productForStockChange = product },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("adjust_stock_btn_${product.id}")
                                        ) {
                                            Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("স্টক চেঞ্জ", fontSize = 12.sp)
                                        }

                                        IconButton(
                                            onClick = { productForEdit = product },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", modifier = Modifier.size(18.dp))
                                        }

                                        if (viewModel.hasPermission(AppPermission.DELETE_PRODUCT)) {
                                            IconButton(
                                                onClick = { viewModel.deleteProduct(product.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "ডিলিট", tint = DangerRed, modifier = Modifier.size(18.dp))
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
    }

    // Dialogs
    if (showAddDialog) {
        AddEditProductDialog(
            categories = categories,
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { showAddDialog = false },
            onSave = { prod ->
                viewModel.saveProduct(prod) {
                    showAddDialog = false
                }
            }
        )
    }

    if (productForEdit != null) {
        AddEditProductDialog(
            initialProduct = productForEdit,
            categories = categories,
            businessId = currentBusinessId,
            branchId = currentBranchId,
            onDismiss = { productForEdit = null },
            onSave = { prod ->
                viewModel.saveProduct(prod) {
                    productForEdit = null
                }
            }
        )
    }

    if (productForStockChange != null) {
        StockAdjustmentDialog(
            product = productForStockChange!!,
            onDismiss = { productForStockChange = null },
            onConfirm = { type, qty, note ->
                viewModel.recordStockChange(productForStockChange!!, type, qty, note)
                productForStockChange = null
            }
        )
    }
}
