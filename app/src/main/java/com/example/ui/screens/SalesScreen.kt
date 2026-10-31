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
fun SalesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val activeSalePrice by viewModel.activeSalePrice.collectAsState()
    val saleInvoices by viewModel.saleInvoices.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // New Sale Form State
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var paidAmountText by remember { mutableStateOf("") }
    var isFullCash by remember { mutableStateOf(true) }

    val scrapList = listOf(
        "النحاس الأحمر", "النحاس الأصفر", "المعدن الخفيف", "المعدن الثقيل",
        "الألمنيوم", "البطاريات", "الحديد", "البلاستيك"
    )
    var selectedScrapName by remember { mutableStateOf(scrapList[0]) }
    var isScrapDropdownExpanded by remember { mutableStateOf(false) }

    var unitPriceText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val currentInvoiceItems = remember { mutableStateListOf<NewInvoiceItem>() }

    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    fun getActiveSalePriceFor(scrap: String): Double {
        return when (scrap) {
            "النحاس الأحمر" -> activeSalePrice?.copperRedPrice ?: 0.0
            "النحاس الأصفر" -> activeSalePrice?.copperYellowPrice ?: 0.0
            "المعدن الخفيف" -> activeSalePrice?.metalLightPrice ?: 0.0
            "المعدن الثقيل" -> activeSalePrice?.metalHeavyPrice ?: 0.0
            "الألمنيوم" -> activeSalePrice?.aluminumPrice ?: 0.0
            "البطاريات" -> activeSalePrice?.batteryPrice ?: 0.0
            "الحديد" -> activeSalePrice?.ironPrice ?: 0.0
            "البلاستيك" -> activeSalePrice?.plasticPrice ?: 0.0
            else -> 0.0
        }
    }

    LaunchedEffect(selectedScrapName, activeSalePrice) {
        val p = getActiveSalePriceFor(selectedScrapName)
        if (p > 0.0) {
            unitPriceText = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
        }
    }

    val totalInvoiceAmount = currentInvoiceItems.sumOf { it.subtotal }
    val enteredPaidAmount = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val remainingOnBuyer = (totalInvoiceAmount - enteredPaidAmount).coerceAtLeast(0.0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "مبيعات الخردة للتجار والمصانع",
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = GreenSuccess
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("فاتورة بيع خردة جديدة", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Sell, contentDescription = null) },
                    modifier = Modifier.testTag("tab_new_sale")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سجل المبيعات (${saleInvoices.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    modifier = Modifier.testTag("tab_sale_history")
                )
            }

            if (selectedTab == 0) {
                val formScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(formScroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Buyer info
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
                                text = "بيانات المشتري (المصنع / التاجر)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess
                            )

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("اسم المشتري / المصنع *") },
                                placeholder = { Text("اكتب اسم المشتري أو المصنع") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = GreenSuccess) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sale_customer_name"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("رقم الهاتف (اختياري)") },
                                placeholder = { Text("770000000") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GreenSuccess) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sale_customer_phone"),
                                singleLine = true
                            )
                        }
                    }

                    // Add Scrap Item Card with Dropdown Menu
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
                                    text = "إضافة صنف خردة مبيوع",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                )
                                val p = getActiveSalePriceFor(selectedScrapName)
                                if (p > 0) {
                                    Text(
                                        text = "سعر البيع المعتمد: ${formatCurrency(p, shopProfile?.currencySymbol ?: "ر.ي")}/كجم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BluePrimary,
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
                                    label = { Text("اختر صنف الخردة المبيوع (قائمة منسدلة)") },
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
                                        .testTag("sale_scrap_dropdown")
                                )

                                ExposedDropdownMenu(
                                    expanded = isScrapDropdownExpanded,
                                    onDismissRequest = { isScrapDropdownExpanded = false }
                                ) {
                                    scrapList.forEach { scrap ->
                                        val activeItemPrice = getActiveSalePriceFor(scrap)
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
                                                            color = GreenSuccess,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedScrapName = scrap
                                                val autoPrice = getActiveSalePriceFor(scrap)
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
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    label = { Text("الوزن المبيوع (كجم) *") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null, tint = GreenSuccess) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sale_weight_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = unitPriceText,
                                    onValueChange = { unitPriceText = it },
                                    label = { Text("سعر البيع للكيلو *") },
                                    placeholder = { Text("0.0") },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GreenSuccess) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sale_unit_price_input"),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    val w = weightText.toDoubleOrNull() ?: 0.0
                                    val p = unitPriceText.toDoubleOrNull() ?: 0.0
                                    if (w <= 0.0 || p <= 0.0) {
                                        viewModel.showMessage("يرجى إدخال وزن وسعر بيع صحيح")
                                    } else {
                                        currentInvoiceItems.add(
                                            NewInvoiceItem(
                                                scrapName = selectedScrapName,
                                                unitPrice = p,
                                                weightKg = w
                                            )
                                        )
                                        weightText = ""
                                        viewModel.showMessage("تمت إضافة $selectedScrapName للفاتورة بنجاح")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_sale_item_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة الصنف إلى فاتورة البيع", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Items Table
                    if (currentInvoiceItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "أصناف البيع (${currentInvoiceItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
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
                                            Text(text = "${index + 1}. ${item.scrapName}", fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${formatWeight(item.weightKg)} × ${formatCurrency(item.unitPrice, shopProfile?.currencySymbol ?: "ر.ي")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(text = formatCurrency(item.subtotal, shopProfile?.currencySymbol ?: "ر.ي"), fontWeight = FontWeight.Bold, color = GreenSuccess)
                                        IconButton(onClick = { currentInvoiceItems.removeAt(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Payment summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي البيع:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatCurrency(totalInvoiceAmount, shopProfile?.currencySymbol ?: "ر.ي"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isFullCash,
                                    onClick = { isFullCash = true },
                                    label = { Text("نقدي (استلمنا كامل المبلغ)") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !isFullCash,
                                    onClick = { isFullCash = false },
                                    label = { Text("آجل (متبقي عليه لنا)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!isFullCash) {
                                OutlinedTextField(
                                    value = paidAmountText,
                                    onValueChange = { paidAmountText = it },
                                    label = { Text("المبلغ المستلم نقدياً الآن") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sale_paid_amount_input"),
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
                                        Text("المتبقي عليه للمحل:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = RedAccent)
                                        Text(
                                            text = formatCurrency(remainingOnBuyer, shopProfile?.currencySymbol ?: "ر.ي"),
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
                                label = { Text("ملاحظات") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (currentInvoiceItems.isEmpty()) {
                                        viewModel.showMessage("يرجى إضافة صنف خردة مبيوع واحد على الأقل")
                                        return@Button
                                    }
                                    val finalPaid = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
                                    viewModel.createInvoice(
                                        type = "SALE",
                                        customerName = customerName.ifBlank { "مشتري خردة نقدي" },
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
                                                currentInvoiceItems.clear()
                                                customerName = ""
                                                customerPhone = ""
                                                paidAmountText = ""
                                                notes = ""
                                                isFullCash = true
                                                selectedTab = 1
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("save_sale_invoice_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ فاتورة البيع وعرض السند", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else {
                // Sale History Tab
                var searchFilter by remember { mutableStateOf("") }
                val filteredSales = saleInvoices.filter {
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
                        placeholder = { Text("بحث في سجل المبيعات بالاسم أو رقم الفاتورة...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    if (filteredSales.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد فواتير مبيعات مسجلة بعد", color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("يمكنك إنشاء أول فاتورة بيع من تبويب البيع", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredSales) { inv ->
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
