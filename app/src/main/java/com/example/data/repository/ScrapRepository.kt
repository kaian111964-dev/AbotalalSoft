package com.example.data.repository

import com.example.data.local.dao.ShopDao
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CustomerBalanceSummary(
    val customer: CustomerEntity,
    val owedToCustomer: Double, // متبقي له (له في ذمة المحل من المشتريات الآجلة)
    val owedByCustomer: Double, // متبقي عليه (عليه للمحل من المبيعات الآجلة)
    val totalPurchases: Double,
    val totalSales: Double
)

class ScrapRepository(private val dao: ShopDao) {

    val shopProfile: Flow<ShopProfileEntity?> = dao.getShopProfile()
    val allCategories: Flow<List<ScrapCategoryEntity>> = dao.getAllCategories()
    val activePurchasePrice: Flow<DailyPriceEntity?> = dao.getActivePrice("PURCHASE")
    val activeSalePrice: Flow<DailyPriceEntity?> = dao.getActivePrice("SALE")
    val allPurchasePrices: Flow<List<DailyPriceEntity>> = dao.getPricesByType("PURCHASE")
    val allSalePrices: Flow<List<DailyPriceEntity>> = dao.getPricesByType("SALE")
    val allInvoices: Flow<List<InvoiceEntity>> = dao.getAllInvoices()
    val purchaseInvoices: Flow<List<InvoiceEntity>> = dao.getInvoicesByType("PURCHASE")
    val saleInvoices: Flow<List<InvoiceEntity>> = dao.getInvoicesByType("SALE")
    val allCustomers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val allSettlements: Flow<List<SettlementEntity>> = dao.getAllSettlements()
    val allEmployees: Flow<List<EmployeeEntity>> = dao.getAllEmployees()
    val allEmployeeTransactions: Flow<List<EmployeeTransactionEntity>> = dao.getAllEmployeeTransactions()

    suspend fun getShopProfileSync(): ShopProfileEntity? = dao.getShopProfileSync()

    suspend fun saveShopProfile(profile: ShopProfileEntity) {
        dao.saveShopProfile(profile)
    }

    suspend fun addCategory(category: ScrapCategoryEntity): Long {
        return dao.insertCategory(category)
    }

    suspend fun deleteCategory(category: ScrapCategoryEntity) {
        dao.deleteCategory(category)
    }

    suspend fun addDailyPrice(price: DailyPriceEntity, setAsActive: Boolean = true): Long {
        if (setAsActive) {
            dao.deactivateAllPrices(price.priceType)
        }
        return dao.insertDailyPrice(price.copy(isActive = setAsActive))
    }

    suspend fun setActivePrice(id: Long, type: String) {
        dao.deactivateAllPrices(type)
        dao.activatePrice(id)
    }

    suspend fun deleteDailyPrice(price: DailyPriceEntity) {
        dao.deleteDailyPrice(price)
    }

