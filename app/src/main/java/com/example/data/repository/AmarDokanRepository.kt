package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.notification.DokanFirebaseMessagingService
import com.example.notification.PushNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AmarDokanRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val businessDao = database.businessDao()
    private val branchDao = database.branchDao()
    private val userDao = database.userDao()
    private val settingsDao = database.settingsDao()
    private val categoryDao = database.categoryDao()
    private val productDao = database.productDao()
    private val stockDao = database.stockDao()
    private val customerDao = database.customerDao()
    private val saleDao = database.saleDao()
    private val saleItemDao = database.saleItemDao()
    private val paymentDao = database.paymentDao()
    private val expenseDao = database.expenseDao()
    private val auditDao = database.auditDao()
    private val syncQueueDao = database.syncQueueDao()
    private val notificationDao = database.notificationDao()

    // Active session state
    private val _currentBusinessId = MutableStateFlow<String>("")
    val currentBusinessId: StateFlow<String> = _currentBusinessId.asStateFlow()

    private val _currentBranchId = MutableStateFlow<String>("")
    val currentBranchId: StateFlow<String> = _currentBranchId.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Cloud sync status simulation / state
    private val _syncState = MutableStateFlow(SyncStatus.SYNCED)
    val syncState: StateFlow<SyncStatus> = _syncState.asStateFlow()

    val pendingSyncCount: Flow<Int> = syncQueueDao.getPendingSyncCount()

    // Reactive streams for active business & branch
    val businesses: Flow<List<BusinessEntity>> = businessDao.getAllBusinesses()

    val branches: Flow<List<BranchEntity>> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(emptyList()) else branchDao.getBranchesForBusiness(bId)
    }

    val currentBusiness: Flow<BusinessEntity?> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(null) else businessDao.observeBusinessById(bId)
    }

    val currentBranch: Flow<BranchEntity?> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank() || brId.isBlank()) flowOf(null) else branchDao.observeBranchById(bId, brId)
    }

    val shopSettings: Flow<ShopSettingsEntity?> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(null) else settingsDao.observeSettings(bId)
    }

    val categories: Flow<List<CategoryEntity>> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(emptyList()) else categoryDao.getCategories(bId)
    }

    val products: Flow<List<ProductEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else productDao.getProducts(bId, brId)
    }

    val lowStockProducts: Flow<List<ProductEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else productDao.getLowStockProducts(bId, brId)
    }

    val customers: Flow<List<CustomerEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else customerDao.getCustomers(bId, brId)
    }

    val sales: Flow<List<SaleEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else saleDao.getSales(bId, brId)
    }

    val expenses: Flow<List<ExpenseEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else expenseDao.getExpenses(bId, brId)
    }

    val users: Flow<List<UserEntity>> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(emptyList()) else userDao.getUsersForBusiness(bId)
    }

    val pendingUsers: Flow<List<UserEntity>> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(emptyList()) else userDao.getPendingUsersForBusiness(bId)
    }

    val auditLogs: Flow<List<AuditLogEntity>> = _currentBusinessId.flatMapLatest { bId ->
        if (bId.isBlank()) flowOf(emptyList()) else auditDao.getLogs(bId)
    }

    val notifications: Flow<List<AppNotificationEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        notificationDao.getNotifications(bId, brId)
    }

    val unreadNotificationCount: Flow<Int> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        notificationDao.getUnreadCount(bId, brId)
    }

    val stockTransactions: Flow<List<StockTransactionEntity>> = combine(_currentBusinessId, _currentBranchId) { bId, brId ->
        Pair(bId, brId)
    }.flatMapLatest { (bId, brId) ->
        if (bId.isBlank()) flowOf(emptyList()) else stockDao.getTransactions(bId, brId)
    }

    // Permission Checking
    fun hasPermission(permission: AppPermission): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role == UserRole.SUPER_ADMIN) return true
        if (user.status != UserStatus.ACTIVE) return false
        return user.permissions.contains(permission.code)
    }

    suspend fun isFirstRun(): Boolean = withContext(Dispatchers.IO) {
        val count = businessDao.getBusinessCount()
        if (count > 0) {
            try {
                userDao.updateSuperAdminEmail("fuadhasan35000@gmail.com", "admin@amardokan.com")
            } catch (e: Exception) {
                // Ignore if migration already done
            }
        }
        count == 0
    }

    // First Run Setup: Creates Business, Default Branch, Super Admin & Settings
    suspend fun setupFirstRun(
        businessName: String,
        ownerName: String,
        phone: String,
        address: String,
        firstBranchName: String,
        adminEmail: String,
        adminPass: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val businessId = "BIZ_" + UUID.randomUUID().toString().take(8).uppercase()
            val branchId = "BR_MAIN"
            val userId = "USR_ADMIN_" + UUID.randomUUID().toString().take(6).uppercase()

            val business = BusinessEntity(
                businessId = businessId,
                name = businessName.ifBlank { "আমার দোকান" },
                ownerName = ownerName.ifBlank { "প্রোপাইটার" },
                phone = phone,
                address = address,
                isDefault = true,
                isActive = true
            )
            businessDao.insertBusiness(business)

            val branch = BranchEntity(
                branchId = branchId,
                businessId = businessId,
                name = firstBranchName.ifBlank { "প্রধান শাখা" },
                address = address,
                phone = phone,
                status = "ACTIVE"
            )
            branchDao.insertBranch(branch)

            val superAdmin = UserEntity(
                userId = userId,
                businessId = businessId,
                branchId = branchId,
                name = ownerName.ifBlank { "সুপার অ্যাডমিন" },
                email = adminEmail.ifBlank { "fuadhasan35000@gmail.com" }.trim().lowercase(),
                phone = phone,
                role = UserRole.SUPER_ADMIN,
                status = UserStatus.ACTIVE,
                permissions = AppPermission.values().map { it.code },
                passwordHash = hashPassword(adminPass)
            )
            userDao.insertUser(superAdmin)

            val settings = ShopSettingsEntity(
                businessId = businessId,
                negativeStockAllowed = false,
                invoiceHeader = "${business.name} - ক্যাশ মেমো",
                invoiceFooter = "ধন্যবাদ, আবার আসবেন!",
                vatPercentage = 0.0,
                aiEnabled = true
            )
            settingsDao.insertOrUpdate(settings)

            // Seed standard categories
            val defaultCategories = listOf("মুদি ও খাদ্য", "পানীয়", "কসমেটিকস", "ইলেকট্রনিক্স", "পোশাক", "সাধারণ")
            defaultCategories.forEach { catName ->
                categoryDao.insertCategory(
                    CategoryEntity(
                        categoryId = "CAT_" + UUID.randomUUID().toString().take(6),
                        businessId = businessId,
                        name = catName
                    )
                )
            }

            // Seed demo sample products for rich initial experience
            seedSampleProducts(businessId, branchId)

            // Audit log
            auditDao.insertLog(
                AuditLogEntity(
                    businessId = businessId,
                    branchId = branchId,
                    userId = userId,
                    userName = superAdmin.name,
                    action = "INITIAL_SETUP",
                    details = "নতুন দোকান ও সুপার অ্যাডমিন তৈরি সম্পন্ন হয়েছে"
                )
            )

            // Set current state
            _currentBusinessId.value = businessId
            _currentBranchId.value = branchId
            _currentUser.value = superAdmin

            Result.success(superAdmin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedSampleProducts(businessId: String, branchId: String) {
        val sampleItems = listOf(
            Triple("মিনিকেট চাল (৫০ কেজি বস্তা)", 3200.0, 3450.0) to ("মুদি ও খাদ্য" to 15.0),
            Triple("রূপচাঁদা সয়াবিন তেল (৫ লিটার)", 790.0, 850.0) to ("মুদি ও খাদ্য" to 24.0),
            Triple("ফ্রেশ আটা (২ কেজি)", 95.0, 110.0) to ("মুদি ও খাদ্য" to 30.0),
            Triple("তীর চিনি (১ কেজি)", 125.0, 135.0) to ("মুদি ও খাদ্য" to 40.0),
            Triple("স্পীড এনার্জি ড্রিংক (২৫০ মি.লি.)", 28.0, 35.0) to ("পানীয়" to 48.0),
            Triple("লাইফবয় সাবান (১০০ গ্রাম)", 45.0, 52.0) to ("কসমেটিকস" to 60.0),
            Triple("প্রাণ ম্যাঙ্গো জুস (১ লিটার)", 75.0, 90.0) to ("পানীয়" to 3.0), // Low stock demo
            Triple("লাক্স বিউটি সোপ (১০০ গ্রাম)", 55.0, 65.0) to ("কসমেটিকস" to 2.0) // Low stock demo
        )

        sampleItems.forEachIndexed { index, (productInfo, extra) ->
            val (name, buy, sell) = productInfo
            val (category, stock) = extra
            productDao.insertProduct(
                ProductEntity(
                    businessId = businessId,
                    branchId = branchId,
                    name = name,
                    sku = "SKU-${1001 + index}",
                    barcode = "894100${1001 + index}",
                    category = category,
                    purchasePrice = buy,
                    salePrice = sell,
                    currentStock = stock,
                    minimumStock = 5.0,
                    unit = if (name.contains("লিটার")) "লিটার" else if (name.contains("বস্তা")) "বস্তা" else "পিস"
                )
            )
        }

        // Seed demo customer
        customerDao.insertCustomer(
            CustomerEntity(
                customerId = "CUST_101",
                businessId = businessId,
                branchId = branchId,
                name = "আব্দুর রহিম",
                phone = "01711000001",
                address = "মেইন রোড, বাজার সংলগ্ন",
                totalPurchase = 5400.0,
                totalPaid = 4000.0,
                totalDue = 1400.0
            )
        )
        customerDao.insertCustomer(
            CustomerEntity(
                customerId = "CUST_102",
                businessId = businessId,
                branchId = branchId,
                name = "মো: হাসান আলী",
                phone = "01819000002",
                address = "পোস্ট অফিস মোড়",
                totalPurchase = 8500.0,
                totalPaid = 8500.0,
                totalDue = 0.0
            )
        )
    }

    // Login with Email and Password
    suspend fun login(email: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserByEmail(email.trim().lowercase())
                ?: return@withContext Result.failure(Exception("ব্যবহারকারী খুঁজে পাওয়া যায়নি। সঠিক ইমেইল দিন।"))

            if (user.passwordHash.isNotEmpty() && user.passwordHash != hashPassword(pass)) {
                return@withContext Result.failure(Exception("পাসওয়ার্ড সঠিক নয়। পুনরায় চেষ্টা করুন।"))
            }

            if (user.status == UserStatus.PENDING) {
                return@withContext Result.failure(Exception("আপনার অ্যাকাউন্টটি এখনো সুপার অ্যাডমিন কর্তৃক অনুমোদিত হয়নি। অনুগ্রহ করে অপেক্ষা করুন।"))
            }

            if (user.status == UserStatus.BLOCKED) {
                return@withContext Result.failure(Exception("আপনার অ্যাকাউন্টটি নিষ্ক্রিয় বা ব্লক করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"))
            }

            _currentBusinessId.value = user.businessId
            _currentBranchId.value = user.branchId
            _currentUser.value = user

            // Update FCM topic subscriptions for user's role and business
            DokanFirebaseMessagingService.updateRoleTopicSubscriptions(user.role)
            DokanFirebaseMessagingService.subscribeToBusinessTopics(user.businessId, user.branchId)

            auditDao.insertLog(
                AuditLogEntity(
                    businessId = user.businessId,
                    branchId = user.branchId,
                    userId = user.userId,
                    userName = user.name,
                    action = "LOGIN",
                    details = "${user.role.getDisplayNameBn()} হিসেবে সফল লগইন"
                )
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    suspend fun updateUserEmail(userId: String, newEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext Result.failure(Exception("ইউজার পাওয়া যায়নি"))
            val updatedUser = user.copy(email = newEmail.trim().lowercase())
            userDao.updateUser(updatedUser)
            if (_currentUser.value?.userId == userId) {
                _currentUser.value = updatedUser
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun switchBusiness(businessId: String) {
        _currentBusinessId.value = businessId
    }

    fun switchBranch(branchId: String) {
        _currentBranchId.value = branchId
    }

    // POS & SALE TRANSACTION
    data class CartItem(
        val product: ProductEntity,
        var quantity: Double,
        var customPrice: Double = product.salePrice
    ) {
        val subtotal: Double get() = quantity * customPrice
    }

    suspend fun processSale(
        cartItems: List<CartItem>,
        customer: CustomerEntity?,
        discountAmount: Double,
        paidAmount: Double,
        paymentMethod: PaymentMethod,
        note: String
    ): Result<SaleEntity> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(Exception("কার্ট খালি! পণ্য যোগ করুন।"))
        }

        val bId = _currentBusinessId.value
        val brId = _currentBranchId.value
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("ব্যবহারকারী লগইন নেই"))

        if (!hasPermission(AppPermission.CREATE_SALE)) {
            return@withContext Result.failure(Exception("আপনার বিক্রি করার অনুমতি নেই।"))
        }

        val settings = settingsDao.getSettings(bId)
        val negativeAllowed = settings?.negativeStockAllowed ?: false

        // Stock validation
        if (!negativeAllowed) {
            for (item in cartItems) {
                val currentP = productDao.getProductById(item.product.id)
                if (currentP != null && currentP.currentStock < item.quantity) {
                    return@withContext Result.failure(
                        Exception("পণ্য '${item.product.name}'-এর পর্যাপ্ত স্টক নেই! বর্তমান স্টক: ${currentP.currentStock} ${currentP.unit}")
                    )
                }
            }
        }

        val subtotal = cartItems.sumOf { it.subtotal }
        val finalTotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val dueAmount = (finalTotal - paidAmount).coerceAtLeast(0.0)
        val saleId = "SALE_" + UUID.randomUUID().toString().take(10).uppercase()
        val invoiceNumber = "INV-" + SimpleDateFormat("yyMMddHHmmss", Locale.US).format(Date())

        val sale = SaleEntity(
            saleId = saleId,
            businessId = bId,
            branchId = brId,
            invoiceNumber = invoiceNumber,
            customerId = customer?.customerId ?: "",
            customerName = customer?.name ?: "সাধারণ কাস্টমার",
            customerPhone = customer?.phone ?: "",
            subtotal = subtotal,
            discount = discountAmount,
            total = finalTotal,
            paid = paidAmount,
            due = dueAmount,
            paymentMethod = paymentMethod,
            userId = user.userId,
            userName = user.name,
            note = note
        )

        saleDao.insertSale(sale)

        val saleItems = cartItems.map { item ->
            SaleItemEntity(
                saleId = saleId,
                businessId = bId,
                branchId = brId,
                productId = item.product.id,
                productName = item.product.name,
                unit = item.product.unit,
                quantity = item.quantity,
                unitPrice = item.customPrice,
                purchasePrice = item.product.purchasePrice,
                subtotal = item.subtotal
            )
        }
        saleItemDao.insertSaleItems(saleItems)

        // Deduct Stock & Record Transactions
        for (item in cartItems) {
            val prod = productDao.getProductById(item.product.id) ?: continue
            val prevStock = prod.currentStock
            val newStock = prevStock - item.quantity
            productDao.updateStock(prod.id, newStock)

            stockDao.insertTransaction(
                StockTransactionEntity(
                    transactionId = "TX_" + UUID.randomUUID().toString().take(8),
                    businessId = bId,
                    branchId = brId,
                    productId = prod.id,
                    productName = prod.name,
                    type = StockTransactionType.OUT,
                    quantity = item.quantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    note = "বিক্রি চালান: $invoiceNumber",
                    userId = user.userId,
                    userName = user.name
                )
            )

            // Trigger Low Stock Push Notification if stock dropped below alert threshold
            if (newStock <= prod.minimumStock) {
                PushNotificationHelper.showLowStockNotification(
                    context = context,
                    productName = prod.name,
                    currentStock = newStock,
                    minStock = prod.minimumStock,
                    productId = prod.id.toString(),
                    businessId = bId,
                    branchId = brId
                )
            }
        }

        // Trigger Due Payment Push Notification if sale has outstanding due
        if (dueAmount > 0) {
            PushNotificationHelper.showDuePaymentNotification(
                context = context,
                customerName = customer?.name ?: "কাস্টমার",
                amount = dueAmount,
                invoiceOrNote = "চালান #$invoiceNumber",
                customerId = customer?.customerId ?: "",
                isPaymentReceived = false,
                businessId = bId,
                branchId = brId
            )
        }

        // Update Customer Ledger
        if (customer != null) {
            customerDao.updateCustomerBalanceOnSale(
                customerId = customer.customerId,
                purchaseAmount = finalTotal,
                dueChange = dueAmount,
                paidAmount = paidAmount
            )

            // If customer paid, record payment entry
            if (paidAmount > 0) {
                paymentDao.insertPayment(
                    PaymentEntity(
                        paymentId = "PAY_" + UUID.randomUUID().toString().take(8),
                        businessId = bId,
                        branchId = brId,
                        customerId = customer.customerId,
                        customerName = customer.name,
                        amount = paidAmount,
                        paymentMethod = paymentMethod,
                        note = "বিক্রি পেমেন্ট (চালান: $invoiceNumber)",
                        receivedBy = user.userId,
                        receivedByName = user.name
                    )
                )
            }
        }

        // Sync Queue & Audit
        enqueueSync("SALE", saleId, "INSERT", JSONObject().apply {
            put("saleId", saleId)
            put("invoiceNumber", invoiceNumber)
            put("total", finalTotal)
        }.toString())

        auditDao.insertLog(
            AuditLogEntity(
                businessId = bId,
                branchId = brId,
                userId = user.userId,
                userName = user.name,
                action = "CREATE_SALE",
                details = "চালান $invoiceNumber সম্পন্ন (মোট: ৳$finalTotal, আদায়: ৳$paidAmount, বকেয়া: ৳$dueAmount)"
            )
        )

        Result.success(sale)
    }

    suspend fun getSaleItems(saleId: String): List<SaleItemEntity> = withContext(Dispatchers.IO) {
        saleItemDao.getItemsForSale(saleId)
    }

    // Stock Management (In / Out / Adjustment)
    suspend fun recordStockChange(
        product: ProductEntity,
        type: StockTransactionType,
        quantity: Double,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bId = _currentBusinessId.value
            val brId = _currentBranchId.value
            val user = _currentUser.value ?: return@withContext Result.failure(Exception("ব্যবহারকারী লগইন নেই"))

            val prevStock = product.currentStock
            val newStock = when (type) {
                StockTransactionType.IN -> prevStock + quantity
                StockTransactionType.OUT -> (prevStock - quantity).coerceAtLeast(0.0)
                StockTransactionType.ADJUSTMENT -> quantity
            }

            productDao.updateStock(product.id, newStock)

            val tx = StockTransactionEntity(
                transactionId = "STX_" + UUID.randomUUID().toString().take(8),
                businessId = bId,
                branchId = brId,
                productId = product.id,
                productName = product.name,
                type = type,
                quantity = quantity,
                previousStock = prevStock,
                newStock = newStock,
                note = note.ifBlank { type.getDisplayNameBn() },
                userId = user.userId,
                userName = user.name
            )
            stockDao.insertTransaction(tx)

            enqueueSync("STOCK", tx.transactionId, "INSERT", JSONObject().apply {
                put("productId", product.id)
                put("newStock", newStock)
                put("type", type.name)
            }.toString())

            auditDao.insertLog(
                AuditLogEntity(
                    businessId = bId,
                    branchId = brId,
                    userId = user.userId,
                    userName = user.name,
                    action = "STOCK_CHANGE",
                    details = "${product.name} ${type.getDisplayNameBn()} (পরিমাণ: $quantity, নতুন স্টক: $newStock)"
                )
            )

            // Low Stock notification check for manual stock out / adjustment
            if (newStock <= product.minimumStock && (type == StockTransactionType.OUT || type == StockTransactionType.ADJUSTMENT)) {
                PushNotificationHelper.showLowStockNotification(
                    context = context,
                    productName = product.name,
                    currentStock = newStock,
                    minStock = product.minimumStock,
                    productId = product.id.toString(),
                    businessId = bId,
                    branchId = brId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Product CRUD
    suspend fun saveProduct(product: ProductEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = productDao.insertProduct(product)
            enqueueSync("PRODUCT", id.toString(), "UPSERT", product.name)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.softDeleteProduct(productId)
            enqueueSync("PRODUCT", productId.toString(), "DELETE", "Soft deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Customer CRUD & Due Collection
    suspend fun saveCustomer(customer: CustomerEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = customerDao.insertCustomer(customer)
            enqueueSync("CUSTOMER", customer.customerId, "UPSERT", customer.name)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordCustomerDuePayment(
        customer: CustomerEntity,
        amount: Double,
        paymentMethod: PaymentMethod,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bId = _currentBusinessId.value
            val brId = _currentBranchId.value
            val user = _currentUser.value ?: return@withContext Result.failure(Exception("ব্যবহারকারী লগইন নেই"))

            if (amount <= 0) return@withContext Result.failure(Exception("সঠিক জমার পরিমাণ লিখুন"))

            customerDao.updateCustomerBalanceOnPayment(customer.customerId, amount)

            val payment = PaymentEntity(
                paymentId = "PAY_" + UUID.randomUUID().toString().take(8),
                businessId = bId,
                branchId = brId,
                customerId = customer.customerId,
                customerName = customer.name,
                amount = amount,
                paymentMethod = paymentMethod,
                note = note.ifBlank { "বকেয়া আদায়" },
                receivedBy = user.userId,
                receivedByName = user.name
            )
            paymentDao.insertPayment(payment)

            enqueueSync("PAYMENT", payment.paymentId, "INSERT", "Payment of $amount")
            auditDao.insertLog(
                AuditLogEntity(
                    businessId = bId,
                    branchId = brId,
                    userId = user.userId,
                    userName = user.name,
                    action = "COLLECT_DUE",
                    details = "${customer.name}-এর কাছ থেকে ৳$amount বকেয়া আদায় করা হয়েছে"
                )
            )

            // Trigger payment received push notification
            PushNotificationHelper.showDuePaymentNotification(
                context = context,
                customerName = customer.name,
                amount = amount,
                invoiceOrNote = note.ifBlank { "বকেয়া আদায় সম্পন্ন" },
                customerId = customer.customerId,
                isPaymentReceived = true,
                businessId = bId,
                branchId = brId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Expense Management
    suspend fun addExpense(
        title: String,
        category: String,
        amount: Double,
        note: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val bId = _currentBusinessId.value
            val brId = _currentBranchId.value
            val user = _currentUser.value ?: return@withContext Result.failure(Exception("ব্যবহারকারী লগইন নেই"))

            val expense = ExpenseEntity(
                expenseId = "EXP_" + UUID.randomUUID().toString().take(8),
                businessId = bId,
                branchId = brId,
                title = title,
                category = category,
                amount = amount,
                note = note,
                createdBy = user.userId,
                createdByName = user.name
            )
            val id = expenseDao.insertExpense(expense)
            enqueueSync("EXPENSE", expense.expenseId, "INSERT", expense.title)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            expenseDao.deleteExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Staff Approval & Management
    suspend fun createStaffAccount(
        name: String,
        email: String,
        phone: String,
        branchId: String,
        role: UserRole,
        pass: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val bId = _currentBusinessId.value
            val existing = userDao.getUserByEmail(email.trim().lowercase())
            if (existing != null) {
                return@withContext Result.failure(Exception("এই ইমেইল দিয়ে ইতোমধ্যে একটি অ্যাকাউন্ট রয়েছে।"))
            }

            val newUser = UserEntity(
                userId = "USR_" + UUID.randomUUID().toString().take(6),
                businessId = bId,
                branchId = branchId,
                name = name,
                email = email.trim().lowercase(),
                phone = phone,
                role = role,
                status = if (role == UserRole.STAFF) UserStatus.PENDING else UserStatus.ACTIVE,
                permissions = AppPermission.getDefaultPermissionsForRole(role).map { it.code },
                passwordHash = hashPassword(pass)
            )
            userDao.insertUser(newUser)

            // Trigger Staff Approval Push Notification if staff is pending approval
            if (newUser.status == UserStatus.PENDING) {
                PushNotificationHelper.showStaffApprovalNotification(
                    context = context,
                    staffName = newUser.name,
                    staffEmail = newUser.email,
                    roleTitle = newUser.role.getDisplayNameBn(),
                    userId = newUser.userId,
                    businessId = bId,
                    branchId = branchId
                )
            }

            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStaffStatus(userId: String, status: UserStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.updateUserStatus(userId, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStaffPermissions(userId: String, permissions: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDao.updateUserPermissions(userId, permissions)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Business & Branch Creation
    suspend fun createBusiness(name: String, owner: String, phone: String, address: String): Result<BusinessEntity> = withContext(Dispatchers.IO) {
        try {
            val bId = "BIZ_" + UUID.randomUUID().toString().take(8).uppercase()
            val biz = BusinessEntity(
                businessId = bId,
                name = name,
                ownerName = owner,
                phone = phone,
                address = address
            )
            businessDao.insertBusiness(biz)

            // Create default branch
            val mainBranch = BranchEntity(
                branchId = "BR_MAIN",
                businessId = bId,
                name = "প্রধান শাখা",
                address = address,
                phone = phone
            )
            branchDao.insertBranch(mainBranch)

            // Create settings
            settingsDao.insertOrUpdate(ShopSettingsEntity(businessId = bId))

            Result.success(biz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBranch(name: String, address: String, phone: String): Result<BranchEntity> = withContext(Dispatchers.IO) {
        try {
            val bId = _currentBusinessId.value
            val brId = "BR_" + UUID.randomUUID().toString().take(6).uppercase()
            val branch = BranchEntity(
                branchId = brId,
                businessId = bId,
                name = name,
                address = address,
                phone = phone
            )
            branchDao.insertBranch(branch)
            Result.success(branch)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Offline Sync Helper
    private suspend fun enqueueSync(type: String, id: String, op: String, payload: String) {
        syncQueueDao.enqueue(
            SyncQueueEntity(
                businessId = _currentBusinessId.value,
                branchId = _currentBranchId.value,
                entityType = type,
                entityId = id,
                operation = op,
                payloadJson = payload,
                status = SyncStatus.PENDING
            )
        )
    }

    suspend fun triggerManualCloudSync(): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = SyncStatus.SYNCING
        try {
            kotlinx.coroutines.delay(1200) // Simulated robust cloud sync sequence
            syncQueueDao.clearCompleted()
            _syncState.value = SyncStatus.SYNCED
            true
        } catch (e: Exception) {
            _syncState.value = SyncStatus.FAILED
            false
        }
    }

    // Backup & Restore Engine
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val bId = _currentBusinessId.value
        val root = JSONObject()
        root.put("appName", "আমার দোকান (Amar Dokan)")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("businessId", bId)

        // Products
        val prods = productDao.getProducts(bId, "").first()
        val prodArray = JSONArray()
        for (p in prods) {
            prodArray.put(JSONObject().apply {
                put("name", p.name)
                put("sku", p.sku)
                put("category", p.category)
                put("purchasePrice", p.purchasePrice)
                put("salePrice", p.salePrice)
                put("currentStock", p.currentStock)
                put("unit", p.unit)
            })
        }
        root.put("products", prodArray)

        // Customers
        val custs = customerDao.getCustomers(bId, "").first()
        val custArray = JSONArray()
        for (c in custs) {
            custArray.put(JSONObject().apply {
                put("name", c.name)
                put("phone", c.phone)
                put("address", c.address)
                put("totalDue", c.totalDue)
                put("totalPurchase", c.totalPurchase)
            })
        }
        root.put("customers", custArray)

        // Sales count & summary
        val salesList = saleDao.getSales(bId, "").first()
        root.put("totalSalesCount", salesList.size)
        root.put("totalSalesAmount", salesList.sumOf { it.total })

        root.toString(2)
    }

    // ==========================================
    // NOTIFICATION MANAGEMENT & SIMULATOR
    // ==========================================
    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(_currentBusinessId.value)
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAllNotifications(_currentBusinessId.value)
    }

    fun triggerTestNotification(type: NotificationType) {
        val bId = _currentBusinessId.value
        val brId = _currentBranchId.value
        when (type) {
            NotificationType.LOW_STOCK -> {
                PushNotificationHelper.showLowStockNotification(
                    context = context,
                    productName = "মিনিকেট চাল (৫০ কেজি বস্তা)",
                    currentStock = 2.0,
                    minStock = 10.0,
                    productId = "PROD_DEMO_01",
                    businessId = bId,
                    branchId = brId
                )
            }
            NotificationType.DUE_PAYMENT -> {
                PushNotificationHelper.showDuePaymentNotification(
                    context = context,
                    customerName = "হাসান মাহমুদ",
                    amount = 3500.0,
                    invoiceOrNote = "চালান #INV-260801 (বকেয়া)",
                    customerId = "CUST_DEMO_01",
                    isPaymentReceived = false,
                    businessId = bId,
                    branchId = brId
                )
            }
            NotificationType.STAFF_APPROVAL -> {
                PushNotificationHelper.showStaffApprovalNotification(
                    context = context,
                    staffName = "তানভীর আহমেদ",
                    staffEmail = "tanvir.sales@example.com",
                    roleTitle = "সেলস এক্সিকিউটিভ",
                    userId = "USR_DEMO_STAFF",
                    businessId = bId,
                    branchId = brId
                )
            }
            NotificationType.GENERAL -> {
                PushNotificationHelper.showGeneralNotification(
                    context = context,
                    title = "🎉 আমার দোকান ক্লাউড সিঙ্ক সফল",
                    message = "সকল দৈনিক বিক্রয় ও বকেয়ার হিসাব ক্লাউড সার্ভারে নিরাপদে সংরক্ষিত হয়েছে।",
                    destination = PushNotificationHelper.DESTINATION_STOCK,
                    businessId = bId,
                    branchId = brId
                )
            }
        }
    }

    private fun hashPassword(password: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(password.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            password.hashCode().toString()
        }
    }
}
