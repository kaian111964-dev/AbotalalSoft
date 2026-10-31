package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.ui.MainViewModel
import com.example.ui.NewInvoiceItem
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val activePrice by viewModel.activePurchasePrice.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: New Purchase, 1: Invoices History

    // New Invoice Form State
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf("") }
    var isFullCash by remember { mutableStateOf(true) }

    // 8 Predefined Scrap Items
    val scrapList = listOf(
        "النحاس الأحمر", "النحاس الأصفر", "المعدن الخفيف", "المعدن الثقيل",
        "الألمنيوم", "البطاريات", "الحديد", "البلاستيك"
    )
    var selectedScrapName by remember { mutableStateOf(scrapList[0]) }
    var isScrapDropdownExpanded by remember { mutableStateOf(false) }

    var unitPriceText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val currentInvoiceItems = remember { mutableStateListOf<NewInvoiceItem>() }

    // Invoice View / Print Dialog state
    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    // Helper to get active price for scrap name
    fun getActivePriceFor(scrap: String): Double {
        return when (scrap) {
            "النحاس الأحمر" -> activePrice?.copperRedPrice ?: 0.0
            "النحاس الأصفر" -> activePrice?.copperYellowPrice ?: 0.0
            "المعدن الخفيف" -> activePrice?.metalLightPrice ?: 0.0
            "المعدن الثقيل" -> activePrice?.metalHeavyPrice ?: 0.0
            "الألمنيوم" -> activePrice?.aluminumPrice ?: 0.0
            "البطاريات" -> activePrice?.batteryPrice ?: 0.0
            "الحديد" -> activePrice?.ironPrice ?: 0.0
            "البلاستيك" -> activePrice?.plasticPrice ?: 0.0
            else -> 0.0
        }
    }

    // Set initial unit price when scrap selected or active price changes
    LaunchedEffect(selectedScrapName, activePrice) {
        val p = getActivePriceFor(selectedScrapName)
        if (p > 0.0) {
            unitPriceText = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
        }
    }

    val totalInvoiceAmount = currentInvoiceItems.sumOf { it.subtotal }
    val enteredPaidAmount = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val remainingToClient = (totalInvoiceAmount - enteredPaidAmount).coerceAtLeast(0.0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "مشتريات الخردة ووزن الأصناف",
                onBack = { viewModel.navigateBack() }
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs with live counter
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = BluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("وزن وشراء خردة (فاتورة جديدة)", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                    modifier = Modifier.testTag("tab_new_purchase")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سجل المشتريات (${purchaseInvoices.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    modifier = Modifier.testTag("tab_purchase_history")
                )
            }

            if (selectedTab == 0) {
                // New Purchase Form
                val formScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(formScroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Customer Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "بيانات المورد / العميل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("اسم العميل / المورد *") },
                                placeholder = { Text("اكتب اسم العميل أو اختر من السابقين") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("purchase_customer_name"),
                                singleLine = true
                            )

                            if (allCustomers.isNotEmpty()) {
                                Text("عملاء سابقين (اضغط للاختيار السريع):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(allCustomers.take(10)) { cust ->
                                        SuggestionChip(
                                            onClick = {
                                                customerName = cust.name
                                                customerPhone = cust.phone
                                            },
                                            label = { Text(cust.name, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("رقم الهاتف (اختياري)") },
                                placeholder = { Text("770000000") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("purchase_customer_phone"),
                                singleLine = true
                            )
                        }
                    }

                    // Add Scrap Item Card with DROPDOWN MENU (قائمة منسدلة حسب طلب المستخدم)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إضافة صنف خردة للميزان",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                val p = getActivePriceFor(selectedScrapName)
                                if (p > 0) {
                                    Text(
                                        text = "السعر اليوم: ${formatCurrency(p, shopProfile?.currencySymbol ?: "ر.ي")}/كجم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GreenSuccess,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Exposed Dropdown Menu Box
                            ExposedDropdownMenuBox(
                                expanded = isScrapDropdownExpanded,
                                onExpandedChange = { isScrapDropdownExpanded = !isScrapDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedScrapName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("اختر صنف الخردة (قائمة منسدلة)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isScrapDropdownExpanded) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(getScrapColor(selectedScrapName), CircleShape)
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("purchase_scrap_dropdown"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        focusedLabelColor = BluePrimary
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = isScrapDropdownExpanded,
                                    onDismissRequest = { isScrapDropdownExpanded = false }
                                ) {
                                    scrapList.forEach { scrap ->
                                        val activeItemPrice = getActivePriceFor(scrap)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .background(getScrapColor(scrap), CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(scrap, fontWeight = FontWeight.Bold)
                                                    }
                                                    if (activeItemPrice > 0) {
                                                        Text(
                                                            text = formatCurrency(activeItemPrice, shopProfile?.currencySymbol ?: "ر.ي"),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = BluePrimary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedScrapName = scrap
                                                val autoPrice = getActivePriceFor(scrap)
                                                if (autoPrice > 0.0) {
                                                    unitPriceText = if (autoPrice % 1.0 == 0.0) autoPrice.toLong().toString() else autoPrice.toString()
                                                }
                                                isScrapDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Weight in KG
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    label = { Text("الوزن (كيلو جرام) *") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null, tint = BluePrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("purchase_weight_input"),
                                    singleLine = true
                                )

                                // Unit Price in Riyals
                                OutlinedTextField(
                                    value = unitPriceText,
                                    onValueChange = { unitPriceText = it },
                                    label = { Text("سعر الكيلو *") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BluePrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("purchase_unit_price_input"),
                                    singleLine = true
                                )
                            }

                            val calculatedSubtotal = (weightText.toDoubleOrNull() ?: 0.0) * (unitPriceText.toDoubleOrNull() ?: 0.0)
                            if (calculatedSubtotal > 0) {
                                Surface(
                                    color = BlueLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("قيمة الصنف الموزون:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = formatCurrency(calculatedSubtotal, shopProfile?.currencySymbol ?: "ر.ي"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val w = weightText.toDoubleOrNull() ?: 0.0
                                    val p = unitPriceText.toDoubleOrNull() ?: 0.0
                                    if (w <= 0.0) {
                                        viewModel.showMessage("يرجى إدخال وزن صحيح بالكيلو")
                                    } else if (p <= 0.0) {
                                        viewModel.showMessage("يرجى إدخال سعر صحيح للكيلو")
                                    } else {
                                        currentInvoiceItems.add(
                                            NewInvoiceItem(
                                                scrapName = selectedScrapName,
                                                unitPrice = p,
                                                weightKg = w
                                            )
                                        )
                                        weightText = ""
                                        viewModel.showMessage("تمت إضافة $selectedScrapName بنجاح إلى الفاتورة")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_scrap_item_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة الصنف إلى الفاتورة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Added Items List Table
                    if (currentInvoiceItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "أصناف الفاتورة الموزونة (${currentInvoiceItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                currentInvoiceItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${index + 1}. ${item.scrapName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${formatWeight(item.weightKg)} × ${formatCurrency(item.unitPrice, shopProfile?.currencySymbol ?: "ر.ي")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = formatCurrency(item.subtotal, shopProfile?.currencySymbol ?: "ر.ي"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )

                                        IconButton(
                                            onClick = { currentInvoiceItems.removeAt(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    if (index < currentInvoiceItems.size - 1) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Payment & Settlement Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "طريقة الحساب والدفع",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )

                            // Total Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي الفاتورة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatCurrency(totalInvoiceAmount, shopProfile?.currencySymbol ?: "ر.ي"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }

                            // Payment mode toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isFullCash,
                                    onClick = { isFullCash = true },
                                    label = { Text("نقدي بالكامل (مسلَّم للعميل)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !isFullCash,
                                    onClick = { isFullCash = false },
                                    label = { Text("آجل / متبقي له في ذمتنا") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!isFullCash) {
                                OutlinedTextField(
                                    value = paidAmountText,
                                    onValueChange = { paidAmountText = it },
                                    label = { Text("المبلغ المدفوع نقدياً الآن") },
                                    placeholder = { Text("0.0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("purchase_paid_amount_input"),
                                    singleLine = true
                                )

                                Surface(
                                    color = RedLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("المتبقي له في ذمة المحل:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RedAccent)
                                        Text(
                                            text = formatCurrency(remainingToClient, shopProfile?.currencySymbol ?: "ر.ي"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RedAccent
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Save & Print Button
                            Button(
                                onClick = {
                                    if (currentInvoiceItems.isEmpty()) {
                                        viewModel.showMessage("يرجى إضافة صنف خردة واحد على الأقل للفاتورة")
                                        return@Button
                                    }
                                    val finalPaid = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
                                    viewModel.createInvoice(
                                        type = "PURCHASE",
                                        customerName = customerName.ifBlank { "عميل خردة نقدي" },
                                        customerPhone = customerPhone,
                                        items = currentInvoiceItems.toList(),
                                        paidAmount = finalPaid,
                                        notes = notes,
                                        onSuccess = { invoiceId ->
                                            coroutineScope.launch {
                                                val pair = viewModel.getInvoiceWithItems(invoiceId)
                                                viewingInvoice = pair.first
                                                viewingItems = pair.second
                                                showPrintDialog = true
                                                // Reset form
                                                currentInvoiceItems.clear()
                                                customerName = ""
                                                customerPhone = ""
                                                paidAmountText = ""
                                                notes = ""
                                                isFullCash = true
                                                // Automatically switch to history tab so user sees their saved bill!
                                                selectedTab = 1
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_purchase_invoice_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ فاتورة الشراء وعرض السند", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else {
                // Invoices History Tab
                var searchFilter by remember { mutableStateOf("") }
                val filteredInvoices = purchaseInvoices.filter {
                    it.customerName.contains(searchFilter, ignoreCase = true) ||
                    it.invoiceNumber.contains(searchFilter, ignoreCase = true)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        placeholder = { Text("بحث في سجل المشتريات بالعميل أو رقم الفاتورة...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    if (filteredInvoices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد فواتير مشتريات مسجلة بعد", color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("يمكنك وزن وإضافة أول فاتورة من تبويب الشراء", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredInvoices) { inv ->
                                InvoiceHistoryCard(
                                    invoice = inv,
                                    currency = shopProfile?.currencySymbol ?: "ر.ي",
                                    onView = {
                                        coroutineScope.launch {
                                            val pair = viewModel.getInvoiceWithItems(inv.id)
                                            viewingInvoice = pair.first
                                            viewingItems = pair.second
                                            showPrintDialog = true
                                        }
                                    },
                                    onDelete = { viewModel.deleteInvoice(inv) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPrintDialog && viewingInvoice != null) {
        PrintInvoiceDialog(
            invoice = viewingInvoice!!,
            items = viewingItems,
            shopProfile = shopProfile,
            onDismiss = {
                showPrintDialog = false
                viewingInvoice = null
            }
        )
    }
}

@Composable
fun InvoiceHistoryCard(
    invoice: InvoiceEntity,
    currency: String,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = invoice.dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "العميل: ${invoice.customerName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإجمالي: ${formatCurrency(invoice.totalAmount, currency)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (invoice.remainingAmount > 0) {
                        Text(
                            text = if (invoice.invoiceType == "PURCHASE") "متبقي له: ${formatCurrency(invoice.remainingAmount, currency)}" else "متبقي عليه: ${formatCurrency(invoice.remainingAmount, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = RedAccent
                        )
                    } else {
                        Text(
                            text = "مسدد بالكامل",
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenSuccess
                        )
                    }
                }

                Row {
                    IconButton(onClick = onView) {
                        Icon(Icons.Default.Visibility, contentDescription = "عرض السند", tint = BluePrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent)
                    }
                }
            }
        }
    }
}
