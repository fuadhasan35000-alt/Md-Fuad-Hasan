package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE isActive = 1 ORDER BY localId ASC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE businessId = :businessId LIMIT 1")
    suspend fun getBusinessById(businessId: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE businessId = :businessId LIMIT 1")
    fun observeBusinessById(businessId: String): Flow<BusinessEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity): Long

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET isActive = :active WHERE businessId = :businessId")
    suspend fun setBusinessActive(businessId: String, active: Boolean)

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getBusinessCount(): Int
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches WHERE businessId = :businessId ORDER BY localId ASC")
    fun getBranchesForBusiness(businessId: String): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE businessId = :businessId AND branchId = :branchId LIMIT 1")
    suspend fun getBranchById(businessId: String, branchId: String): BranchEntity?

    @Query("SELECT * FROM branches WHERE businessId = :businessId AND branchId = :branchId LIMIT 1")
    fun observeBranchById(businessId: String, branchId: String): Flow<BranchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity): Long

    @Update
    suspend fun updateBranch(branch: BranchEntity)

    @Query("UPDATE branches SET status = :status WHERE businessId = :businessId AND branchId = :branchId")
    suspend fun updateBranchStatus(businessId: String, branchId: String, status: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE businessId = :businessId ORDER BY localId DESC")
    fun getUsersForBusiness(businessId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE businessId = :businessId AND status = 'PENDING' ORDER BY localId DESC")
    fun getPendingUsersForBusiness(businessId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET status = :status WHERE userId = :userId")
    suspend fun updateUserStatus(userId: String, status: UserStatus)

    @Query("UPDATE users SET permissions = :permissions WHERE userId = :userId")
    suspend fun updateUserPermissions(userId: String, permissions: List<String>)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'SUPER_ADMIN'")
    suspend fun getSuperAdminCount(): Int

    @Query("SELECT * FROM users WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveUserSync(): UserEntity?

    @Query("UPDATE users SET email = :newEmail WHERE email = :oldEmail OR role = 'SUPER_ADMIN'")
    suspend fun updateSuperAdminEmail(newEmail: String, oldEmail: String = "admin@amardokan.com")
}

@Dao
interface ShopSettingsDao {
    @Query("SELECT * FROM shop_settings WHERE businessId = :businessId LIMIT 1")
    fun observeSettings(businessId: String): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE businessId = :businessId LIMIT 1")
    suspend fun getSettings(businessId: String): ShopSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ShopSettingsEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE businessId = :businessId ORDER BY name ASC")
    fun getCategories(businessId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE businessId = :businessId ORDER BY name ASC")
    fun getSuppliers(businessId: String): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long
}

@Dao
interface ProductDao {
    @Query("""
        SELECT * FROM products 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId OR branchId = 'ALL')
          AND deletedAt IS NULL 
        ORDER BY id DESC
    """)
    fun getProducts(businessId: String, branchId: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId OR branchId = 'ALL')
          AND currentStock <= minimumStock 
          AND deletedAt IS NULL 
        ORDER BY currentStock ASC
    """)
    fun getLowStockProducts(businessId: String, branchId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE businessId = :businessId AND (sku = :query OR barcode = :query) AND deletedAt IS NULL LIMIT 1")
    suspend fun findByBarcodeOrSku(businessId: String, query: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStock = :newStock, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteProduct(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId AND deletedAt IS NULL")
    fun getProductCount(businessId: String): Flow<Int>
}

@Dao
interface StockTransactionDao {
    @Query("""
        SELECT * FROM stock_transactions 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
        ORDER BY createdAt DESC LIMIT 100
    """)
    fun getTransactions(businessId: String, branchId: String): Flow<List<StockTransactionEntity>>

    @Query("""
        SELECT * FROM stock_transactions 
        WHERE businessId = :businessId AND productId = :productId 
        ORDER BY createdAt DESC
    """)
    fun getTransactionsForProduct(businessId: String, productId: Long): Flow<List<StockTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity): Long
}

@Dao
interface CustomerDao {
    @Query("""
        SELECT * FROM customers 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
          AND deletedAt IS NULL 
        ORDER BY totalDue DESC, localId DESC
    """)
    fun getCustomers(businessId: String, branchId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND totalDue > 0 AND deletedAt IS NULL ORDER BY totalDue DESC")
    fun getDueCustomers(businessId: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("""
        UPDATE customers 
        SET totalPurchase = totalPurchase + :purchaseAmount, 
            totalDue = totalDue + :dueChange,
            totalPaid = totalPaid + :paidAmount,
            updatedAt = :timestamp 
        WHERE customerId = :customerId
    """)
    suspend fun updateCustomerBalanceOnSale(
        customerId: String,
        purchaseAmount: Double,
        dueChange: Double,
        paidAmount: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE customers 
        SET totalPaid = totalPaid + :paidAmount, 
            totalDue = totalDue - :paidAmount,
            updatedAt = :timestamp 
        WHERE customerId = :customerId
    """)
    suspend fun updateCustomerBalanceOnPayment(
        customerId: String,
        paidAmount: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE customers SET deletedAt = :timestamp WHERE customerId = :customerId")
    suspend fun softDeleteCustomer(customerId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT SUM(totalDue) FROM customers WHERE businessId = :businessId AND deletedAt IS NULL")
    fun getTotalDue(businessId: String): Flow<Double?>
}

@Dao
interface SaleDao {
    @Query("""
        SELECT * FROM sales 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
        ORDER BY createdAt DESC
    """)
    fun getSales(businessId: String, branchId: String): Flow<List<SaleEntity>>

    @Query("""
        SELECT * FROM sales 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
          AND createdAt >= :startTime AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    fun getSalesBetween(businessId: String, branchId: String, startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE saleId = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getSaleByInvoiceNumber(invoiceNumber: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getSalesForCustomer(customerId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT COUNT(*) FROM sales WHERE businessId = :businessId")
    suspend fun getSaleCount(businessId: String): Int
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun observeItemsForSale(saleId: String): Flow<List<SaleItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)
}

@Dao
interface PaymentDao {
    @Query("""
        SELECT * FROM payments 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
        ORDER BY createdAt DESC
    """)
    fun getPayments(businessId: String, branchId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("""
        SELECT * FROM payments 
        WHERE businessId = :businessId 
          AND createdAt >= :startTime AND createdAt <= :endTime
    """)
    fun getPaymentsBetween(businessId: String, startTime: Long, endTime: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long
}

@Dao
interface ExpenseDao {
    @Query("""
        SELECT * FROM expenses 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
        ORDER BY createdAt DESC
    """)
    fun getExpenses(businessId: String, branchId: String): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses 
        WHERE businessId = :businessId 
          AND (:branchId = '' OR branchId = :branchId)
          AND createdAt >= :startTime AND createdAt <= :endTime
        ORDER BY createdAt DESC
    """)
    fun getExpensesBetween(businessId: String, branchId: String, startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface AuditLogDao {
    @Query("""
        SELECT * FROM audit_logs 
        WHERE businessId = :businessId 
        ORDER BY timestamp DESC LIMIT 200
    """)
    fun getLogs(businessId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE businessId = :businessId AND status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingSyncItems(businessId: String): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun delete(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearCompleted()
}

@Dao
interface NotificationDao {
    @Query("""
        SELECT * FROM app_notifications 
        WHERE (businessId = :businessId OR businessId = '') 
          AND (branchId = :branchId OR branchId = '' OR :branchId = '')
        ORDER BY createdAt DESC
    """)
    fun getNotifications(businessId: String, branchId: String): Flow<List<AppNotificationEntity>>

    @Query("""
        SELECT COUNT(*) FROM app_notifications 
        WHERE isRead = 0 
          AND (businessId = :businessId OR businessId = '')
          AND (branchId = :branchId OR branchId = '' OR :branchId = '')
    """)
    fun getUnreadCount(businessId: String, branchId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE (businessId = :businessId OR businessId = '')")
    suspend fun markAllAsRead(businessId: String)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM app_notifications WHERE (businessId = :businessId OR businessId = '')")
    suspend fun clearAllNotifications(businessId: String)
}

