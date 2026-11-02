package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
import com.example.data.repository.CustomerBalanceSummary
import com.example.data.repository.ScrapRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    SPLASH,
    SETUP,
    HOME,
    PURCHASES,
    PURCHASE_HISTORY,
    PURCHASE_REPORTS,
    SALES,
    CUSTOMERS,
    CUSTOMER_DETAIL,
    EMPLOYEES,
    EXPENSES,
    CASHBOX,
    PRICING_PURCHASE,
    PRICING_SALE,
    REPORTS,
    SETTINGS,
    DEVELOPER_INFO
}

data class NewInvoiceItem(
    val scrapName: String,
    val unitPrice: Double,
    val weightKg: Double
) {
    val subtotal: Double get() = unitPrice * weightKg
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScrapRepository
    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ScrapRepository(db.shopDao())
        viewModelScope.launch {
            if (repository.getShopProfileSync() == null) {
                repository.saveShopProfile(
                    ShopProfileEntity(
                        id = 1,
                        ownerName = "مدير النظام",
                        shopName = "المتجر الرئيسي",
                        phoneNumber = "778215553",
                        address = "الفرع الرئيسي",
                        currencySymbol = "ريال",
                        isSetupCompleted = true
                    )
                )
            }
        }
    }

    // Navigation Stack
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen, clearStack: Boolean = false) {
        if (clearStack) {
            screenStack.clear()
            _currentScreen.value = screen
            return
        }
        if (_currentScreen.value != screen) {
            if (_currentScreen.value != AppScreen.SPLASH) {
                screenStack.add(_currentScreen.value)
            }
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        while (screenStack.isNotEmpty()) {
            val prev = screenStack.removeAt(screenStack.size - 1)
            if (prev != AppScreen.SPLASH) {
                _currentScreen.value = prev
                return true
            }
        }
        if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            return true
        }
        return false
    }

    // Hot & Eager Flow states - Always synchronized in real time!
    val shopProfile: StateFlow<ShopProfileEntity?> = repository.shopProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activePurchasePrice: StateFlow<DailyPriceEntity?> = repository.activePurchasePrice
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeSalePrice: StateFlow<DailyPriceEntity?> = repository.activeSalePrice
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allPurchasePrices: StateFlow<List<DailyPriceEntity>> = repository.allPurchasePrices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSalePrices: StateFlow<List<DailyPriceEntity>> = repository.allSalePrices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val purchaseInvoices: StateFlow<List<InvoiceEntity>> = repository.purchaseInvoices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val saleInvoices: StateFlow<List<InvoiceEntity>> = repository.saleInvoices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSettlements: StateFlow<List<SettlementEntity>> = repository.allSettlements
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<ScrapCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allEmployees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allEmployeeTransactions: StateFlow<List<EmployeeTransactionEntity>> = repository.allEmployeeTransactions
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Selected Customer for Details
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    private val _selectedCustomerSummary = MutableStateFlow<CustomerBalanceSummary?>(null)
    val selectedCustomerSummary: StateFlow<CustomerBalanceSummary?> = _selectedCustomerSummary.asStateFlow()

    fun selectCustomer(id: Long) {
        _selectedCustomerId.value = id
        loadCustomerSummary(id)
        navigateTo(AppScreen.CUSTOMER_DETAIL)
    }

    fun loadCustomerSummary(id: Long) {
        viewModelScope.launch {
            _selectedCustomerSummary.value = repository.getCustomerBalanceDetails(id)
        }
    }

    // UI Snackbars
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    fun showMessage(msg: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(msg)
        }
    }

    // Save Shop Profile
    fun saveProfile(ownerName: String, shopName: String, phone: String, address: String) {
        viewModelScope.launch {
            val current = repository.getShopProfileSync()
            repository.saveShopProfile(
                ShopProfileEntity(
                    id = 1,
                    ownerName = ownerName.trim(),
                    shopName = shopName.trim(),
                    phoneNumber = phone.trim(),
                    address = address.trim(),
                    isSetupCompleted = true,
                    currencySymbol = current?.currencySymbol ?: "ر.ي",
                    invoiceHeaderNote = current?.invoiceHeaderNote ?: "فاتورة خردة رسمية",
                    invoiceFooterNote = current?.invoiceFooterNote ?: "شكراً لتعاملكم معنا"
                )
            )
            showMessage("تم حفظ بيانات المحل بنجاح")
            navigateTo(AppScreen.HOME)
        }
    }

    fun saveInvoiceSettings(headerNote: String, footerNote: String, currency: String) {
        viewModelScope.launch {
            val current = repository.getShopProfileSync()
            if (current != null) {
                repository.saveShopProfile(
                    current.copy(
                        invoiceHeaderNote = headerNote.trim(),
                        invoiceFooterNote = footerNote.trim(),
                        currencySymbol = currency.trim()
                    )
                )
            } else {
                repository.saveShopProfile(
                    ShopProfileEntity(
                        id = 1,
                        ownerName = "مالك المحل",
                        shopName = "محل ابوطلال لتجارة الخردة",
                        phoneNumber = "778215553",
                        address = "صنعاء",
                        isSetupCompleted = true,
                        invoiceHeaderNote = headerNote.trim(),
                        invoiceFooterNote = footerNote.trim(),
                        currencySymbol = currency.trim()
                    )
                )
            }
            showMessage("تم حفظ إعدادات الفواتير والسندات بنجاح")
        }
    }

    // Daily Pricing Operations
    fun saveNewDailyPricing(
        type: String,
        dateStr: String,
        dayName: String,
        copperRed: Double,
        copperYellow: Double,
        metalLight: Double,
        metalHeavy: Double,
        aluminum: Double,
        battery: Double,
        iron: Double,
        plastic: Double,
        setActive: Boolean
    ) {
        viewModelScope.launch {
            val priceEntity = DailyPriceEntity(
                priceType = type,
                dateString = dateStr,
                dayName = dayName,
                timestamp = System.currentTimeMillis(),
                isActive = setActive,
                copperRedPrice = copperRed,
                copperYellowPrice = copperYellow,
                metalLightPrice = metalLight,
                metalHeavyPrice = metalHeavy,
                aluminumPrice = aluminum,
                batteryPrice = battery,
                ironPrice = iron,
                plasticPrice = plastic
            )
            repository.addDailyPrice(priceEntity, setActive)
            showMessage(if (type == "PURCHASE") "تم حفظ واعتماد تسعيرة مشتريات الخردة بنجاح" else "تم حفظ واعتماد تسعيرة مبيعات الخردة بنجاح")
        }
    }

    fun adoptPricing(id: Long, type: String) {
        viewModelScope.launch {
            repository.setActivePrice(id, type)
            showMessage("تم اعتماد هذه التسعيرة كتسعيرة نشطة اليوم")
        }
    }

    fun deleteDailyPrice(price: DailyPriceEntity) {
        viewModelScope.launch {
            repository.deleteDailyPrice(price)
            showMessage("تم حذف التسعيرة")
        }
    }

    // Invoices creation
    fun createInvoice(
        type: String,
        customerName: String,
        customerPhone: String,
        items: List<NewInvoiceItem>,
        paidAmount: Double,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        if (items.isEmpty()) {
            showMessage("يرجى إضافة صنف خردة واحد على الأقل")
            return
        }
        viewModelScope.launch {
            val entities = items.map { item ->
                InvoiceItemEntity(
                    invoiceId = 0,
                    scrapName = item.scrapName,
                    unitPrice = item.unitPrice,
                    weightKg = item.weightKg,
                    subtotal = item.subtotal
                )
            }
            val id = repository.createInvoice(
                type = type,
                customerName = customerName,
                customerPhone = customerPhone,
                items = entities,
                paidAmount = paidAmount,
                notes = notes
            )
            showMessage(if (type == "PURCHASE") "تم حفظ فاتورة الشراء بنجاح في سجل المشتريات" else "تم حفظ فاتورة البيع بنجاح في سجل المبيعات")
            onSuccess(id)
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            _selectedCustomerId.value?.let { loadCustomerSummary(it) }
            showMessage("تم حذف الفاتورة بنجاح")
        }
    }

    fun updateInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateInvoice(invoice, items)
            _selectedCustomerId.value?.let { loadCustomerSummary(it) }
            showMessage("تم تحديث وحفظ بيانات الفاتورة بنجاح")
            onSuccess()
        }
    }

    // Settlements
    fun addSettlement(customerId: Long, customerName: String, type: String, amount: Double, notes: String) {
        if (amount <= 0) {
            showMessage("يرجى إدخال مبلغ صحيح للتسديد")
            return
        }
        viewModelScope.launch {
            repository.addSettlement(customerId, customerName, type, amount, notes)
            loadCustomerSummary(customerId)
            showMessage("تم تسجيل سند التسديد بنجاح")
        }
    }

    fun deleteSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
            _selectedCustomerId.value?.let { loadCustomerSummary(it) }
            showMessage("تم حذف السند بنجاح")
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double, notes: String) {
        if (title.isBlank() || amount <= 0) {
            showMessage("يرجى إدخال بيان ومبلغ المصروف")
            return
        }
        viewModelScope.launch {
            repository.addExpense(title, category, amount, notes)
            showMessage("تم تسجيل المصروف بنجاح")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showMessage("تم حذف المصروف")
        }
    }

    // Customers management
    fun addOrUpdateCustomer(customer: CustomerEntity) {
        if (customer.name.isBlank()) {
            showMessage("يرجى كتابة اسم العميل")
            return
        }
        viewModelScope.launch {
            val id = repository.addOrUpdateCustomer(customer)
            loadCustomerSummary(id)
            showMessage("تم حفظ بيانات العميل")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("تم حذف العميل")
            navigateBack()
        }
    }

    // Backup & Restore
    suspend fun getBackupJson(): String {
        return repository.exportFullBackupJson()
    }

    fun restoreBackup(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.restoreFromBackupJson(jsonString)
            if (ok) {
                showMessage("تمت استعادة البيانات بنجاح تام")
            } else {
                showMessage("فشل استعادة النسخة الاحتياطية! تحقق من صحة الملف")
            }
            onComplete(ok)
        }
    }

    // Invoice Details & Printing
    suspend fun getInvoiceWithItems(invoiceId: Long): Pair<InvoiceEntity?, List<InvoiceItemEntity>> {
        val invoice = repository.getInvoiceById(invoiceId)
        val items = repository.getItemsForInvoiceSync(invoiceId)
        return Pair(invoice, items)
    }

    // Cashbox Direct Operations (إيداع وسحب نقدي)
    fun addCashTransaction(type: String, amount: Double, reason: String, personName: String) {
        if (amount <= 0) {
            showMessage("يرجى إدخال مبلغ صحيح")
            return
        }
        val isDeposit = type == "DEPOSIT"
        val settlementType = if (isDeposit) "CASH_DEPOSIT" else "CASH_WITHDRAWAL"
        val label = if (isDeposit) "إيداع في الصندوق: $reason" else "سحب من الصندوق: $reason"
        val customerTitle = personName.ifBlank { if (isDeposit) "إيداع نقدي مباشر" else "سحب نقدي مباشر" }

        viewModelScope.launch {
            repository.addSettlement(
                customerId = 0,
                customerName = customerTitle,
                type = settlementType,
                amount = amount,
                notes = label
            )
            showMessage(if (isDeposit) "تم إيداع $amount في الصندوق بنجاح" else "تم سحب $amount من الصندوق بنجاح")
        }
    }

    // Custom Scrap Categories
    fun addCustomCategory(name: String, purchasePrice: Double = 0.0, salePrice: Double = 0.0) {
        if (name.isBlank()) {
            showMessage("يرجى كتابة اسم الصنف")
            return
        }
        viewModelScope.launch {
            repository.addCategory(
                ScrapCategoryEntity(
                    name = name.trim(),
                    defaultPurchasePrice = purchasePrice,
                    defaultSalePrice = salePrice,
                    unit = "كجم"
                )
            )
            showMessage("تمت إضافة صنف $name بنجاح")
        }
    }

    fun deleteCustomCategory(category: ScrapCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showMessage("تم حذف الصنف")
        }
    }

    // Employees Management
    fun addOrUpdateEmployee(employee: EmployeeEntity) {
        if (employee.name.isBlank()) {
            showMessage("يرجى إدخال اسم الموظف")
            return
        }
        viewModelScope.launch {
            repository.addOrUpdateEmployee(employee)
            showMessage("تم حفظ بيانات الموظف بنجاح")
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            showMessage("تم حذف الموظف")
        }
    }

    fun payEmployee(
        employee: EmployeeEntity,
        transactionType: String,
        amount: Double,
        notes: String
    ) {
        if (amount <= 0) {
            showMessage("يرجى إدخال مبلغ صحيح")
            return
        }
        viewModelScope.launch {
            repository.payEmployee(employee, transactionType, amount, notes)
            val actionName = when (transactionType) {
                "SALARY" -> "صرف راتب"
                "ADVANCE" -> "تسجيل سلفة"
                "BONUS" -> "صرف مكافأة"
                else -> "تسجيل خصم"
            }
            showMessage("تم $actionName بمبلغ $amount للموظف ${employee.name} بنجاح")
        }
    }

    fun deleteEmployeeTransaction(transaction: EmployeeTransactionEntity) {
        viewModelScope.launch {
            repository.deleteEmployeeTransaction(transaction)
            showMessage("تم حذف السجل")
        }
    }
}
