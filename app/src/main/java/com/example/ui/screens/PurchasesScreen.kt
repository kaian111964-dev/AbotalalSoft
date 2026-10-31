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
fun PurchasesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val activePurchasePrice by viewModel.activePurchasePrice.collectAsState()
    val purchaseInvoices by viewModel.purchaseInvoices.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()

    val currency = shopProfile?.currencySymbol ?: "ريال يمني"

    // Form state
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isFullCash by remember { mutableStateOf(true) }
    var paidAmountText by remember { mutableStateOf("") }

    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    // 8 Predefined Scrap Items matching Screenshot 2
    val scrapList = listOf(
        "المعدن الثقيل", "المعدن الخفيف", "النحاس الأصفر", "النحاس الأحمر",
        "البلاستيك", "الحديد", "البطاريات", "الألمنيوم"
    )
    var selectedScrapName by remember { mutableStateOf("النحاس الأحمر") }
    var isScrapDropdownExpanded by remember { mutableStateOf(false) }

    var unitPriceText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val currentInvoiceItems = remember { mutableStateListOf<NewInvoiceItem>() }

    // History and Dialogs
    var showHistoryDialog by remember { mutableStateOf(false) }
    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    // Helper to get active purchase price
    fun getActivePurchasePriceFor(scrap: String): Double {
        return when (scrap) {
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

    LaunchedEffect(selectedScrapName, activePurchasePrice) {
        val p = getActivePurchasePriceFor(selectedScrapName)
        if (p > 0.0) {
            unitPriceText = if (p % 1.0 == 0.0) p.toLong().toString() else p.toString()
        }
    }

    val totalInvoiceAmount = currentInvoiceItems.sumOf { it.subtotal }
    val enteredPaidAmount = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
    val remainingToClient = (totalInvoiceAmount - enteredPaidAmount).coerceAtLeast(0.0)

    val dateDisplay = remember {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sdfDay = SimpleDateFormat("EEEE", Locale("ar")).format(Date())
        Pair(sdfDate, sdfDay)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            // Top Bar strictly matching Screenshot 2
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
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.size(38.dp).testTag("purchases_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "المشتريات",
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
                            text = "شاشة مشتريات جديدة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

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
            // Bottom Navigation Bar matching Screenshot 2
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الرئيسية
                    BottomBarNavItem(icon = Icons.Default.Home, label = "الرئيسية", isSelected = false) {
                        viewModel.navigateTo(AppScreen.HOME)
                    }
                    // المبيعات
                    BottomBarNavItem(icon = Icons.Default.TrendingUp, label = "المبيعات", isSelected = false) {
                        viewModel.navigateTo(AppScreen.SALES)
                    }
                    // المشتريات (Active)
                    BottomBarNavItem(icon = Icons.Default.ShoppingCart, label = "المشتريات", isSelected = true) {
                        // Current screen
                    }
                    // التقارير
                    BottomBarNavItem(icon = Icons.Default.BarChart, label = "التقارير", isSelected = false) {
                        viewModel.navigateTo(AppScreen.REPORTS)
                    }
                    // الإعدادات
                    BottomBarNavItem(icon = Icons.Default.Settings, label = "الإعدادات", isSelected = false) {
                        viewModel.navigateTo(AppScreen.SETTINGS)
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
            // ---------------- 1. CUSTOMER & PAYMENT TYPE ROW ----------------
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
                    // Payment type toggle (نقدي vs آجل)
                    Row(
                        modifier = Modifier.weight(1.1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // نقدي
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "نقدي",
                                    color = if (isFullCash) Color.White else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // آجل
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "آجل",
                                    color = if (!isFullCash) Color.White else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Customer Selector
                    Column(modifier = Modifier.weight(1.5f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "اسم العميل (اختياري)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        ExposedDropdownMenuBox(
                            expanded = isCustomerDropdownExpanded,
                            onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customerName.ifBlank { "اختر العميل" },
                                onValueChange = { customerName = it },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded)
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = isCustomerDropdownExpanded,
                                onDismissRequest = { isCustomerDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("عميل مشتريات نقدي", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        customerName = "عميل مشتريات نقدي"
                                        customerPhone = ""
                                        isCustomerDropdownExpanded = false
                                    }
                                )
                                allCustomers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text(cust.name) },
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

            // ---------------- 2. SCRAP CATEGORY CARDS (أصناف الخردة) ----------------
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "أصناف الخردة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0256D0),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    // Row 1: النحاس الأحمر, النحاس الأصفر, المعدن الخفيف, المعدن الثقيل
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PurchaseScrapBadge(
                            name = "النحاس الأحمر",
                            bg = Color(0xFFFFECEE),
                            border = Color(0xFFFCA5A5),
                            isSelected = selectedScrapName == "النحاس الأحمر",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "النحاس الأحمر" }

                        PurchaseScrapBadge(
                            name = "النحاس الأصفر",
                            bg = Color(0xFFFEF3C7),
                            border = Color(0xFFFDE68A),
                            isSelected = selectedScrapName == "النحاس الأصفر",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "النحاس الأصفر" }

                        PurchaseScrapBadge(
                            name = "المعدن الخفيف",
                            bg = Color(0xFFE0F2FE),
                            border = Color(0xFFBAE6FD),
                            isSelected = selectedScrapName == "المعدن الخفيف",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "المعدن الخفيف" }

                        PurchaseScrapBadge(
                            name = "المعدن الثقيل",
                            bg = Color(0xFFF1F5F9),
                            border = Color(0xFFCBD5E1),
                            isSelected = selectedScrapName == "المعدن الثقيل",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "المعدن الثقيل" }
                    }

                    // Row 2: الألمنيوم, البطاريات, الحديد, البلاستيك
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PurchaseScrapBadge(
                            name = "الألمنيوم",
                            bg = Color(0xFFE0F7FA),
                            border = Color(0xFFB2EBF2),
                            isSelected = selectedScrapName == "الألمنيوم",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "الألمنيوم" }

                        PurchaseScrapBadge(
                            name = "البطاريات",
                            bg = Color(0xFFF3E8FF),
                            border = Color(0xFFE9D5FF),
                            isSelected = selectedScrapName == "البطاريات",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "البطاريات" }

                        PurchaseScrapBadge(
                            name = "الحديد",
                            bg = Color(0xFFE0F2FE),
                            border = Color(0xFFBFDBFE),
                            isSelected = selectedScrapName == "الحديد",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "الحديد" }

                        PurchaseScrapBadge(
                            name = "البلاستيك",
                            bg = Color(0xFFDCFCE7),
                            border = Color(0xFFBBF7D0),
                            isSelected = selectedScrapName == "البلاستيك",
                            modifier = Modifier.weight(1f)
                        ) { selectedScrapName = "البلاستيك" }
                    }
                }
            }

            // ---------------- 3. ACTIVE ITEM INPUT ROW (إضافة صنف جديد) ----------------
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with Barcode button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.showMessage("ماسح الباركود نشط وجاهز") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("باركود", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "إضافة صنف جديد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Input Fields Row: الإجمالي | سعر الكيلو | الوزن | الصنف | حذف
                    val curCalcTotal = (weightText.toDoubleOrNull() ?: 0.0) * (unitPriceText.toDoubleOrNull() ?: 0.0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Delete / Clear button (Red)
                        IconButton(
                            onClick = {
                                weightText = ""
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEF4444),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // الصنف dropdown
                        ExposedDropdownMenuBox(
                            expanded = isScrapDropdownExpanded,
                            onExpandedChange = { isScrapDropdownExpanded = !isScrapDropdownExpanded },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = selectedScrapName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(6.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isScrapDropdownExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = isScrapDropdownExpanded,
                                onDismissRequest = { isScrapDropdownExpanded = false }
                            ) {
                                scrapList.forEach { scrap ->
                                    DropdownMenuItem(
                                        text = { Text(scrap, fontSize = 12.sp) },
                                        onClick = {
                                            selectedScrapName = scrap
                                            isScrapDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // الوزن (كيلو)
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("purchase_weight_input"),
                            shape = RoundedCornerShape(6.dp),
                            placeholder = { Text("0.0", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )

                        // سعر الكيلو (ريال)
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("purchase_price_input"),
                            shape = RoundedCornerShape(6.dp),
                            placeholder = { Text("0.0", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )

                        // الإجمالي (ريال)
                        Surface(
                            modifier = Modifier.weight(1f).height(44.dp),
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = formatCurrency(curCalcTotal, ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0256D0),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Button: + إضافة صنف آخر
                    Button(
                        onClick = {
                            val w = weightText.toDoubleOrNull() ?: 0.0
                            val p = unitPriceText.toDoubleOrNull() ?: 0.0
                            if (w <= 0.0 || p <= 0.0) {
                                viewModel.showMessage("يرجى إدخال وزن وسعر شراء صحيحين")
                            } else {
                                currentInvoiceItems.add(
                                    NewInvoiceItem(
                                        scrapName = selectedScrapName,
                                        unitPrice = p,
                                        weightKg = w
                                    )
                                )
                                weightText = ""
                                viewModel.showMessage("تمت إضافة $selectedScrapName بنجاح")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة صنف آخر", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // ---------------- 4. ADDED ITEMS LIST (قائمة الأصناف المضافة) ----------------
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "عدد الأصناف : ${currentInvoiceItems.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "قائمة الأصناف المضافة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Table Header Pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إجراءات", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                            Text("الإجمالي (ريال)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("سعر الكيلو (ريال)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("الوزن (كجم)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("الصنف", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            Text("م", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
                        }
                    }

                    if (currentInvoiceItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد أصناف مضافة في الفاتورة بعد",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            currentInvoiceItems.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Delete
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
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )

                                    // Scrap pill
                                    Surface(
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        color = getScrapBgColor(item.scrapName)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(text = item.scrapName, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Image(
                                                painter = painterResource(id = getScrapDrawableRes(item.scrapName)),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Serial
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

            // ---------------- 5. NOTES & TOTAL BOX ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Box
                Surface(
                    modifier = Modifier.weight(1.1f).height(50.dp),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("الإجمالي الكلي", fontSize = 9.sp, color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                            Text(
                                text = "${formatCurrency(totalInvoiceAmount, "")} $currency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0256D0),
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(20.dp))
                    }
                }

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("أدخل ملاحظاتك هنا ...", fontSize = 10.sp) },
                    modifier = Modifier.weight(1.9f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp)) }
                )
            }

            // ---------------- 6. ACTION BUTTONS ROW ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // حفظ المشتريات (Blue Button)
                Button(
                    onClick = {
                        if (currentInvoiceItems.isEmpty()) {
                            viewModel.showMessage("يرجى إضافة صنف مشتريات واحد على الأقل")
                            return@Button
                        }
                        val finalPaid = if (isFullCash) totalInvoiceAmount else (paidAmountText.toDoubleOrNull() ?: 0.0)
                        viewModel.createInvoice(
                            type = "PURCHASE",
                            customerName = customerName.ifBlank { "عميل مشتريات نقدي" },
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
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(1.3f).height(42.dp).testTag("save_purchase_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ المشتريات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // حذف الكل (Red Outline)
                OutlinedButton(
                    onClick = {
                        currentInvoiceItems.clear()
                        weightText = ""
                        notes = ""
                        paidAmountText = ""
                        isFullCash = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(0.9f).height(42.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("حذف الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // مشاركة (Blue Outline)
                OutlinedButton(
                    onClick = {
                        if (currentInvoiceItems.isEmpty()) {
                            viewModel.showMessage("لا توجد أصناف للمشاركة")
                        } else {
                            val summary = buildString {
                                appendLine("فاتورة مشتريات خردة - ابوطلال سوفت")
                                appendLine("العميل: ${customerName.ifBlank { "عميل نقدي" }}")
                                appendLine("التاريخ: ${dateDisplay.first}")
                                currentInvoiceItems.forEachIndexed { i, itm ->
                                    appendLine("${i+1}. ${itm.scrapName} - ${itm.weightKg} كجم × ${itm.unitPrice} = ${itm.subtotal} ريال")
                                }
                                appendLine("المجموع: $totalInvoiceAmount ريال")
                            }
                            shareText(context, "فاتورة مشتريات", summary)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0256D0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0256D0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(0.8f).height(42.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // طباعة (Blue Outline)
                OutlinedButton(
                    onClick = {
                        if (currentInvoiceItems.isEmpty()) {
                            viewModel.showMessage("أضف أصناف أولاً لمعاينة الطباعة")
                        } else {
                            val dummyInv = InvoiceEntity(
                                id = 0,
                                invoiceNumber = "PUR-${System.currentTimeMillis() % 10000}",
                                customerName = customerName.ifBlank { "عميل نقدي" },
                                customerPhone = customerPhone,
                                invoiceType = "PURCHASE",
                                totalAmount = totalInvoiceAmount,
                                paidAmount = enteredPaidAmount,
                                remainingAmount = remainingToClient,
                                isDebt = remainingToClient > 0,
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0256D0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0256D0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(0.8f).height(42.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("طباعة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
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
fun PurchaseScrapBadge(
    name: String,
    bg: Color,
    border: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFDBEAFE) else bg,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFF0256D0) else border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = getScrapDrawableRes(name)),
                contentDescription = name,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1
            )
        }
    }
}

fun getScrapBgColor(scrapName: String): Color {
    return when {
        scrapName.contains("أحمر") -> Color(0xFFFFECEE)
        scrapName.contains("أصفر") -> Color(0xFFFEF3C7)
        scrapName.contains("خفيف") -> Color(0xFFE0F2FE)
        scrapName.contains("ثقيل") -> Color(0xFFF1F5F9)
        scrapName.contains("ألمنيوم") || scrapName.contains("المنيوم") -> Color(0xFFE0F7FA)
        scrapName.contains("بطاريات") || scrapName.contains("بطارية") -> Color(0xFFF3E8FF)
        scrapName.contains("حديد") -> Color(0xFFE0F2FE)
        scrapName.contains("بلاستيك") -> Color(0xFFDCFCE7)
        else -> Color(0xFFEFF6FF)
    }
}

@Composable
fun BottomBarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF0256D0) else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF0256D0) else Color(0xFF64748B)
        )
    }
}
