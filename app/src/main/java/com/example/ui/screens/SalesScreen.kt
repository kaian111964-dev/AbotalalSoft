package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.NewInvoiceItem
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val activeSalePrice by viewModel.activeSalePrice.collectAsState()
    val saleInvoices by viewModel.saleInvoices.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    val currency = shopProfile?.currencySymbol ?: "ريال"

    // Form State
    var customerName by remember { mutableStateOf("عميل نقدي") }
    var customerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isFullCash by remember { mutableStateOf(true) }
    var paidAmountText by remember { mutableStateOf("") }

    // Dropdown state for customer selection
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    // Filter sales customers (factories, merchants, buyers)
    val salesCustomers = remember(allCustomers) {
        allCustomers.filter { it.customerType != "SUPPLIER" }
    }

    // 8 Predefined Scrap Items matching user specifications
    val scrapList = listOf(
        "الألمنيوم", "البطاريات", "الحديد", "البلاستيك",
        "المعدن الثقيل", "المعدن الخفيف", "النحاس الأصفر", "النحاس الأحمر"
    )
    var selectedScrapName by remember { mutableStateOf(scrapList[7]) } // Default to النحاس الأحمر

    var unitPriceText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val currentInvoiceItems = remember { mutableStateListOf<NewInvoiceItem>() }

    // History and Dialogs
    var showHistoryDialog by remember { mutableStateOf(false) }
    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    // Helper to get active selling price
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

    // Set initial unit price when scrap selected or active price changes
    LaunchedEffect(selectedScrapName, activeSalePrice) {
        val p = getActiveSalePriceFor(selectedScrapName)
        if (p > 0.0) {
            unitPriceText = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
        }
    }

    val totalInvoiceAmount = currentInvoiceItems.sumOf { it.subtotal }
    val enteredPaidAmount = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val remainingOnBuyer = (totalInvoiceAmount - enteredPaidAmount).coerceAtLeast(0.0)

    val totalSalesAllTime = saleInvoices.sumOf { it.totalAmount }

    // Date formatting matching screenshot
    val dateDisplay = remember {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sdfDay = SimpleDateFormat("EEEE", Locale("ar")).format(Date())
        Pair(sdfDate, sdfDay)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            // Top Bar strictly matching Screenshot 1
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0256D0),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back button on start
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("sales_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    // Title & subtitle with shopping cart
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "المبيعات",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "Abu Talal Soft",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Date capsule pill on end
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = dateDisplay.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = dateDisplay.second,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Action Bar & Sub Bar matching Screenshot 1
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action Buttons Row: المجموع الكلي | تفريغ الكل | طباعة | حفظ الفاتورة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Total Box
                        Surface(
                            modifier = Modifier.weight(1.3f).height(46.dp),
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "المجموع الكلي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1E3A8A),
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = "${formatCurrency(totalInvoiceAmount, "")} $currency",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0256D0),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = Color(0xFF0256D0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Clear Button (Red)
                        Button(
                            onClick = {
                                currentInvoiceItems.clear()
                                weightText = ""
                                notes = ""
                                paidAmountText = ""
                                isFullCash = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(46.dp).weight(1.1f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تفريغ الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Print Button (Blue)
                        Button(
                            onClick = {
                                if (currentInvoiceItems.isEmpty()) {
                                    viewModel.showMessage("أضف أصناف أولاً لمعاينة الطباعة")
                                } else {
                                    // Preview current draft invoice
                                    val dummyInv = InvoiceEntity(
                                        id = 0,
                                        invoiceNumber = "DRAFT-${System.currentTimeMillis() % 10000}",
                                        customerName = customerName.ifBlank { "عميل نقدي" },
                                        customerPhone = customerPhone,
                                        invoiceType = "SALE",
                                        totalAmount = totalInvoiceAmount,
                                        paidAmount = enteredPaidAmount,
                                        remainingAmount = remainingOnBuyer,
                                        isDebt = remainingOnBuyer > 0,
                                        notes = notes,
                                        dateString = dateDisplay.first,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    val draftEntities = currentInvoiceItems.mapIndexed { idx, itm ->
                                        InvoiceItemEntity(
                                            id = idx.toLong(),
                                            invoiceId = 0,
                                            scrapName = itm.scrapName,
                                            weightKg = itm.weightKg,
                                            unitPrice = itm.unitPrice,
                                            subtotal = itm.subtotal
                                        )
                                    }
                                    viewingInvoice = dummyInv
                                    viewingItems = draftEntities
                                    showPrintDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(46.dp).weight(0.9f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("طباعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Save Button (Green)
                        Button(
                            onClick = {
                                if (currentInvoiceItems.isEmpty()) {
                                    viewModel.showMessage("يرجى إضافة صنف خردة مبيوع واحد على الأقل")
                                    return@Button
                                }
                                val finalPaid = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
                                viewModel.createInvoice(
                                    type = "SALE",
                                    customerName = customerName.ifBlank { "عميل نقدي" },
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
                                            customerName = "عميل نقدي"
                                            customerPhone = ""
                                            paidAmountText = ""
                                            notes = ""
                                            isFullCash = true
                                        }
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(46.dp).weight(1.3f).testTag("save_sale_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ الفاتورة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Bottom Sub-Bar: الإعدادات | سجل المبيعات
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Settings Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(AppScreen.SETTINGS) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الإعدادات", fontSize = 12.sp, color = Color(0xFF0256D0), fontWeight = FontWeight.Bold)
                            }

                            VerticalDivider(modifier = Modifier.height(18.dp), color = Color(0xFFCBD5E1))

                            // Sales History Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showHistoryDialog = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سجل المبيعات (${saleInvoices.size})", fontSize = 12.sp, color = Color(0xFF0256D0), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------------- 1. TOP SUMMARY STATS ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // إجمالي المبيعات (Blue)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFDBEAFE),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(24.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("إجمالي المبيعات", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A), fontSize = 10.sp)
                            Text(
                                text = "${formatCurrency(totalSalesAllTime, "")} $currency",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // عدد الفواتير (Red)
                Surface(
                    modifier = Modifier.weight(0.9f),
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("عدد الفواتير", style = MaterialTheme.typography.labelSmall, color = Color(0xFF991B1B), fontSize = 10.sp)
                            Text(
                                text = "${saleInvoices.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // العملاء (Green)
                Surface(
                    modifier = Modifier.weight(0.8f),
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("العملاء", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534), fontSize = 10.sp)
                            Text(
                                text = "${allCustomers.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            // ---------------- 2. CUSTOMER & SALE TYPE ROW ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sale Type toggle (نقدي vs آجل)
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "نوع البيع",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // آجل
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clickable { isFullCash = false },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isFullCash) Color(0xFF0256D0) else Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (!isFullCash) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "آجل",
                                        color = if (!isFullCash) Color.White else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // نقدي
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clickable { isFullCash = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFullCash) Color(0xFF0256D0) else Color(0xFFF1F5F9)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (isFullCash) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "نقدي",
                                        color = if (isFullCash) Color.White else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Customer Selector Dropdown
                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "اختر العميل",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        ExposedDropdownMenuBox(
                            expanded = isCustomerDropdownExpanded,
                            onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                placeholder = { Text("اختر أو اكتب اسم المشتري / المصنع", fontSize = 11.sp) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0256D0),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = isCustomerDropdownExpanded,
                                onDismissRequest = { isCustomerDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("عميل مبيعات نقدي (افتراضي)", fontWeight = FontWeight.Bold, color = Color(0xFF0256D0)) },
                                    onClick = {
                                        customerName = "عميل نقدي"
                                        customerPhone = ""
                                        isCustomerDropdownExpanded = false
                                    }
                                )
                                if (salesCustomers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("لا يوجد مصانع/تجار مسجلين (اكتب الاسم مباشرة)", fontSize = 11.sp, color = Color.Gray) },
                                        onClick = { isCustomerDropdownExpanded = false }
                                    )
                                } else {
                                    salesCustomers.forEach { cust ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    if (cust.phone.isNotBlank()) {
                                                        Text(cust.phone, fontSize = 10.sp, color = Color(0xFF64748B))
                                                    }
                                                }
                                            },
                                            onClick = {
                                                customerName = cust.name
                                                customerPhone = cust.phone
                                                isCustomerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- 2.B DEBT & REMAINING INPUTS (عند اختيار بيع آجل) ----------------
            if (!isFullCash) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تفاصيل الدفع الآجل (مستحقات للمحل لدى المشتري)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // المبلغ المقبوض نقدياً الآن
                            OutlinedTextField(
                                value = paidAmountText,
                                onValueChange = { paidAmountText = it },
                                label = { Text("المقبوض نقدياً الآن", fontSize = 10.sp) },
                                placeholder = { Text("0.0", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .testTag("sale_paid_amount_input"),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            )

                            // المتبقي عليه للمحل (Card)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("المتبقي عليه للمحل", fontSize = 10.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${formatCurrency(remainingOnBuyer, "")} $currency",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- 3. ADD SCRAP SECTION (إضافة أصناف المبيعات) ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إضافة أصناف المبيعات",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0256D0)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF0256D0),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Scrap Cards & Numeric Inputs Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Numeric inputs column
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Weight input
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الوزن (كيلو)", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Scale, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                                }
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("sale_weight_field"),
                                    shape = RoundedCornerShape(8.dp),
                                    placeholder = { Text("0.00", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                            }

                            // Price input
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سعر الكيلو (ريال)", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                                }
                                OutlinedTextField(
                                    value = unitPriceText,
                                    onValueChange = { unitPriceText = it },
                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("sale_price_field"),
                                    shape = RoundedCornerShape(8.dp),
                                    placeholder = { Text("0.00", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                )
                            }

                            // Subtotal display
                            val curSubtotal = (weightText.toDoubleOrNull() ?: 0.0) * (unitPriceText.toDoubleOrNull() ?: 0.0)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الإجمالي (ريال)", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = formatCurrency(curSubtotal, ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0256D0)
                                        )
                                    }
                                }
                            }
                        }

                        // 8 Scrap Items Grid (3 columns) matching Screenshot 1
                        Column(
                            modifier = Modifier.weight(1.9f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Row 1: الألمنيوم, البطاريات, الحديد
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ScrapPickCard(scrapName = "الألمنيوم", isSelected = selectedScrapName == "الألمنيوم", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "الألمنيوم"
                                }
                                ScrapPickCard(scrapName = "البطاريات", isSelected = selectedScrapName == "البطاريات", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "البطاريات"
                                }
                                ScrapPickCard(scrapName = "الحديد", isSelected = selectedScrapName == "الحديد", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "الحديد"
                                }
                            }

                            // Row 2: البلاستيك, المعدن الثقيل, المعدن الخفيف
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ScrapPickCard(scrapName = "البلاستيك", isSelected = selectedScrapName == "البلاستيك", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "البلاستيك"
                                }
                                ScrapPickCard(scrapName = "المعدن الثقيل", isSelected = selectedScrapName == "المعدن الثقيل", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "المعدن الثقيل"
                                }
                                ScrapPickCard(scrapName = "المعدن الخفيف", isSelected = selectedScrapName == "المعدن الخفيف", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "المعدن الخفيف"
                                }
                            }

                            // Row 3: النحاس الأصفر, النحاس الأحمر
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ScrapPickCard(scrapName = "النحاس الأصفر", isSelected = selectedScrapName == "النحاس الأصفر", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "النحاس الأصفر"
                                }
                                ScrapPickCard(scrapName = "النحاس الأحمر", isSelected = selectedScrapName == "النحاس الأحمر", modifier = Modifier.weight(1f)) {
                                    selectedScrapName = "النحاس الأحمر"
                                }
                            }
                        }
                    }

                    // Notes text field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        placeholder = { Text("ملاحظات [اختياري] ...", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp)) }
                    )

                    // Add Item Button
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 0.0
                            val p = unitPriceText.toDoubleOrNull() ?: 0.0
                            if (w <= 0.0 || p <= 0.0) {
                                viewModel.showMessage("يرجى إدخال وزن وسعر بيع صحيحين")
                            } else {
                                currentInvoiceItems.add(
                                    NewInvoiceItem(
                                        scrapName = selectedScrapName,
                                        unitPrice = p,
                                        weightKg = w
                                    )
                                )
                                weightText = ""
                                viewModel.showMessage("تمت إضافة $selectedScrapName")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة الصنف إلى الفاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ---------------- 4. INVOICE ITEMS TABLE (عناصر الفاتورة) ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عناصر الفاتورة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0256D0)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Table Header Pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFDBEAFE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("خيارات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                            Text("الإجمالي", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("سعر الكيلو", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("الوزن (كجم)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                            Text("الصنف", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            Text("م", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
                        }
                    }

                    if (currentInvoiceItems.isEmpty()) {
                        // Empty State matching Screenshot 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "لا توجد أصناف مضافة بعد",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Added Items rows
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            currentInvoiceItems.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Delete action
                                    IconButton(
                                        onClick = { currentInvoiceItems.removeAt(idx) },
                                        modifier = Modifier.weight(0.7f).size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }

                                    // Total
                                    Text(
                                        text = formatCurrency(item.subtotal, ""),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0256D0),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    // Unit price
                                    Text(
                                        text = formatCurrency(item.unitPrice, ""),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    // Weight
                                    Text(
                                        text = formatWeight(item.weightKg),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.Center
                                    )

                                    // Scrap name + icon
                                    Row(
                                        modifier = Modifier.weight(1.5f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(text = item.scrapName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Image(
                                            painter = painterResource(id = getScrapDrawableRes(item.scrapName)),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Serial number
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.weight(0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Sales History Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showHistoryDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "سجل فواتير المبيعات (${saleInvoices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0256D0)
                    )
                }
            },
            text = {
                var searchFilter by remember { mutableStateOf("") }
                val filteredSales = saleInvoices.filter {
                    it.customerName.contains(searchFilter, ignoreCase = true) ||
                    it.invoiceNumber.contains(searchFilter, ignoreCase = true)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        placeholder = { Text("بحث بالاسم أو رقم الفاتورة...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredSales.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد فواتير مبيعات سابقة", color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredSales) { inv ->
                                InvoiceHistoryCard(
                                    invoice = inv,
                                    currency = currency,
                                    onView = {
                                        coroutineScope.launch {
                                            val pair = viewModel.getInvoiceWithItems(inv.id)
                                            viewingInvoice = pair.first
                                            viewingItems = pair.second
                                            showPrintDialog = true
                                            showHistoryDialog = false
                                        }
                                    },
                                    onDelete = { viewModel.deleteInvoice(inv) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Print Invoice Dialog
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
fun ScrapPickCard(
    scrapName: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF0256D0) else Color(0xFFE2E8F0)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Blue plus button in top start / corner
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF0256D0) else Color(0xFF3B82F6),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Scrap content
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = getScrapDrawableRes(scrapName)),
                    contentDescription = scrapName,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = scrapName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1
                )
            }
        }
    }
}