    // Invoice creation
    suspend fun createInvoice(
        type: String, // "PURCHASE" or "SALE"
        customerName: String,
        customerPhone: String,
        items: List<InvoiceItemEntity>,
        paidAmount: Double,
        notes: String
    ): Long {
        val totalAmount = items.sumOf { it.subtotal }
        val remaining = (totalAmount - paidAmount).coerceAtLeast(0.0)
        val isDebt = remaining > 0

        // Ensure customer exists
        val trimmedName = customerName.trim().ifEmpty { if (type == "PURCHASE") "عميل مورد نقدي" else "مشتري نقدي" }
        var customer = dao.getCustomerByName(trimmedName)
        val customerId = if (customer == null) {
            val newCust = CustomerEntity(
                name = trimmedName,
                phone = customerPhone.trim(),
                customerType = if (type == "PURCHASE") "SUPPLIER" else "BUYER"
            )
            dao.insertCustomer(newCust)
        } else {
            if (customer.phone.isEmpty() && customerPhone.isNotBlank()) {
                dao.updateCustomer(customer.copy(phone = customerPhone.trim()))
            }
            customer.id
        }

        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))
        val prefix = if (type == "PURCHASE") "PUR" else "SAL"
        val invoiceNumber = "$prefix-${now.toString().takeLast(6)}"

        val invoice = InvoiceEntity(
            invoiceNumber = invoiceNumber,
            invoiceType = type,
            customerId = customerId,
            customerName = trimmedName,
            customerPhone = customerPhone.trim(),
            dateString = dateStr,
            timestamp = now,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            remainingAmount = remaining,
            isDebt = isDebt,
            notes = notes
        )

        val invoiceId = dao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        dao.insertInvoiceItems(itemsWithId)

        return invoiceId
    }

    fun getItemsForInvoice(invoiceId: Long): Flow<List<InvoiceItemEntity>> = dao.getItemsForInvoice(invoiceId)
    suspend fun getItemsForInvoiceSync(invoiceId: Long): List<InvoiceItemEntity> = dao.getItemsForInvoiceSync(invoiceId)
    suspend fun getInvoiceById(id: Long): InvoiceEntity? = dao.getInvoiceById(id)

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        dao.deleteItemsForInvoice(invoice.id)
        dao.deleteInvoice(invoice)
    }

    suspend fun updateInvoice(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ) {
        val totalAmount = items.sumOf { it.subtotal }
        val remaining = (totalAmount - invoice.paidAmount).coerceAtLeast(0.0)
        val updated = invoice.copy(
            totalAmount = totalAmount,
            remainingAmount = remaining,
            isDebt = remaining > 0
        )
        dao.updateInvoice(updated)
        dao.deleteItemsForInvoice(invoice.id)
        val itemsWithId = items.map { it.copy(invoiceId = invoice.id) }
        dao.insertInvoiceItems(itemsWithId)
    }

    // Settlements
    suspend fun addSettlement(
        customerId: Long,
        customerName: String,
        type: String, // "PAYMENT_TO_SUPPLIER" or "RECEIPT_FROM_BUYER"
        amount: Double,
        notes: String
    ): Long {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val settlement = SettlementEntity(
            customerId = customerId,
            customerName = customerName,
            type = type,
            amount = amount,
            dateString = dateFormat.format(Date(now)),
            timestamp = now,
            notes = notes
        )
        return dao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlement: SettlementEntity) {
        dao.deleteSettlement(settlement)
    }

    // Expenses
    suspend fun addExpense(title: String, category: String, amount: Double, notes: String): Long {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val expense = ExpenseEntity(
            title = title,
            category = category,
            amount = amount,
            dateString = dateFormat.format(Date(now)),
            timestamp = now,
            notes = notes
        )
        return dao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        dao.deleteExpense(expense)
    }

    // Customers
    suspend fun addOrUpdateCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            dao.insertCustomer(customer)
        } else {
            dao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        dao.deleteCustomer(customer)
    }

    suspend fun getCustomerBalanceDetails(customerId: Long): CustomerBalanceSummary? {
        val customer = dao.getCustomerById(customerId) ?: return null
        val invoices = dao.getInvoicesForCustomerSync(customerId)
        val settlements = dao.getSettlementsForCustomerSync(customerId)

        // Purchases from this customer: shop owes customer remaining
        val purchaseRemaining = invoices.filter { it.invoiceType == "PURCHASE" }.sumOf { it.remainingAmount }
        val purchaseSettlementsPaid = settlements.filter { it.type == "PAYMENT_TO_SUPPLIER" }.sumOf { it.amount }
        val netOwedToCustomer = (purchaseRemaining - purchaseSettlementsPaid).coerceAtLeast(0.0)

        // Sales to this customer: customer owes shop remaining
        val saleRemaining = invoices.filter { it.invoiceType == "SALE" }.sumOf { it.remainingAmount }
        val saleSettlementsReceived = settlements.filter { it.type == "RECEIPT_FROM_BUYER" }.sumOf { it.amount }
        val netOwedByCustomer = (saleRemaining - saleSettlementsReceived).coerceAtLeast(0.0)

        val totalPurchases = invoices.filter { it.invoiceType == "PURCHASE" }.sumOf { it.totalAmount }
        val totalSales = invoices.filter { it.invoiceType == "SALE" }.sumOf { it.totalAmount }

        return CustomerBalanceSummary(
            customer = customer,
            owedToCustomer = netOwedToCustomer,
            owedByCustomer = netOwedByCustomer,
            totalPurchases = totalPurchases,
            totalSales = totalSales
        )
    }

    // Full JSON Backup Export
    suspend fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "ابوطلال سوفت")

        // Profile
        dao.getShopProfileSync()?.let { p ->
            val pObj = JSONObject()
            pObj.put("ownerName", p.ownerName)
            pObj.put("shopName", p.shopName)
            pObj.put("phoneNumber", p.phoneNumber)
            pObj.put("address", p.address)
            root.put("shopProfile", pObj)
        }

        // Daily Prices
        val pricesArr = JSONArray()
        dao.getAllDailyPricesSync().forEach { dp ->
            val obj = JSONObject()
            obj.put("priceType", dp.priceType)
            obj.put("dateString", dp.dateString)
            obj.put("dayName", dp.dayName)
            obj.put("timestamp", dp.timestamp)
            obj.put("isActive", dp.isActive)
            obj.put("copperRedPrice", dp.copperRedPrice)
            obj.put("copperYellowPrice", dp.copperYellowPrice)
            obj.put("metalLightPrice", dp.metalLightPrice)
            obj.put("metalHeavyPrice", dp.metalHeavyPrice)
            obj.put("aluminumPrice", dp.aluminumPrice)
            obj.put("batteryPrice", dp.batteryPrice)
            obj.put("ironPrice", dp.ironPrice)
            obj.put("plasticPrice", dp.plasticPrice)
            pricesArr.put(obj)
        }
        root.put("dailyPrices", pricesArr)

        // Customers
        val custArr = JSONArray()
        dao.getAllCustomersSync().forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("address", c.address)
            obj.put("customerType", c.customerType)
            obj.put("notes", c.notes)
            custArr.put(obj)
        }
        root.put("customers", custArr)

        // Invoices
        val invArr = JSONArray()
        dao.getAllInvoicesSync().forEach { inv ->
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("invoiceNumber", inv.invoiceNumber)
            obj.put("invoiceType", inv.invoiceType)
            obj.put("customerId", inv.customerId ?: 0)
            obj.put("customerName", inv.customerName)
            obj.put("customerPhone", inv.customerPhone)
            obj.put("dateString", inv.dateString)
            obj.put("timestamp", inv.timestamp)
            obj.put("totalAmount", inv.totalAmount)
            obj.put("paidAmount", inv.paidAmount)
            obj.put("remainingAmount", inv.remainingAmount)
            obj.put("isDebt", inv.isDebt)
            obj.put("notes", inv.notes)
            invArr.put(obj)
        }
        root.put("invoices", invArr)

        // Invoice Items
        val itemsArr = JSONArray()
        dao.getAllInvoiceItemsSync().forEach { item ->
            val obj = JSONObject()
            obj.put("invoiceId", item.invoiceId)
            obj.put("scrapName", item.scrapName)
            obj.put("unitPrice", item.unitPrice)
            obj.put("weightKg", item.weightKg)
            obj.put("subtotal", item.subtotal)
            itemsArr.put(obj)
        }
        root.put("invoiceItems", itemsArr)

        // Settlements
        val setArr = JSONArray()
        dao.getAllSettlementsSync().forEach { s ->
            val obj = JSONObject()
            obj.put("customerId", s.customerId)
            obj.put("customerName", s.customerName)
            obj.put("type", s.type)
            obj.put("amount", s.amount)
            obj.put("dateString", s.dateString)
            obj.put("timestamp", s.timestamp)
            obj.put("notes", s.notes)
            setArr.put(obj)
        }
        root.put("settlements", setArr)

        // Expenses
        val expArr = JSONArray()
        dao.getAllExpensesSync().forEach { exp ->
            val obj = JSONObject()
            obj.put("title", exp.title)
            obj.put("category", exp.category)
            obj.put("amount", exp.amount)
            obj.put("dateString", exp.dateString)
            obj.put("timestamp", exp.timestamp)
            obj.put("notes", exp.notes)
            expArr.put(obj)
        }
        root.put("expenses", expArr)

        return root.toString(2)
    }

    // Full JSON Backup Restore
    suspend fun restoreFromBackupJson(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)

            // Restore Profile if available
            if (root.has("shopProfile")) {
                val pObj = root.getJSONObject("shopProfile")
                val profile = ShopProfileEntity(
                    id = 1,
                    ownerName = pObj.optString("ownerName", "عبدالحميد داوؤد"),
                    shopName = pObj.optString("shopName", "ابوطلال سوفت"),
                    phoneNumber = pObj.optString("phoneNumber", "778215553"),
                    address = pObj.optString("address", "صنعاء"),
                    isSetupCompleted = true
                )
                dao.saveShopProfile(profile)
            }

            // Restore Daily Prices
            if (root.has("dailyPrices")) {
                val pricesArr = root.getJSONArray("dailyPrices")
                for (i in 0 until pricesArr.length()) {
                    val obj = pricesArr.getJSONObject(i)
                    val price = DailyPriceEntity(
                        priceType = obj.optString("priceType", "PURCHASE"),
                        dateString = obj.optString("dateString", ""),
                        dayName = obj.optString("dayName", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", false),
                        copperRedPrice = obj.optDouble("copperRedPrice", 0.0),
                        copperYellowPrice = obj.optDouble("copperYellowPrice", 0.0),
                        metalLightPrice = obj.optDouble("metalLightPrice", 0.0),
                        metalHeavyPrice = obj.optDouble("metalHeavyPrice", 0.0),
                        aluminumPrice = obj.optDouble("aluminumPrice", 0.0),
                        batteryPrice = obj.optDouble("batteryPrice", 0.0),
                        ironPrice = obj.optDouble("ironPrice", 0.0),
                        plasticPrice = obj.optDouble("plasticPrice", 0.0)
                    )
                    dao.insertDailyPrice(price)
                }
            }

            // Restore Customers
            if (root.has("customers")) {
                val custArr = root.getJSONArray("customers")
                for (i in 0 until custArr.length()) {
                    val obj = custArr.getJSONObject(i)
                    val customer = CustomerEntity(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name"),
                        phone = obj.optString("phone"),
                        address = obj.optString("address"),
                        customerType = obj.optString("customerType", "ALL"),
                        notes = obj.optString("notes")
                    )
                    dao.insertCustomer(customer)
                }
            }

            // Restore Invoices
            if (root.has("invoices")) {
                val invArr = root.getJSONArray("invoices")
                for (i in 0 until invArr.length()) {
                    val obj = invArr.getJSONObject(i)
                    val invoice = InvoiceEntity(
                        id = obj.optLong("id", 0),
                        invoiceNumber = obj.optString("invoiceNumber"),
                        invoiceType = obj.optString("invoiceType", "PURCHASE"),
                        customerId = obj.optLong("customerId"),
                        customerName = obj.optString("customerName"),
                        customerPhone = obj.optString("customerPhone"),
                        dateString = obj.optString("dateString"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        totalAmount = obj.optDouble("totalAmount", 0.0),
                        paidAmount = obj.optDouble("paidAmount", 0.0),
                        remainingAmount = obj.optDouble("remainingAmount", 0.0),
                        isDebt = obj.optBoolean("isDebt", false),
                        notes = obj.optString("notes")
                    )
                    dao.insertInvoice(invoice)
                }
            }

            // Restore Invoice Items
            if (root.has("invoiceItems")) {
                val itemsArr = root.getJSONArray("invoiceItems")
                val items = mutableListOf<InvoiceItemEntity>()
                for (i in 0 until itemsArr.length()) {
                    val obj = itemsArr.getJSONObject(i)
                    items.add(
                        InvoiceItemEntity(
                            invoiceId = obj.optLong("invoiceId"),
                            scrapName = obj.optString("scrapName"),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            weightKg = obj.optDouble("weightKg", 0.0),
                            subtotal = obj.optDouble("subtotal", 0.0)
                        )
                    )
                }
                dao.insertInvoiceItems(items)
            }

            // Restore Settlements
            if (root.has("settlements")) {
                val setArr = root.getJSONArray("settlements")
                for (i in 0 until setArr.length()) {
                    val obj = setArr.getJSONObject(i)
                    dao.insertSettlement(
                        SettlementEntity(
                            customerId = obj.optLong("customerId"),
                            customerName = obj.optString("customerName"),
                            type = obj.optString("type"),
                            amount = obj.optDouble("amount", 0.0),
                            dateString = obj.optString("dateString"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // Restore Expenses
            if (root.has("expenses")) {
                val expArr = root.getJSONArray("expenses")
                for (i in 0 until expArr.length()) {
                    val obj = expArr.getJSONObject(i)
                    dao.insertExpense(
                        ExpenseEntity(
                            title = obj.optString("title"),
                            category = obj.optString("category"),
                            amount = obj.optDouble("amount", 0.0),
                            dateString = obj.optString("dateString"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Employees Management
    suspend fun addOrUpdateEmployee(employee: EmployeeEntity): Long {
        return if (employee.id == 0L) {
            dao.insertEmployee(employee)
        } else {
            dao.updateEmployee(employee)
            employee.id
        }
    }

    suspend fun deleteEmployee(employee: EmployeeEntity) {
        dao.deleteEmployee(employee)
    }

    suspend fun payEmployee(
        employee: EmployeeEntity,
        transactionType: String, // "SALARY", "ADVANCE", "BONUS", "DEDUCTION"
        amount: Double,
        notes: String
    ): Long {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(now))

        val tx = EmployeeTransactionEntity(
            employeeId = employee.id,
            employeeName = employee.name,
            transactionType = transactionType,
            amount = amount,
            dateString = dateStr,
            timestamp = now,
            notes = notes
        )
        val txId = dao.insertEmployeeTransaction(tx)

        // Update balance: ADVANCE means employee owes money (negative), DEDUCTION reduces payout, BONUS adds credit
        val balanceDelta = when (transactionType) {
            "ADVANCE" -> -amount
            "DEDUCTION" -> -amount
            "BONUS" -> amount
            else -> 0.0
        }
        val updatedEmployee = employee.copy(balance = employee.balance + balanceDelta)
        dao.updateEmployee(updatedEmployee)

        // Record as operational expense under "رواتب ومستحقات الموظفين" so profit calculation is 100% accurate!
        val expenseCategory = "رواتب ومستحقات الموظفين"
        val expenseTitle = when (transactionType) {
            "SALARY" -> "صرف راتب: ${employee.name}"
            "ADVANCE" -> "سلفة موظف: ${employee.name}"
            "BONUS" -> "مكافأة موظف: ${employee.name}"
            else -> "خصم/مستحقات: ${employee.name}"
        }

        if (transactionType != "DEDUCTION") {
            dao.insertExpense(
                ExpenseEntity(
                    title = expenseTitle,
                    category = expenseCategory,
                    amount = amount,
                    dateString = dateStr,
                    timestamp = now,
                    notes = notes
                )
            )

            // Deduct from cashbox
            dao.insertSettlement(
                SettlementEntity(
                    customerId = 0,
                    customerName = "الموظف: ${employee.name}",
                    type = "CASH_WITHDRAWAL",
                    amount = amount,
                    dateString = dateStr,
                    timestamp = now,
                    notes = "$expenseTitle - $notes"
                )
            )
        }

        return txId
    }

    suspend fun deleteEmployeeTransaction(transaction: EmployeeTransactionEntity) {
        dao.deleteEmployeeTransaction(transaction)
    }
}
