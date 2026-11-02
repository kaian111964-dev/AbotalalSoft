package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.PrintInvoiceDialog
import com.example.ui.components.formatCurrency
import com.example.ui.components.getScrapDrawableRes
import com.example.ui.components.shareText
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val activePurchasePrice by viewModel.activePurchasePrice.collectAsState()

    val currency = shopProfile?.currencySymbol ?: "ريال"

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: آجل (ديون), 2: نقدي مسدد, 3: تقرير الأصناف

    // Dialog states
    var invoiceToEdit by remember { mutableStateOf<InvoiceEntity?>(null) }
    var itemsForEdit by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var invoiceToPrint by remember { mutableStateOf<InvoiceEntity?>(null) }
    var itemsForPrint by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceEntity?>(null) }

    // Aggregate statistics
    val totalPurchasesAmount = remember(purchaseInvoices) { purchaseInvoices.sumOf { it.totalAmount } }
    val totalPaidAmount = remember(purchaseInvoices) { purchaseInvoices.sumOf { it.paidAmount } }
    val totalRemainingDebts = remember(purchaseInvoices) { purchaseInvoices.sumOf { it.remainingAmount } }
    val invoicesCount = purchaseInvoices.size

    // Filtered invoices
    val filteredInvoices = remember(purchaseInvoices, searchQuery, selectedFilterTab) {
        purchaseInvoices.filter { inv ->
            val matchQuery = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.customerPhone.contains(searchQuery) ||
                    inv.notes.contains(searchQuery, ignoreCase = true)

            val matchTab = when (selectedFilterTab) {
                1 -> inv.remainingAmount > 0 // آجل / ديون
                2 -> inv.remainingAmount <= 0 // نقدي مسدد
                else -> true
            }
            matchQuery && matchTab
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(
                color = Color(0xFF0256D0),
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "سجل وتقارير المشتريات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "تعديل، عرض، وطباعة فواتير الخردة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }

                        // New Purchase Button
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.PURCHASES) },
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "شراء جديد",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث برقم الفاتورة، اسم المورد، الهاتف...", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Stat Cards Bar
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryMetricCard(
                            title = "إجمالي المشتريات",
                            value = "${formatCurrency(totalPurchasesAmount, "")} $currency",
                            subtitle = "$invoicesCount فاتورة",
                            color = Color(0xFF0256D0),
                            icon = Icons.Default.ShoppingCart,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            title = "المسدد نقداً",
                            value = "${formatCurrency(totalPaidAmount, "")} $currency",
                            subtitle = "صندوق المشتريات",
                            color = Color(0xFF16A34A),
                            icon = Icons.Default.Savings,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            title = "ديون الموردين (آجل)",
                            value = "${formatCurrency(totalRemainingDebts, "")} $currency",
                            subtitle = "متبقي في ذمتنا",
                            color = Color(0xFFEF4444),
                            icon = Icons.Default.ReceiptLong,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedFilterTab,
                        edgePadding = 0.dp,
                        containerColor = Color(0xFFF1F5F9),
                        indicator = {},
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        val tabs = listOf(
                            "الكل ($invoicesCount)",
                            "آجل / ديون (${purchaseInvoices.count { it.remainingAmount > 0 }})",
                            "مسدد نقداً (${purchaseInvoices.count { it.remainingAmount <= 0 }})",
                            "تقرير أصناف الخردة"
                        )
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedFilterTab == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedFilterTab = index },
                                modifier = Modifier
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF0256D0) else Color.Transparent)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area
            if (selectedFilterTab == 3) {
                // Scrap Items Aggregate Report Tab
                ScrapItemsReportTab(
                    purchaseInvoices = purchaseInvoices,
                    currency = currency,
                    viewModel = viewModel
                )
            } else {
                // Invoice List
                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "لا توجد فواتير تطابق بحثك" else "لا توجد فواتير مشتريات مسجلة بعد",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك إضافة فاتورة شراء جديدة بسهولة من شاشة المشتريات",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.PURCHASES) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنشاء فاتورة مشتريات الآن")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            PurchaseInvoiceCard(
                                invoice = invoice,
                                currency = currency,
                                viewModel = viewModel,
                                onEditClick = {
                                    coroutineScope.launch {
                                        val items = viewModel.getInvoiceWithItems(invoice.id).second
                                        invoiceToEdit = invoice
                                        itemsForEdit = items
                                    }
                                },
                                onPrintClick = {
                                    coroutineScope.launch {
                                        val items = viewModel.getInvoiceWithItems(invoice.id).second
                                        invoiceToPrint = invoice
                                        itemsForPrint = items
                                    }
                                },
                                onDeleteClick = {
                                    invoiceToDelete = invoice
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Invoice Dialog
    if (invoiceToEdit != null) {
        EditPurchaseInvoiceDialog(
            invoice = invoiceToEdit!!,
            initialItems = itemsForEdit,
            currency = currency,
            allCategories = allCategories.map { it.name },
            activePurchasePrice = activePurchasePrice,
            onDismiss = {
                invoiceToEdit = null
                itemsForEdit = emptyList()
            },
            onSave = { updatedInvoice, updatedItems ->
                viewModel.updateInvoice(updatedInvoice, updatedItems) {
                    invoiceToEdit = null
                    itemsForEdit = emptyList()
                }
            }
        )
    }

    // Print Invoice Dialog
    if (invoiceToPrint != null) {
        PrintInvoiceDialog(
            invoice = invoiceToPrint!!,
            items = itemsForPrint,
            shopProfile = shopProfile,
            onDismiss = {
                invoiceToPrint = null
                itemsForPrint = emptyList()
            }
        )
    }

    // Delete Confirmation Dialog
    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp)) },
            title = { Text("حذف فاتورة المشتريات", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Text(
                    "هل أنت متأكد من حذف فاتورة المشتريات رقم ${invoiceToDelete!!.invoiceNumber} للمورد ${invoiceToDelete!!.customerName}؟\nسيتم حذف كافة بنود الفاتورة وإلغاء الأثر المالي.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inv = invoiceToDelete!!
                        invoiceToDelete = null
                        viewModel.deleteInvoice(inv)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("نعم، احذف الفاتورة")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// ----------------- SUMMARY METRIC CARD -----------------
@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1)
        }
    }
}

