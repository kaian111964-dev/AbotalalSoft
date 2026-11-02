package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DailyPriceEntity
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.EmployeeTransactionEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ScrapCategoryEntity
import com.example.data.local.entity.SettlementEntity
import com.example.data.local.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // Shop Profile
    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    fun getShopProfile(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    suspend fun getShopProfileSync(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShopProfile(profile: ShopProfileEntity)

    // Scrap Categories
    @Query("SELECT * FROM scrap_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<ScrapCategoryEntity>>

    @Query("SELECT * FROM scrap_categories ORDER BY id ASC")
    suspend fun getAllCategoriesSync(): List<ScrapCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ScrapCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ScrapCategoryEntity>)

    @Update
    suspend fun updateCategory(category: ScrapCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: ScrapCategoryEntity)

    // Daily Prices
    @Query("SELECT * FROM daily_prices WHERE priceType = :type ORDER BY timestamp DESC")
    fun getPricesByType(type: String): Flow<List<DailyPriceEntity>>

    @Query("SELECT * FROM daily_prices WHERE priceType = :type AND isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getActivePrice(type: String): Flow<DailyPriceEntity?>

    @Query("SELECT * FROM daily_prices WHERE priceType = :type AND isActive = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActivePriceSync(type: String): DailyPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPrice(price: DailyPriceEntity): Long

    @Update
    suspend fun updateDailyPrice(price: DailyPriceEntity)

    @Query("UPDATE daily_prices SET isActive = 0 WHERE priceType = :type")
    suspend fun deactivateAllPrices(type: String)

    @Query("UPDATE daily_prices SET isActive = 1 WHERE id = :id")
    suspend fun activatePrice(id: Long)

    @Delete
    suspend fun deleteDailyPrice(price: DailyPriceEntity)

    // Invoices
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE invoiceType = :type ORDER BY timestamp DESC")
    fun getInvoicesByType(type: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getInvoicesForCustomer(customerId: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getInvoicesForCustomerSync(customerId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    // Invoice Items
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItemsForInvoiceSync(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsSync(): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: Long)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersSync(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    // Settlements
    @Query("SELECT * FROM settlements WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getSettlementsForCustomer(customerId: Long): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getSettlementsForCustomerSync(customerId: Long): List<SettlementEntity>

    @Query("SELECT * FROM settlements ORDER BY timestamp DESC")
    fun getAllSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements ORDER BY timestamp DESC")
    suspend fun getAllSettlementsSync(): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity): Long

    @Delete
    suspend fun deleteSettlement(settlement: SettlementEntity)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Backup & Restore bulk helpers
    @Query("SELECT * FROM invoices")
    suspend fun getAllInvoicesSync(): List<InvoiceEntity>

    @Query("SELECT * FROM daily_prices")
    suspend fun getAllDailyPricesSync(): List<DailyPriceEntity>

    @Query("DELETE FROM shop_profile")
    suspend fun clearShopProfile()

    @Query("DELETE FROM scrap_categories")
    suspend fun clearCategories()

    @Query("DELETE FROM daily_prices")
    suspend fun clearDailyPrices()

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()

    @Query("DELETE FROM invoice_items")
    suspend fun clearInvoiceItems()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM settlements")
    suspend fun clearSettlements()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    // Employees
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAllEmployeesSync(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeEntity)

    // Employee Transactions
    @Query("SELECT * FROM employee_transactions ORDER BY timestamp DESC")
    fun getAllEmployeeTransactions(): Flow<List<EmployeeTransactionEntity>>

    @Query("SELECT * FROM employee_transactions WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getTransactionsForEmployee(employeeId: Long): Flow<List<EmployeeTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployeeTransaction(transaction: EmployeeTransactionEntity): Long

    @Delete
    suspend fun deleteEmployeeTransaction(transaction: EmployeeTransactionEntity)
}
