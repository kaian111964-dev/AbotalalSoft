package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey val id: Int = 1,
    val ownerName: String,
    val shopName: String,
    val phoneNumber: String,
    val address: String,
    val isSetupCompleted: Boolean = true,
    val currencySymbol: String = "ر.ي",
    val invoiceHeaderNote: String = "فاتورة خردة رسمية",
    val invoiceFooterNote: String = "شكراً لتعاملكم معنا - نشتري كافة أنواع الخردة والمعادن بأفضل الأسعار"
)

@Entity(tableName = "scrap_categories")
data class ScrapCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultPurchasePrice: Double = 0.0,
    val defaultSalePrice: Double = 0.0,
    val unit: String = "كجم",
    val colorHex: String = "#0D47A1"
)

@Entity(tableName = "daily_prices")
data class DailyPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val priceType: String, // "PURCHASE" or "SALE"
    val dateString: String, // e.g. "2026-10-30"
    val dayName: String, // e.g. "الجمعة"
    val timestamp: Long,
    val isActive: Boolean = true,
    val copperRedPrice: Double = 0.0,
    val copperYellowPrice: Double = 0.0,
    val metalLightPrice: Double = 0.0,
    val metalHeavyPrice: Double = 0.0,
    val aluminumPrice: Double = 0.0,
    val batteryPrice: Double = 0.0,
    val ironPrice: Double = 0.0,
    val plasticPrice: Double = 0.0,
    val customPricesJson: String = "{}"
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val invoiceType: String, // "PURCHASE" or "SALE"
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val dateString: String,
    val timestamp: Long,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double, // for purchase: amount shop owes client. for sale: amount buyer owes shop.
    val isDebt: Boolean,
    val notes: String = ""
)

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val scrapName: String,
    val unitPrice: Double,
    val weightKg: Double,
    val subtotal: Double,
    val notes: String = ""
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val customerType: String = "ALL", // "SUPPLIER" (بائع خردة), "BUYER" (مشتري خردة), or "ALL"
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val type: String, // "PAYMENT_TO_SUPPLIER" (صرف للمورد / العميل) or "RECEIPT_FROM_BUYER" (قبض من المشتري)
    val amount: Double,
    val dateString: String,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "عمالة وأجور", "نقل وشحن", "إيجار المحل", "كهرباء ومياه", "صيانة", "أخرى"
    val amount: Double,
    val dateString: String,
    val timestamp: Long,
    val notes: String = ""
)
