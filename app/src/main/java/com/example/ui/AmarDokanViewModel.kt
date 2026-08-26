package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AmarDokanRepository
import com.example.util.AiAdvisorService
import com.example.util.BusinessInsight
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppDestination {
    HOME,
    STOCK,
    POS,
    CUSTOMERS,
    REPORTS
}

enum class ReportPeriod(val titleBn: String) {
    TODAY("আজ"),
    YESTERDAY("গতকাল"),
    THIS_WEEK("এই সপ্তাহ"),
    THIS_MONTH("এই মাস"),
    ALL_TIME("সর্বমোট")
}

data class DashboardMetrics(
    val totalSalesAmount: Double = 0.0,
    val todaySalesAmount: Double = 0.0,
    val todaySalesCount: Int = 0,
    val totalCollectedAmount: Double = 0.0,
    val totalDueAmount: Double = 0.0,
    val totalProductsCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalCustomersCount: Int = 0,
    val todayExpensesAmount: Double = 0.0,
    val estimatedProfit: Double = 0.0
)

class AmarDokanViewModel(
    private val repository: AmarDokanRepository
) : ViewModel() {

    val currentBusinessId = repository.currentBusinessId
    val currentBranchId = repository.currentBranchId
    val currentUser = repository.currentUser
    val syncState = repository.syncState
    val pendingSyncCount = repository.pendingSyncCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val businesses = repository.businesses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val branches = repository.branches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentBusiness = repository.currentBusiness.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val currentBranch = repository.currentBranch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val shopSettings = repository.shopSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products = repository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lowStockProducts = repository.lowStockProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repository.customers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = repository.sales.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = repository.expenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val users = repository.users.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingUsers = repository.pendingUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockTransactions = repository.stockTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unreadNotificationCount = repository.unreadNotificationCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI state
    val isFirstRun = MutableStateFlow<Boolean?>(null)
    val activeDestination = MutableStateFlow(AppDestination.HOME)
    val snackbarMessage = MutableStateFlow<String?>(null)

    // POS Cart State
    val cartItems = MutableStateFlow<List<AmarDokanRepository.CartItem>>(emptyList())
    val selectedPosCustomer = MutableStateFlow<CustomerEntity?>(null)
    val posDiscount = MutableStateFlow(0.0)
    val posPaidAmount = MutableStateFlow(0.0)
    val posPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val selectedSaleForInvoice = MutableStateFlow<SaleEntity?>(null)
    val invoiceSaleItems = MutableStateFlow<List<SaleItemEntity>>(emptyList())

    // Reports filter
    val reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val reportBranchFilter = MutableStateFlow<String>("") // "" means current branch or all

    // Search and filters
    val productSearchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String>("")
    val customerSearchQuery = MutableStateFlow("")

    init {
        checkFirstRun()
    }

    fun checkFirstRun() {
        viewModelScope.launch {
            val first = repository.isFirstRun()
            isFirstRun.value = first
        }
    }

    // Dashboard metrics derived reactively
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        sales,
        expenses,
        customers,
        products,
        lowStockProducts
    ) { salesList, expList, custList, prodList, lowList ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + (24 * 60 * 60 * 1000)

        val todaySales = salesList.filter { it.createdAt in startOfToday..endOfToday }
        val todayExpenses = expList.filter { it.createdAt in startOfToday..endOfToday }

        val totalSales = salesList.sumOf { it.total }
        val todaySalesTotal = todaySales.sumOf { it.total }
        val todayExpensesTotal = todayExpenses.sumOf { it.amount }
        val totalDue = custList.sumOf { it.totalDue }
        val totalPaid = salesList.sumOf { it.paid }

        // Approx profit: Total sales - (Estimated cost 75%) - expenses
        val estimatedGrossProfit = (totalSales * 0.20) - expList.sumOf { it.amount }

        DashboardMetrics(
            totalSalesAmount = totalSales,
            todaySalesAmount = todaySalesTotal,
            todaySalesCount = todaySales.size,
            totalCollectedAmount = totalPaid,
            totalDueAmount = totalDue,
            totalProductsCount = prodList.size,
            lowStockCount = lowList.size,
            totalCustomersCount = custList.size,
            todayExpensesAmount = todayExpensesTotal,
            estimatedProfit = estimatedGrossProfit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val aiInsights: StateFlow<List<BusinessInsight>> = combine(
        sales,
        products,
        customers,
        expenses
    ) { s, p, c, e ->
        AiAdvisorService.generateInsights(s, p, c, e)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // First Run Setup
    fun setupFirstRun(
        bizName: String,
        owner: String,
        phone: String,
        address: String,
        branch: String,
        email: String,
        pass: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.setupFirstRun(bizName, owner, phone, address, branch, email, pass)
            result.onSuccess {
                isFirstRun.value = false
                showSnackbar("দোকান সেটআপ সফল হয়েছে! স্বাগতম।")
                onSuccess()
            }.onFailure {
                showSnackbar("ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    // Login
    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.login(email, pass)
            res.onSuccess {
                showSnackbar("স্বাগতম, ${it.name} (${it.role.getDisplayNameBn()})")
                onSuccess()
            }.onFailure {
                showSnackbar("লগইন ব্যর্থ: ${it.localizedMessage}")
            }
        }
    }

    fun logout() {
        repository.logout()
        cartItems.value = emptyList()
        showSnackbar("লগআউট সফল হয়েছে")
    }

    fun updateUserEmail(userId: String, newEmail: String) {
        viewModelScope.launch {
            val res = repository.updateUserEmail(userId, newEmail)
            res.onSuccess {
                showSnackbar("ইমেইল সফলভাবে পরিবর্তন করা হয়েছে: $newEmail")
            }.onFailure {
                showSnackbar("ইমেইল পরিবর্তন ব্যর্থ: ${it.localizedMessage}")
            }
        }
    }

    fun updateCurrentUserEmail(newEmail: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateUserEmail(user.userId, newEmail)
            res.onSuccess {
                showSnackbar("ইমেইল সফলভাবে পরিবর্তন করা হয়েছে: $newEmail")
            }.onFailure {
                showSnackbar("ইমেইল পরিবর্তন ব্যর্থ: ${it.localizedMessage}")
            }
        }
    }

    fun switchBusiness(bId: String) {
        repository.switchBusiness(bId)
        showSnackbar("ব্যবসা পরিবর্তন করা হয়েছে")
    }

    fun switchBranch(brId: String) {
        repository.switchBranch(brId)
        showSnackbar("শাখা পরিবর্তন করা হয়েছে")
    }

    fun hasPermission(permission: AppPermission): Boolean = repository.hasPermission(permission)

    // POS Cart Operations
    fun addToCart(product: ProductEntity, qty: Double = 1.0) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + qty
            current[index] = existing.copy(quantity = newQty)
        } else {
            current.add(AmarDokanRepository.CartItem(product = product, quantity = qty))
        }
        cartItems.value = current
        showSnackbar("${product.name} কার্টে যোগ করা হয়েছে")
    }

    fun updateCartItemQuantity(product: ProductEntity, qty: Double) {
        val current = cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            if (qty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = qty)
            }
            cartItems.value = current
        }
    }

    fun removeFromCart(product: ProductEntity) {
        val current = cartItems.value.toMutableList()
        current.removeAll { it.product.id == product.id }
        cartItems.value = current
    }

    fun clearCart() {
        cartItems.value = emptyList()
        selectedPosCustomer.value = null
        posDiscount.value = 0.0
        posPaidAmount.value = 0.0
    }

    fun processSale(note: String = "", onSuccess: (SaleEntity) -> Unit) {
        viewModelScope.launch {
            val items = cartItems.value
            val cust = selectedPosCustomer.value
            val disc = posDiscount.value
            val paid = posPaidAmount.value
            val method = posPaymentMethod.value

            val result = repository.processSale(
                cartItems = items,
                customer = cust,
                discountAmount = disc,
                paidAmount = paid,
                paymentMethod = method,
                note = note
            )

            result.onSuccess { sale ->
                clearCart()
                loadInvoice(sale)
                showSnackbar("চালান ${sale.invoiceNumber} সফলভাবে তৈরি হয়েছে!")
                onSuccess(sale)
            }.onFailure {
                showSnackbar("বিক্রি ব্যর্থ: ${it.localizedMessage}")
            }
        }
    }

    fun loadInvoice(sale: SaleEntity) {
        selectedSaleForInvoice.value = sale
        viewModelScope.launch {
            invoiceSaleItems.value = repository.getSaleItems(sale.saleId)
        }
    }

    // Stock Actions
    fun recordStockChange(product: ProductEntity, type: StockTransactionType, qty: Double, note: String) {
        viewModelScope.launch {
            val res = repository.recordStockChange(product, type, qty, note)
            res.onSuccess {
                showSnackbar("${product.name}-এর ${type.getDisplayNameBn()} সম্পন্ন হয়েছে")
            }.onFailure {
                showSnackbar("স্টক আপডেটে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun saveProduct(product: ProductEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val res = repository.saveProduct(product)
            res.onSuccess {
                showSnackbar("পণ্য সংরক্ষিত হয়েছে")
                onDone()
            }.onFailure {
                showSnackbar("পণ্য সংরক্ষণে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId).onSuccess {
                showSnackbar("পণ্য মুছে ফেলা হয়েছে")
            }
        }
    }

    // Customer Actions
    fun saveCustomer(customer: CustomerEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveCustomer(customer).onSuccess {
                showSnackbar("কাস্টমার তথ্য সংরক্ষিত হয়েছে")
                onDone()
            }.onFailure {
                showSnackbar("কাস্টমার সংরক্ষণে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun recordDuePayment(customer: CustomerEntity, amount: Double, method: PaymentMethod, note: String) {
        viewModelScope.launch {
            repository.recordCustomerDuePayment(customer, amount, method, note).onSuccess {
                showSnackbar("৳$amount বকেয়া আদায় সফল হয়েছে")
            }.onFailure {
                showSnackbar("পেমেন্ট রেকর্ডে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    // Expense Actions
    fun addExpense(title: String, category: String, amount: Double, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addExpense(title, category, amount, note).onSuccess {
                showSnackbar("খরচ ৳$amount যুক্ত হয়েছে")
                onDone()
            }.onFailure {
                showSnackbar("খরচ যুক্ত করতে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense).onSuccess {
                showSnackbar("খরচ এন্ট্রি মুছে ফেলা হয়েছে")
            }
        }
    }

    // Staff Actions
    fun createStaff(name: String, email: String, phone: String, branchId: String, role: UserRole, pass: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.createStaffAccount(name, email, phone, branchId, role, pass).onSuccess {
                showSnackbar("স্টাফ অ্যাকাউন্ট তৈরি হয়েছে (অনুমোদনের অপেক্ষায়)")
                onDone()
            }.onFailure {
                showSnackbar("স্টাফ তৈরিতে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun approveStaff(userId: String) {
        viewModelScope.launch {
            repository.updateStaffStatus(userId, UserStatus.ACTIVE).onSuccess {
                showSnackbar("স্টাফ অনুমোদন করা হয়েছে")
            }
        }
    }

    fun rejectStaff(userId: String) {
        viewModelScope.launch {
            repository.updateStaffStatus(userId, UserStatus.BLOCKED).onSuccess {
                showSnackbar("স্টাফ বাতিল/ব্লক করা হয়েছে")
            }
        }
    }

    fun updateStaffPermissions(userId: String, permissions: List<String>) {
        viewModelScope.launch {
            repository.updateStaffPermissions(userId, permissions).onSuccess {
                showSnackbar("পারমিশন আপডেট করা হয়েছে")
            }
        }
    }

    // Business & Branch Actions
    fun createBusiness(name: String, owner: String, phone: String, address: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.createBusiness(name, owner, phone, address).onSuccess {
                showSnackbar("নতুন দোকান/ব্যবসা তৈরি হয়েছে")
                onDone()
            }.onFailure {
                showSnackbar("ব্যবসা তৈরিতে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun createBranch(name: String, address: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.createBranch(name, address, phone).onSuccess {
                showSnackbar("নতুন ব্রাঞ্চ তৈরি হয়েছে")
                onDone()
            }.onFailure {
                showSnackbar("শাখা তৈরিতে ত্রুটি: ${it.localizedMessage}")
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            showSnackbar("ক্লাউড সিঙ্ক শুরু হয়েছে...")
            val success = repository.triggerManualCloudSync()
            if (success) {
                showSnackbar("ক্লাউড সিঙ্ক্রোনাইজেশন সম্পন্ন হয়েছে!")
            } else {
                showSnackbar("সিঙ্কে সমস্যা হয়েছে। অফলাইন মোডে সংরক্ষিত আছে।")
            }
        }
    }

    // ==========================================
    // NOTIFICATIONS
    // ==========================================
    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            showSnackbar("সকল নোটিফিকেশন পড়া হিসেবে চিহ্নিত হয়েছে")
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            showSnackbar("সকল নোটিফিকেশন মুছে ফেলা হয়েছে")
        }
    }

    fun triggerTestNotification(type: NotificationType) {
        repository.triggerTestNotification(type)
        showSnackbar("টেস্ট নোটিফিকেশন তৈরি করা হয়েছে (${type.getDisplayNameBn()})")
    }

    fun handleNotificationNavigation(destination: String?, action: String?, entityId: String?) {
        when (destination?.uppercase()) {
            "STOCK" -> activeDestination.value = AppDestination.STOCK
            "CUSTOMERS" -> activeDestination.value = AppDestination.CUSTOMERS
            "POS" -> activeDestination.value = AppDestination.POS
            "REPORTS" -> activeDestination.value = AppDestination.REPORTS
            "STAFF" -> {
                // If direct approve action requested from notification
                if (action == "APPROVE_STAFF" && !entityId.isNullOrBlank()) {
                    approveStaff(entityId)
                }
            }
        }
    }

    fun showSnackbar(msg: String) {
        snackbarMessage.value = msg
    }

    fun dismissSnackbar() {
        snackbarMessage.value = null
    }
}
