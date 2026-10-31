package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ريال يمني"

    val allInvoices by viewModel.allInvoices.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()

    var showCashAndCredit by remember { mutableStateOf(true) }
    var selectedPeriodFilter by remember { mutableStateOf("آخر 7 أيام") }
    var isPeriodDropdownExpanded by remember { mutableStateOf(false) }

    // Date calculations
    val (startDateStr, endDateStr) = remember {
        val cal = Calendar.getInstance()
        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        Pair(start, end)
    }

    // Real financial figures from Room
    val purchaseInvoices = remember(allInvoices) { allInvoices.filter { it.invoiceType == "PURCHASE" } }
    val saleInvoices = remember(allInvoices) { allInvoices.filter { it.invoiceType == "SALE" } }

    val totalSales = saleInvoices.sumOf { it.totalAmount }
    val totalSalesCashReceived = saleInvoices.sumOf { it.paidAmount }
    val totalSalesRemainingCredit = saleInvoices.sumOf { it.remainingAmount }

    val totalPurchases = purchaseInvoices.sumOf { it.totalAmount }
    val totalPurchasesCashPaid = purchaseInvoices.sumOf { it.paidAmount }
    val totalPurchasesRemainingCredit = purchaseInvoices.sumOf { it.remainingAmount }

    val totalExpensesAmount = allExpenses.sumOf { it.amount }

    // Cashbox calculations
    val currentCashBalance = (totalSalesCashReceived - totalPurchasesCashPaid - totalExpensesAmount).coerceAtLeast(0.0)
    val totalCreditBalance = totalSalesRemainingCredit

    val netProfit = totalSales - totalPurchases - totalExpensesAmount

    // 7-day profit chart points calculation
    val weekDaysLabels = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    val chartPoints = remember(allInvoices, allExpenses) {
        val points = mutableListOf<Float>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val checkCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            val dayStr = sdf.format(checkCal.time)
            val daySales = saleInvoices.filter { it.dateString.startsWith(dayStr) }.sumOf { it.totalAmount }
            val dayPurchases = purchaseInvoices.filter { it.dateString.startsWith(dayStr) }.sumOf { it.totalAmount }
            val dayExpenses = allExpenses.filter { it.dateString.startsWith(dayStr) }.sumOf { it.amount }
            val dayProfit = (daySales - dayPurchases - dayExpenses).toFloat()
            points.add(dayProfit)
        }
        points
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            // Top Bar matching Screenshot 3
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
                        modifier = Modifier.size(38.dp).testTag("reports_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "الصندوق والأرباح",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "متابعة مالية شاملة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Bottom Actions Bar: الإعدادات | طباعة التقرير matching Screenshot 3
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEFF6FF),
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Settings Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.SETTINGS) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإعدادات", fontSize = 13.sp, color = Color(0xFF0256D0), fontWeight = FontWeight.Bold)
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp), color = Color(0xFFCBD5E1))

                    // Print Report Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val reportSummary = buildString {
                                    appendLine("تقرير الصندوق والأرباح - ابوطلال سوفت")
                                    appendLine("الفترة: من $startDateStr إلى $endDateStr")
                                    appendLine("-------------------------------")
                                    appendLine("إجمالي المبيعات: ${formatCurrency(totalSales, currency)}")
                                    appendLine("إجمالي المشتريات: ${formatCurrency(totalPurchases, currency)}")
                                    appendLine("إجمالي المصروفات: ${formatCurrency(totalExpensesAmount, currency)}")
                                    appendLine("صافي الربح / الخسارة: ${formatCurrency(netProfit, currency)}")
                                    appendLine("رصيد الصندوق النقدي: ${formatCurrency(currentCashBalance, currency)}")
                                    appendLine("الرصيد الآجل: ${formatCurrency(totalCreditBalance, currency)}")
                                }
                                shareText(context, "تقرير الصندوق والأرباح", reportSummary)
                            }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة التقرير", fontSize = 13.sp, color = Color(0xFF0256D0), fontWeight = FontWeight.Bold)
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
            // ---------------- 1. 3 TOP STAT CARDS ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // الأرباح الصافية (Blue/Green)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الأرباح الصافية", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatCurrency(netProfit, ""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netProfit >= 0) Color(0xFF0256D0) else Color(0xFFEF4444),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // إجمالي المصروفات (Red)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("إجمالي المصروفات", style = MaterialTheme.typography.labelSmall, color = Color(0xFF991B1B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatCurrency(totalExpensesAmount, ""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // إجمالي الإيرادات (Blue)
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("إجمالي الإيرادات", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatCurrency(totalSalesCashReceived, ""),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0256D0),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // ---------------- 2. HERO CARD: رصيد الصندوق الحالي ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Blue Header Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0256D0),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Toggle button: نقدي وآجل
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showCashAndCredit = !showCashAndCredit }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نقدي وآجل", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Title & Safe icon
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "رصيد الصندوق الحالي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Card Body with 3D Safe illustration, Cash balance, and Credit breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: 3D Safe illustration
                        Image(
                            painter = painterResource(id = R.drawable.ic_safe_box),
                            contentDescription = "الصندوق",
                            modifier = Modifier.size(62.dp)
                        )

                        // Center: Main Balance
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatCurrency(currentCashBalance, ""),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0256D0)
                            )
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Right: الرصيد النقدي والآجل
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // الرصيد النقدي
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = formatCurrency(currentCashBalance, ""), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0256D0))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "الرصيد النقدي", fontSize = 9.sp, color = Color(0xFF1E3A8A))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(12.dp))
                                }
                            }

                            // الرصيد الآجل
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = formatCurrency(totalCreditBalance, ""), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "الرصيد الآجل", fontSize = 9.sp, color = Color(0xFF991B1B))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- 3. DATE RANGE FILTER BAR ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Update Button
                    Button(
                        onClick = { viewModel.showMessage("تم تحديث البيانات المالية بنجاح") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Period display pill
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "من $startDateStr إلى $endDateStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الفترة الزمنية",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ---------------- 4. ACCOUNT DETAILS CARD (تفاصيل الحساب) ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0256D0),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Text(
                            text = "تفاصيل الحساب",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // إجمالي المبيعات
                        AccountDetailRow(
                            title = "إجمالي المبيعات",
                            amount = formatCurrency(totalSales, ""),
                            icon = Icons.Default.ShoppingCart,
                            iconTint = Color(0xFF0256D0),
                            amountColor = Color(0xFF0256D0)
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // إجمالي المشتريات
                        AccountDetailRow(
                            title = "إجمالي المشتريات",
                            amount = formatCurrency(totalPurchases, ""),
                            icon = Icons.Default.ShoppingBag,
                            iconTint = Color(0xFFEF4444),
                            amountColor = Color(0xFFEF4444)
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // إجمالي المصروفات
                        AccountDetailRow(
                            title = "إجمالي المصروفات",
                            amount = formatCurrency(totalExpensesAmount, ""),
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Color(0xFFEF4444),
                            amountColor = Color(0xFFEF4444)
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // صافي الربح / الخسارة
                        AccountDetailRow(
                            title = "صافي الربح / الخسارة",
                            amount = formatCurrency(netProfit, ""),
                            icon = Icons.Default.Calculate,
                            iconTint = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444),
                            amountColor = if (netProfit >= 0) Color(0xFF16A34A) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            // ---------------- 5. PROFIT CHART CARD (مخطط الأرباح) ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header with Filter Dropdown
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0256D0),
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Filter dropdown
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { isPeriodDropdownExpanded = !isPeriodDropdownExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(selectedPeriodFilter, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "مخطط الأرباح",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Chart Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw horizontal grid lines (0, 0.5, 1)
                            val gridLines = listOf(0.15f, 0.5f, 0.85f)
                            gridLines.forEach { frac ->
                                val y = h * frac
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Plot 7 days points & blue trend line
                            val n = 7
                            val step = w / (n - 1)
                            val path = Path()

                            val maxVal = (chartPoints.maxOrNull() ?: 100f).coerceAtLeast(10f)
                            val minVal = (chartPoints.minOrNull() ?: 0f).coerceAtMost(0f)
                            val range = (maxVal - minVal).coerceAtLeast(1f)

                            val offsets = mutableListOf<Offset>()

                            for (i in 0 until n) {
                                val x = i * step
                                val ptVal = chartPoints.getOrElse(i) { 0f }
                                val norm = (ptVal - minVal) / range
                                val y = h * (0.85f - (norm * 0.7f))
                                offsets.add(Offset(x, y))

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            // Draw blue line
                            drawPath(
                                path = path,
                                color = Color(0xFF0256D0),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw blue dots on points
                            offsets.forEach { pt ->
                                drawCircle(
                                    color = Color(0xFF0256D0),
                                    radius = 4.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekdays labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekDaysLabels.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun AccountDetailRow(
    title: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    amountColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = amountColor,
            fontSize = 14.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
