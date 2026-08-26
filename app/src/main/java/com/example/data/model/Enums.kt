package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Roles
 */
enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    STAFF;

    fun getDisplayNameBn(): String = when (this) {
        SUPER_ADMIN -> "সুপার অ্যাডমিন"
        ADMIN -> "অ্যাডমিন"
        STAFF -> "স্টাফ"
    }
}

/**
 * User Account Status (Supports Staff Approval Flow)
 */
enum class UserStatus {
    ACTIVE,
    PENDING,
    BLOCKED;

    fun getDisplayNameBn(): String = when (this) {
        ACTIVE -> "সক্রিয়"
        PENDING -> "অনুমোদনের অপেক্ষায়"
        BLOCKED -> "ব্লক করা"
    }
}

/**
 * Granular Centralized Permissions
 */
enum class AppPermission(val code: String, val titleBn: String, val descriptionBn: String) {
    VIEW_STOCK("VIEW_STOCK", "স্টক দেখা", "পণ্যের তালিকা ও বর্তমান স্টক পরিমাণ দেখার অনুমতি"),
    EDIT_STOCK("EDIT_STOCK", "স্টক পরিবর্তন", "স্টক ইন, স্টক আউট ও সমন্বয় করার অনুমতি"),
    ADD_PRODUCT("ADD_PRODUCT", "নতুন পণ্য যোগ", "নতুন পণ্য ডাটাবেজে যুক্ত করার অনুমতি"),
    DELETE_PRODUCT("DELETE_PRODUCT", "পণ্য মুছে ফেলা", "পণ্য তালিকা থেকে ডিলিট করার অনুমতি"),
    CREATE_SALE("CREATE_SALE", "বিক্রি করা / পিওএস", "বিক্রি সম্পন্ন ও চালান তৈরির অনুমতি"),
    VIEW_SALES("VIEW_SALES", "বিক্রি দেখা", "সকল বিক্রির রেকর্ড দেখার অনুমতি"),
    VIEW_CUSTOMERS("VIEW_CUSTOMERS", "কাস্টমার দেখা", "কাস্টমার তালিকা ও প্রোফাইল দেখার অনুমতি"),
    EDIT_CUSTOMERS("EDIT_CUSTOMERS", "কাস্টমার যোগ/সম্পাদনা", "কাস্টমার তথ্য তৈরি ও পরিবর্তনের অনুমতি"),
    RECORD_PAYMENT("RECORD_PAYMENT", "পেমেন্ট গ্রহণ", "দেনা আদায় ও পেমেন্ট রেকর্ড করার অনুমতি"),
    VIEW_DUE("VIEW_DUE", "বকেয়া হিসাব", "দেনা-পাওনা ও কাস্টমার বকেয়া দেখার অনুমতি"),
    VIEW_REPORTS("VIEW_REPORTS", "রিপোর্ট দেখা", "দৈনিক, মাসিক ও ব্রাঞ্চভিত্তিক রিপোর্ট দেখার অনুমতি"),
    MANAGE_EXPENSES("MANAGE_EXPENSES", "খরচ হিসাব", "দোকানের খরচ যোগ ও ব্যবস্থাপনা করার অনুমতি"),
    SEND_SMS("SEND_SMS", "এসএমএস পাঠানো", "চালান ও বকেয়া নোটিফিকেশন এসএমএস পাঠানোর অনুমতি"),
    SEND_WHATSAPP("SEND_WHATSAPP", "হোয়াটসঅ্যাপে শেয়ার", "হোয়াটসঅ্যাপে চালান ও রিমাইন্ডার পাঠানোর অনুমতি"),
    MANAGE_STAFF("MANAGE_STAFF", "স্টাফ ব্যবস্থাপনা", "স্টাফ অনুমোদন, যুক্ত ও পারমিশন ব্যবস্থাপনার অনুমতি"),
    MANAGE_BRANCH("MANAGE_BRANCH", "শাখা ব্যবস্থাপনা", "নতুন ব্রাঞ্চ খোলা ও নিয়ন্ত্রণের অনুমতি"),
    EXPORT_DATA("EXPORT_DATA", "ডাটা এক্সপোর্ট", "এক্সেল/সিএসভি এবং পিডিএফ এক্সপোর্টের অনুমতি"),
    BACKUP_DATA("BACKUP_DATA", "ব্যাকআপ নেওয়া", "ডাটাবেজ ব্যাকআপ নেওয়ার অনুমতি"),
    RESTORE_DATA("RESTORE_DATA", "রিস্টোর করা", "পূর্বে সংরক্ষিত ব্যাকআপ রিস্টোর করার অনুমতি");

    companion object {
        fun getDefaultPermissionsForRole(role: UserRole): Set<AppPermission> {
            return when (role) {
                UserRole.SUPER_ADMIN -> AppPermission.values().toSet()
                UserRole.ADMIN -> setOf(
                    VIEW_STOCK, EDIT_STOCK, ADD_PRODUCT, CREATE_SALE, VIEW_SALES,
                    VIEW_CUSTOMERS, EDIT_CUSTOMERS, RECORD_PAYMENT, VIEW_DUE,
                    VIEW_REPORTS, MANAGE_EXPENSES, SEND_SMS, SEND_WHATSAPP,
                    EXPORT_DATA, BACKUP_DATA
                )
                UserRole.STAFF -> setOf(
                    VIEW_STOCK, EDIT_STOCK, CREATE_SALE, VIEW_SALES,
                    VIEW_CUSTOMERS, EDIT_CUSTOMERS, RECORD_PAYMENT, VIEW_DUE,
                    SEND_SMS, SEND_WHATSAPP
                )
            }
        }
    }
}

/**
 * Sync status for offline-first architecture
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

/**
 * Stock transaction types
 */
enum class StockTransactionType {
    IN,
    OUT,
    ADJUSTMENT;

    fun getDisplayNameBn(): String = when (this) {
        IN -> "স্টক ইন (ক্রয়/যোগ)"
        OUT -> "স্টক আউট (বিক্রি/ক্ষতি)"
        ADJUSTMENT -> "সমন্বয় (অ্যাডজাস্টমেন্ট)"
    }
}

/**
 * Payment Methods
 */
enum class PaymentMethod {
    CASH,
    BKASH,
    NAGAD,
    ROCKET,
    CARD,
    BANK_TRANSFER;

    fun getDisplayNameBn(): String = when (this) {
        CASH -> "নগদ (Cash)"
        BKASH -> "বিকাশ (bKash)"
        NAGAD -> "নগদ (Nagad)"
        ROCKET -> "রকেট (Rocket)"
        CARD -> "কার্ড (Card)"
        BANK_TRANSFER -> "ব্যাংক ট্রান্সফার"
    }
}

/**
 * Notification Types for FCM and in-app alerts
 */
enum class NotificationType {
    LOW_STOCK,
    DUE_PAYMENT,
    STAFF_APPROVAL,
    GENERAL;

    fun getDisplayNameBn(): String = when (this) {
        LOW_STOCK -> "কম স্টক সতর্কতা"
        DUE_PAYMENT -> "বকেয়া ও পেমেন্ট অ্যালার্ট"
        STAFF_APPROVAL -> "স্টাফ অনুমোদন অনুরোধ"
        GENERAL -> "সাধারণ নোটিফিকেশন"
    }
}

