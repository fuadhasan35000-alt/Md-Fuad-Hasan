package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 1. Business Entity (Multi-Business Architecture)
 */
@Entity(
    tableName = "businesses",
    indices = [Index(value = ["businessId"], unique = true)]
)
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val businessId: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val taxNumber: String = "",
    val currencySymbol: String = "৳",
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 2. Branch Entity (Multi-Branch Support)
 */
@Entity(
    tableName = "branches",
    indices = [
        Index(value = ["branchId"], unique = true),
        Index(value = ["businessId", "branchId"])
    ]
)
data class BranchEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val branchId: String,
    val businessId: String,
    val name: String,
    val address: String,
    val phone: String,
    val managerId: String = "",
    val status: String = "ACTIVE", // ACTIVE, DISABLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 3. User Entity (Role Based Access & Staff Approval)
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["userId"], unique = true),
        Index(value = ["email"], unique = true),
        Index(value = ["businessId", "branchId"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val userId: String,
    val businessId: String,
    val branchId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val status: UserStatus,
    val permissions: List<String>, // Serialized list of permission codes
    val passwordHash: String = "", // Hashed password representation for offline authentication
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 4. Shop Settings Entity
 */
@Entity(
    tableName = "shop_settings",
    indices = [Index(value = ["businessId"], unique = true)]
)
data class ShopSettingsEntity(
    @PrimaryKey val businessId: String,
    val negativeStockAllowed: Boolean = false,
    val invoiceHeader: String = "আমার দোকান ক্যাশ মেমো",
    val invoiceFooter: String = "ধন্যবাদ, আবার আসবেন!",
    val vatPercentage: Double = 0.0,
    val aiEnabled: Boolean = true,
    val autoSync: Boolean = true,
    val lowStockThresholdDefault: Int = 5,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 5. Category Entity
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["businessId", "name"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: String,
    val businessId: String,
    val name: String,
    val iconName: String = "category",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 6. Supplier Entity
 */
@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["businessId", "phone"])]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val company: String = "",
    val address: String = "",
    val totalPurchased: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 7. Product Entity
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["businessId", "branchId", "id"]),
        Index(value = ["businessId", "branchId", "sku"]),
        Index(value = ["businessId", "branchId", "barcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String = "",
    val businessId: String,
    val branchId: String,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val category: String = "সাধারণ",
    val purchasePrice: Double,
    val salePrice: Double,
    val currentStock: Double,
    val minimumStock: Double = 5.0,
    val unit: String = "পিস", // কেজি, লিটার, পিস, বক্স, ডজন ইত্যাদি
    val supplier: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 8. Stock Transaction Entity
 */
@Entity(
    tableName = "stock_transactions",
    indices = [
        Index(value = ["businessId", "branchId", "productId"]),
        Index(value = ["transactionId"], unique = true)
    ]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val transactionId: String,
    val businessId: String,
    val branchId: String,
    val productId: Long,
    val productName: String,
    val type: StockTransactionType,
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val note: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 9. Customer Entity (দেনা-পাওনা লেজার)
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerId"], unique = true),
        Index(value = ["businessId", "branchId"]),
        Index(value = ["businessId", "phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val customerId: String,
    val businessId: String,
    val branchId: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 10. Sale / POS Entity
 */
@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["saleId"], unique = true),
        Index(value = ["invoiceNumber"], unique = true),
        Index(value = ["businessId", "branchId"]),
        Index(value = ["customerId"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val saleId: String,
    val businessId: String,
    val branchId: String,
    val invoiceNumber: String,
    val customerId: String = "",
    val customerName: String = "সাধারণ কাস্টমার",
    val customerPhone: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val vatAmount: Double = 0.0,
    val total: Double,
    val paid: Double,
    val due: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val userId: String,
    val userName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 11. Sale Item Entity
 */
@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
    val saleId: String,
    val businessId: String,
    val branchId: String,
    val productId: Long,
    val productName: String,
    val unit: String = "পিস",
    val quantity: Double,
    val unitPrice: Double,
    val purchasePrice: Double,
    val subtotal: Double
)

/**
 * 12. Payment Entity (দেনা আদায় ও পরিশোধ)
 */
@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["paymentId"], unique = true),
        Index(value = ["businessId", "branchId", "customerId"])
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val paymentId: String,
    val businessId: String,
    val branchId: String,
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val receivedBy: String,
    val receivedByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 13. Expense Entity (খরচ হিসাব)
 */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["expenseId"], unique = true),
        Index(value = ["businessId", "branchId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val expenseId: String,
    val businessId: String,
    val branchId: String,
    val title: String,
    val category: String, // দোকান ভাড়া, বিদ্যুৎ, পরিবহন, বেতন, ক্রয়, অন্যান্য
    val amount: Double,
    val note: String = "",
    val createdBy: String,
    val createdByName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

/**
 * 14. Audit Log Entity
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["businessId", "branchId"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val businessId: String,
    val branchId: String,
    val userId: String,
    val userName: String,
    val action: String, // e.g. "CREATE_SALE", "STOCK_IN", "APPROVE_STAFF"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 15. Offline Sync Queue Entity
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["businessId"]),
        Index(value = ["status"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val businessId: String,
    val branchId: String = "",
    val entityType: String, // "PRODUCT", "SALE", "CUSTOMER", "PAYMENT", "EXPENSE"
    val entityId: String,
    val operation: String, // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null
)

/**
 * 16. App Notification Entity (In-App & FCM Push Notification Store)
 */
@Entity(
    tableName = "app_notifications",
    indices = [
        Index(value = ["businessId", "branchId"]),
        Index(value = ["type"]),
        Index(value = ["createdAt"])
    ]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationId: String = UUID.randomUUID().toString(),
    val businessId: String = "",
    val branchId: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val title: String,
    val message: String,
    val targetRole: UserRole? = null, // null means all roles or role-appropriate
    val entityId: String = "", // productId, customerId/saleId, userId
    val actionType: String = "", // "VIEW_STOCK", "VIEW_DUE", "APPROVE_STAFF", "VIEW_REPORTS"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

