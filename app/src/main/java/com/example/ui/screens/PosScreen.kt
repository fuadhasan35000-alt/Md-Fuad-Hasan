package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.ui.AmarDokanViewModel
import com.example.ui.components.InvoiceDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: AmarDokanViewModel
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val currentBranch by viewModel.currentBranch.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // Checkout sheet & customer dialog state
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var discountText by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var saleNote by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    val activeInvoiceSale by viewModel.selectedSaleForInvoice.collectAsState()
    val activeInvoiceItems by viewModel.invoiceSaleItems.collectAsState()
    var showInvoiceDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory.isBlank() || p.category == selectedCategory
            matchesSearch && matchesCat
        }
    }

    val cartTotal = remember(cartItems) { cartItems.sumOf { it.subtotal } }
    val discount = remember(discountText) { discountText.toDoubleOrNull() ?: 0.0 }
    val grandTotal = remember(cartTotal, discount) { (cartTotal - discount).coerceAtLeast(0.0) }
    val paidAmount = remember(paidAmountText, grandTotal) {
        paidAmountText.toDoubleOrNull() ?: grandTotal
    }
    val dueAmount = remember(grandTotal, paidAmount) { (grandTotal - paidAmount).coerceAtLeast(0.0) }

    Scaffold(
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "কার্টে ${cartItems.sumOf { it.quantity.toInt() }}টি আইটেম",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "৳${cartTotal}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Button(
                            onClick = {
                                paidAmountText = grandTotal.toString()
                                showCheckoutSheet = true
                            },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("checkout_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("বিলিং সম্পন্ন করুন >", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
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
                placeholder = { Text("বিক্রির জন্য পণ্য বা বারকোড খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "ক্লিয়ার")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Categories Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory.isBlank(),
                        onClick = { selectedCategory = "" },
                        label = { Text("সকল") }
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

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("কোনো পণ্য পাওয়া যায়নি", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val inCart = cartItems.find { it.product.id == product.id }
                        val isOutStock = product.currentStock <= 0

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (inCart != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isOutStock) {
                                        viewModel.addToCart(product, 1.0)
                                    } else {
                                        viewModel.showSnackbar("পণ্যটির স্টক শেষ!")
                                    }
                                }
                                .testTag("pos_product_${product.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isOutStock) DangerRedContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (isOutStock) "স্টক আউট" else "স্টক: ${product.currentStock} ${product.unit}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isOutStock) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (inCart != null) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "${inCart.quantity.toInt()}টি",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "৳${product.salePrice}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    FilledTonalIconButton(
                                        onClick = {
                                            if (!isOutStock) viewModel.addToCart(product, 1.0)
                                        },
                                        modifier = Modifier.size(32.dp),
                                        enabled = !isOutStock
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "যোগ করুন", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Checkout BottomSheet
    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 পিওএস বিলিং ও চেকআউট",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.clearCart(); showCheckoutSheet = false }) {
                        Text("কার্ট খালি করুন", color = DangerRed)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Cart items scroll
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    items(cartItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("৳${item.customPrice} x ${item.quantity} = ৳${item.subtotal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(item.product, item.quantity - 1.0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "কমান", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${item.quantity.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartItemQuantity(item.product, item.quantity + 1.0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "বাড়ান", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.product) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "মুছুন", tint = DangerRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Customer Selection
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomerPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = selectedCustomer?.name ?: "সাধারণ নগদ ক্রেতা (কাস্টমার নির্বাচন করুন)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (selectedCustomer != null) {
                                    Text(
                                        text = "মোবা: ${selectedCustomer!!.phone} | বকেয়া: ৳${selectedCustomer!!.totalDue}",
                                        fontSize = 11.sp,
                                        color = if (selectedCustomer!!.totalDue > 0) DangerRed else SuccessGreen
                                    )
                                }
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Discount & Paid Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("ছাড় (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("discount_input")
                    )
                    OutlinedTextField(
                        value = paidAmountText,
                        onValueChange = { paidAmountText = it },
                        label = { Text("পরিশোধ (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paid_amount_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Payment Methods
                Text("পরিশোধ মাধ্যম:", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.BKASH, PaymentMethod.NAGAD, PaymentMethod.CARD).forEach { method ->
                        FilterChip(
                            selected = selectedPaymentMethod == method,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method.getDisplayNameBn(), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Calculation Summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("মোট বিল:", fontWeight = FontWeight.Medium)
                            Text("৳$grandTotal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("নগদ পরিশোধ:", fontWeight = FontWeight.Medium)
                            Text("৳$paidAmount", fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        if (dueAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("বকেয়া রাখা হবে:", fontWeight = FontWeight.Bold, color = DangerRed)
                                Text("৳$dueAmount", fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.selectedPosCustomer.value = selectedCustomer
                        viewModel.posDiscount.value = discount
                        viewModel.posPaidAmount.value = paidAmount
                        viewModel.posPaymentMethod.value = selectedPaymentMethod

                        viewModel.processSale(note = saleNote) { sale ->
                            showCheckoutSheet = false
                            showInvoiceDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_sale_submit_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("বিক্রি নিশ্চিত করুন (চালান তৈরি)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("কাস্টমার নির্বাচন করুন", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomer = null
                                    showCustomerPicker = false
                                }
                        ) {
                            Text(
                                "সাধারণ নগদ কাস্টমার (নামহীন)",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(customers) { cust ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomer = cust
                                    showCustomerPicker = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(cust.name, fontWeight = FontWeight.Bold)
                                Text("ফোন: ${cust.phone} • বকেয়া: ৳${cust.totalDue}", fontSize = 12.sp, color = if (cust.totalDue > 0) DangerRed else SuccessGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("বন্ধ করুন") }
            }
        )
    }

    // Invoice View Dialog
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
