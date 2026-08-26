package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * 1. Add / Edit Product Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    categories: List<CategoryEntity>,
    businessId: String,
    branchId: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "সাধারণ") }
    var buyPrice by remember { mutableStateOf(initialProduct?.purchasePrice?.toString() ?: "") }
    var sellPrice by remember { mutableStateOf(initialProduct?.salePrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(initialProduct?.currentStock?.toString() ?: "10") }
    var minStock by remember { mutableStateOf(initialProduct?.minimumStock?.toString() ?: "5") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "পিস") }
    var supplier by remember { mutableStateOf(initialProduct?.supplier ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct == null) "নতুন পণ্য যোগ করুন" else "পণ্য সম্পাদনা",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("পণ্যের নাম *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = { Text("ক্রয় মূল্য (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_buy_price_input")
                    )
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = { Text("বিক্রি মূল্য (৳) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sell_price_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("বর্তমান স্টক *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text("সতর্কতা স্টক") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক (পিস/কেজি/লিটার)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("ক্যাটাগরি") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("বারকোড / স্ক্যান কোড") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU কোড") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("সরবরাহকারী / ডিলার নাম") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "পণ্যের নাম অবশ্যই দিন"
                        return@Button
                    }
                    val buy = buyPrice.toDoubleOrNull()
                    val sell = sellPrice.toDoubleOrNull()
                    if (buy == null || sell == null) {
                        errorText = "সঠিক ক্রয় ও বিক্রি মূল্য দিন"
                        return@Button
                    }
                    val curStock = stock.toDoubleOrNull() ?: 0.0
                    val mStock = minStock.toDoubleOrNull() ?: 5.0

                    val product = initialProduct?.copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        barcode = barcode.trim(),
                        category = category.trim(),
                        purchasePrice = buy,
                        salePrice = sell,
                        currentStock = curStock,
                        minimumStock = mStock,
                        unit = unit.trim(),
                        supplier = supplier.trim(),
                        updatedAt = System.currentTimeMillis()
                    ) ?: ProductEntity(
                        businessId = businessId,
                        branchId = branchId,
                        name = name.trim(),
                        sku = sku.trim().ifBlank { "SKU-${System.currentTimeMillis().toString().takeLast(6)}" },
                        barcode = barcode.trim(),
                        category = category.trim(),
                        purchasePrice = buy,
                        salePrice = sell,
                        currentStock = curStock,
                        minimumStock = mStock,
                        unit = unit.trim(),
                        supplier = supplier.trim()
                    )
                    onSave(product)
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * 2. Stock Adjustment Dialog (In / Out / Adjust)
 */
@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (StockTransactionType, Double, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(StockTransactionType.IN) }
    var quantityText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("স্টক পরিবর্তন: ${product.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("বর্তমান স্টক:", fontWeight = FontWeight.Medium)
                        Text(
                            "${product.currentStock} ${product.unit}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("অপারেশনের ধরন নির্বাচন করুন:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == StockTransactionType.IN,
                        onClick = { selectedType = StockTransactionType.IN },
                        label = { Text("স্টক ইন (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == StockTransactionType.OUT,
                        onClick = { selectedType = StockTransactionType.OUT },
                        label = { Text("স্টক আউট (-)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == StockTransactionType.ADJUSTMENT,
                        onClick = { selectedType = StockTransactionType.ADJUSTMENT },
                        label = { Text("সমন্বয়") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it; errorText = null },
                    label = {
                        Text(
                            if (selectedType == StockTransactionType.ADJUSTMENT) "নতুন সর্বমোট স্টক পরিমাণ"
                            else "পরিমাণ (${product.unit})"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_quantity_input")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("কারণ / নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull()
                    if (qty == null || qty <= 0 && selectedType != StockTransactionType.ADJUSTMENT) {
                        errorText = "সঠিক স্টক পরিমাণ লিখুন"
                        return@Button
                    }
                    onConfirm(selectedType, qty, note)
                },
                modifier = Modifier.testTag("confirm_stock_button")
            ) {
                Text("নিশ্চিত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * 3. Add / Edit Customer Dialog
 */
@Composable
fun AddEditCustomerDialog(
    initialCustomer: CustomerEntity? = null,
    businessId: String,
    branchId: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }
    var initialDue by remember { mutableStateOf(initialCustomer?.totalDue?.toString() ?: "0") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialCustomer == null) "নতুন কাস্টমার যোগ" else "কাস্টমার সম্পাদনা", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("কাস্টমারের নাম *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("মোবাইল নম্বর *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialCustomer == null) {
                    OutlinedTextField(
                        value = initialDue,
                        onValueChange = { initialDue = it },
                        label = { Text("পূর্বের বকেয়া (যদি থাকে ৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট / বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "কাস্টমারের নাম লিখুন"
                        return@Button
                    }
                    if (phone.isBlank()) {
                        errorText = "মোবাইল নম্বর লিখুন"
                        return@Button
                    }
                    val due = initialDue.toDoubleOrNull() ?: 0.0
                    val cust = initialCustomer?.copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        updatedAt = System.currentTimeMillis()
                    ) ?: CustomerEntity(
                        customerId = "CUST_" + UUID.randomUUID().toString().take(8),
                        businessId = businessId,
                        branchId = branchId,
                        name = name.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        totalDue = due,
                        notes = notes.trim()
                    )
                    onSave(cust)
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * 4. Collect Due Payment Dialog
 */
@Composable
fun CollectDueDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, PaymentMethod, String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.totalDue.toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("বকেয়া আদায়: ${customer.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = DangerRedContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("বর্তমান বকেয়া:", fontWeight = FontWeight.Medium, color = DangerRed)
                        Text("৳${customer.totalDue}", fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorText = null },
                    label = { Text("জমার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("due_amount_input")
                )

                Text("পেমেন্ট মাধ্যম:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.BKASH, PaymentMethod.NAGAD).forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.getDisplayNameBn()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = "সঠিক টাকার পরিমাণ দিন"
                        return@Button
                    }
                    onConfirm(amount, selectedMethod, note)
                },
                modifier = Modifier.testTag("confirm_due_payment_button")
            ) {
                Text("জমা নিন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

/**
 * 5. Add Expense Dialog
 */
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    val categories = listOf("দোকান ভাড়া", "বিদ্যুৎ", "পরিবহন", "বেতন", "ক্রয়", "অন্যান্য")
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন খরচ যুক্ত করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text("খরচের বিবরণ *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorText = null },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Text("ক্যাটাগরি:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.chunked(3).forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowList.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("অতিরিক্ত মন্তব্য") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(errorText ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorText = "খরচের বিবরণ দিন"
                        return@Button
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = "সঠিক পরিমাণ দিন"
                        return@Button
                    }
                    onConfirm(title.trim(), selectedCategory, amount, note.trim())
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

/**
 * 6. Full Invoice Modal Screen Dialog with WhatsApp & SMS Sharing
 */
@Composable
fun InvoiceDialog(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    business: BusinessEntity?,
    branch: BranchEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bizName = business?.name ?: "আমার দোকান"
    val brName = branch?.name ?: "প্রধান শাখা"
    val phone = business?.phone ?: ""
    val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))

    val invoiceShareText = remember(sale, items, bizName, brName) {
        ShareHelper.formatInvoiceText(bizName, brName, phone, sale, items)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧾 ডিজিটাল ক্যাশ মেমো",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Invoice Body (Paper Style)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Shop Banner
                        Text(
                            text = bizName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "শাখা: $brName | মোবা: $phone",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Invoice Details
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("চালান নং: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Text("ক্রেতা: ${sale.customerName} ${if (sale.customerPhone.isNotBlank()) "(${sale.customerPhone})" else ""}", fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Item Table Header
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("পণ্য ও পরিমাণ", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                Text("দর", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("মোট", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }

                        // Items List
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("${item.quantity} ${item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text("৳${item.unitPrice}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("৳${item.subtotal}", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calculation summary
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("সাবটোটাল:", fontSize = 13.sp)
                                Text("৳${sale.subtotal}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                            if (sale.discount > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("ছাড়:", fontSize = 13.sp, color = SuccessGreen)
                                    Text("-৳${sale.discount}", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = SuccessGreen)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("সর্বমোট বিল:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("৳${sale.total}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("পরিশোধ (${sale.paymentMethod.getDisplayNameBn()}):", fontSize = 13.sp)
                                Text("৳${sale.paid}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                            if (sale.due > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("বকেয়া:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                    Text("৳${sale.due}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "ধন্যবাদ, আবার আসবেন!",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: WhatsApp, SMS, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ShareHelper.shareViaWhatsApp(context, sale.customerPhone, invoiceShareText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("whatsapp_invoice_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            ShareHelper.sendSms(context, sale.customerPhone, invoiceShareText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sms_invoice_button")
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS")
                    }

                    FilledTonalButton(
                        onClick = {
                            ShareHelper.shareTextGeneral(context, invoiceShareText, "ক্যাশ মেমো শেয়ার করুন")
                        },
                        modifier = Modifier.testTag("share_invoice_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "প্রিন্ট বা শেয়ার")
                    }
                }
            }
        }
    }
}