// ----------------- PURCHASE INVOICE CARD -----------------
@Composable
fun PurchaseInvoiceCard(
    invoice: InvoiceEntity,
    currency: String,
    viewModel: MainViewModel,
    onEditClick: () -> Unit,
    onPrintClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var itemsList by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }

    LaunchedEffect(isExpanded, invoice.id) {
        if (isExpanded && itemsList.isEmpty()) {
            itemsList = viewModel.getInvoiceWithItems(invoice.id).second
        }
    }

    val isDebt = invoice.remainingAmount > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDebt) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Header Row: Invoice Number, Date, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0256D0).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = invoice.invoiceNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0256D0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = invoice.dateString,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Debt / Cash Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDebt) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (isDebt) "آجل (متبقي للمورد)" else "نقدي مسدد",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDebt) Color(0xFFDC2626) else Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supplier Info & Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = invoice.customerName.ifBlank { "مورد نقدي" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                    if (invoice.customerPhone.isNotBlank()) {
                        Text(
                            text = "هاتف: ${invoice.customerPhone}",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الإجمالي: ${formatCurrency(invoice.totalAmount, "")} $currency",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color(0xFF0256D0)
                    )
                    Row {
                        Text(
                            text = "المدفوع: ${formatCurrency(invoice.paidAmount, "")}",
                            fontSize = 10.sp,
                            color = Color(0xFF16A34A)
                        )
                        if (isDebt) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المتبقي: ${formatCurrency(invoice.remainingAmount, "")}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            if (invoice.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${invoice.notes}",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2
                )
            }

            // Expanded Items Table Preview
            if (isExpanded && itemsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "أصناف الخردة في الفاتورة:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الصنف", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                    Text("الوزن (كجم)", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("سعر الكيلو", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("الإجمالي", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                itemsList.forEach { itm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(itm.scrapName, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f))
                        Text("${itm.weightKg} كجم", fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${formatCurrency(itm.unitPrice, "")}", fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${formatCurrency(itm.subtotal, "")} $currency", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0256D0), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row: تعديل | طباعة | مشاركة | تفاصيل | حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Edit Button
                    Button(
                        onClick = onEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تعديل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Print / PDF Button
                    OutlinedButton(
                        onClick = onPrintClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0256D0)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0256D0)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("طباعة PDF", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = {
                            val shareContent = buildString {
                                appendLine("فاتورة مشتريات خردة رقم: ${invoice.invoiceNumber}")
                                appendLine("المورد: ${invoice.customerName}")
                                appendLine("التاريخ: ${invoice.dateString}")
                                appendLine("إجمالي الفاتورة: ${invoice.totalAmount} $currency")
                                appendLine("المبلغ المدفوع: ${invoice.paidAmount} $currency")
                                if (invoice.remainingAmount > 0) {
                                    appendLine("المتبقي في ذمتنا: ${invoice.remainingAmount} $currency")
                                }
                            }
                            shareText(context, "فاتورة مشتريات ${invoice.invoiceNumber}", shareContent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF16A34A)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand/Collapse Details
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "إخفاء الأصناف" else "عرض الأصناف",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ----------------- EDIT PURCHASE INVOICE DIALOG -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseInvoiceDialog(
    invoice: InvoiceEntity,
    initialItems: List<InvoiceItemEntity>,
    currency: String,
    allCategories: List<String>,
    activePurchasePrice: com.example.data.local.entity.DailyPriceEntity?,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
) {
    var supplierName by remember { mutableStateOf(invoice.customerName) }
    var supplierPhone by remember { mutableStateOf(invoice.customerPhone) }
    var notes by remember { mutableStateOf(invoice.notes) }
    var paidAmountText by remember { mutableStateOf(invoice.paidAmount.toString()) }

    val itemsState = remember {
        mutableStateListOf<InvoiceItemEntity>().apply {
            addAll(initialItems)
        }
    }

    // New item inputs
    val defaultScrapList = listOf(
        "النحاس الأحمر", "النحاس الأصفر", "المعدن الخفيف", "المعدن الثقيل",
        "الألمنيوم", "البطاريات", "الحديد", "البلاستيك"
    )
    val availableScrapList = remember(allCategories) {
        (defaultScrapList + allCategories).distinct()
    }

    var selectedScrap by remember { mutableStateOf(availableScrapList.firstOrNull() ?: "النحاس الأحمر") }
    var scrapDropdownExpanded by remember { mutableStateOf(false) }
    var weightText by remember { mutableStateOf("") }
    var unitPriceText by remember { mutableStateOf("") }

    fun getPriceFor(name: String): Double {
        return when (name) {
            "النحاس الأحمر" -> activePurchasePrice?.copperRedPrice ?: 0.0
            "النحاس الأصفر" -> activePurchasePrice?.copperYellowPrice ?: 0.0
            "المعدن الخفيف" -> activePurchasePrice?.metalLightPrice ?: 0.0
            "المعدن الثقيل" -> activePurchasePrice?.metalHeavyPrice ?: 0.0
            "الألمنيوم" -> activePurchasePrice?.aluminumPrice ?: 0.0
            "البطاريات" -> activePurchasePrice?.batteryPrice ?: 0.0
            "الحديد" -> activePurchasePrice?.ironPrice ?: 0.0
            "البلاستيك" -> activePurchasePrice?.plasticPrice ?: 0.0
            else -> 0.0
        }
    }

    LaunchedEffect(selectedScrap) {
        val p = getPriceFor(selectedScrap)
        if (p > 0.0) unitPriceText = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
    }

    val totalAmount = itemsState.sumOf { it.subtotal }
    val paidAmount = paidAmountText.toDoubleOrNull() ?: 0.0
    val remaining = (totalAmount - paidAmount).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "تعديل فاتورة مشتريات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0256D0)
                        )
                        Text(
                            text = "رقم الفاتورة: ${invoice.invoiceNumber}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0256D0))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Supplier inputs
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("اسم المورد / العميل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = supplierPhone,
                    onValueChange = { supplierPhone = it },
                    label = { Text("هاتف المورد (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Items Section
                Text(
                    text = "بنود وأصناف الفاتورة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                itemsState.forEachIndexed { idx, itm ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { itemsState.removeAt(idx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الصنف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = itm.scrapName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${itm.weightKg} كجم × ${formatCurrency(itm.unitPrice, "")} = ${formatCurrency(itm.subtotal, "")} $currency",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0256D0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add item to invoice section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "+ إضافة صنف جديد للفاتورة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF0256D0)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Scrap dropdown
                        Box {
                            OutlinedButton(
                                onClick = { scrapDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    Text(selectedScrap, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DropdownMenu(
                                expanded = scrapDropdownExpanded,
                                onDismissRequest = { scrapDropdownExpanded = false }
                            ) {
                                availableScrapList.forEach { scrap ->
                                    DropdownMenuItem(
                                        text = { Text(scrap) },
                                        onClick = {
                                            selectedScrap = scrap
                                            scrapDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("الوزن (كجم)", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(50.dp)
                            )
                            OutlinedTextField(
                                value = unitPriceText,
                                onValueChange = { unitPriceText = it },
                                label = { Text("سعر الكيلو", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                val w = weightText.toDoubleOrNull() ?: 0.0
                                val p = unitPriceText.toDoubleOrNull() ?: 0.0
                                if (w <= 0 || p <= 0) return@Button
                                itemsState.add(
                                    InvoiceItemEntity(
                                        invoiceId = invoice.id,
                                        scrapName = selectedScrap,
                                        weightKg = w,
                                        unitPrice = p,
                                        subtotal = w * p
                                    )
                                )
                                weightText = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة الصنف للقائمة", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment and Total Calculation
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي الفاتورة المحدث:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${formatCurrency(totalAmount, "")} $currency", fontWeight = FontWeight.ExtraBold, color = Color(0xFF0256D0), fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("المبلغ المدفوع للمورد نقداً") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي في ذمتنا للمورد (آجل):", fontSize = 11.sp, color = if (remaining > 0) Color(0xFFEF4444) else Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            Text("${formatCurrency(remaining, "")} $currency", fontWeight = FontWeight.ExtraBold, color = if (remaining > 0) Color(0xFFEF4444) else Color(0xFF16A34A), fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الفاتورة") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        if (itemsState.isEmpty()) return@Button
                        val updatedInvoice = invoice.copy(
                            customerName = supplierName.trim().ifBlank { "مورد نقدي" },
                            customerPhone = supplierPhone.trim(),
                            totalAmount = totalAmount,
                            paidAmount = paidAmount,
                            remainingAmount = remaining,
                            isDebt = remaining > 0,
                            notes = notes.trim()
                        )
                        onSave(updatedInvoice, itemsState.toList())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التعديلات في السجل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ----------------- SCRAP ITEMS AGGREGATE REPORT TAB -----------------
@Composable
fun ScrapItemsReportTab(
    purchaseInvoices: List<InvoiceEntity>,
    currency: String,
    viewModel: MainViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var scrapReportItems by remember { mutableStateOf<List<ScrapItemSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(purchaseInvoices) {
        isLoading = true
        val itemMap = mutableMapOf<String, Pair<Double, Double>>() // scrapName -> (totalWeight, totalCost)
        purchaseInvoices.forEach { inv ->
            val items = viewModel.getInvoiceWithItems(inv.id).second
            items.forEach { itm ->
                val current = itemMap[itm.scrapName] ?: Pair(0.0, 0.0)
                itemMap[itm.scrapName] = Pair(current.first + itm.weightKg, current.second + itm.subtotal)
            }
        }
        scrapReportItems = itemMap.map { (name, pair) ->
            ScrapItemSummary(
                scrapName = name,
                totalWeightKg = pair.first,
                totalCost = pair.second,
                averagePrice = if (pair.first > 0) pair.second / pair.first else 0.0
            )
        }.sortedByDescending { it.totalCost }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0256D0))
        }
    } else if (scrapReportItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد بيانات أصناف مشتراة حتى الآن", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("تقرير مشتريات أصناف الخردة المجمعة", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF0256D0))
                            Text("إجمالي الأوزان والمبالغ ومتوسط سعر الشراء لكل نوع خردة", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            items(scrapReportItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = getScrapDrawableRes(item.scrapName)),
                                contentDescription = item.scrapName,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(item.scrapName, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                Text("إجمالي الوزن: ${item.totalWeightKg} كجم", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatCurrency(item.totalCost, "")} $currency",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0256D0)
                            )
                            Text(
                                text = "متوسط: ${formatCurrency(item.averagePrice, "")} /كجم",
                                fontSize = 10.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ScrapItemSummary(
    val scrapName: String,
    val totalWeightKg: Double,
    val totalCost: Double,
    val averagePrice: Double
)
