package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.PaymentMethod
import com.example.data.model.StockTransactionType
import com.example.data.model.SyncStatus
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.model.NotificationType

class RoomConverters {

    @TypeConverter
    fun fromNotificationType(type: NotificationType?): String? = type?.name

    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? = value?.let {
        try { NotificationType.valueOf(it) } catch (e: Exception) { NotificationType.GENERAL }
    }

    @TypeConverter
    fun fromUserRole(role: UserRole?): String? = role?.name

    @TypeConverter
    fun toUserRole(value: String?): UserRole? = value?.let {
        try { UserRole.valueOf(it) } catch (e: Exception) { UserRole.STAFF }
    }

    @TypeConverter
    fun fromUserStatus(status: UserStatus?): String? = status?.name

    @TypeConverter
    fun toUserStatus(value: String?): UserStatus? = value?.let {
        try { UserStatus.valueOf(it) } catch (e: Exception) { UserStatus.PENDING }
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? = status?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let {
        try { SyncStatus.valueOf(it) } catch (e: Exception) { SyncStatus.PENDING }
    }

    @TypeConverter
    fun fromStockTransactionType(type: StockTransactionType?): String? = type?.name

    @TypeConverter
    fun toStockTransactionType(value: String?): StockTransactionType? = value?.let {
        try { StockTransactionType.valueOf(it) } catch (e: Exception) { StockTransactionType.IN }
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod?): String? = method?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let {
        try { PaymentMethod.valueOf(it) } catch (e: Exception) { PaymentMethod.CASH }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.joinToString(",")

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        if (value.isNullOrBlank()) emptyList() else value.split(",")
}
