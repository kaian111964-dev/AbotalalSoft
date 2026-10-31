package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val currency = shopProfile?.currencySymbol ?: "ر.ي"

    val allInvoices by viewModel.allInvoices.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var dateFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Today, 2: This Month

    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val thisMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val filteredInvoices = remember(allInvoices, dateFilter) {
        allInvoices.filter { inv ->
            when (dateFilter) {
                1 -> inv.dateString.startsWith(todayDateStr)
                2 -> inv.dateString.startsWith(thisMonthStr)
                else -> true
            }
        }
    }

    val filteredExpenses = remember(allExpenses, dateFilter) {
        allExpenses.filter { exp ->
            when (dateFilter) {
                1 -> exp.dateString.startsWith(todayDateStr)
                2 -> exp.dateString.startsWith(thisMonthStr)
                else -> true
            }
        }
    }

    val purchaseInvoices = filteredInvoices.filter { it.invoiceType == "PURCHASE" }
    val saleInvoices = filteredInvoices.filter { it.invoiceType == "SALE" }

    val totalPurchases = purchaseInvoices.sumOf { it.totalAmount }
    val totalPurchasesPaid = purchaseInvoices.sumOf { it.paidAmount }
    val totalPurchasesDebtsOwedToSuppliers = purchaseInvoices.sumOf { it.remainingAmount }

    val totalSales = saleInvoices.sumOf { it.totalAmount }
    val totalSalesReceived = saleInvoices.sumOf { it.paidAmount }
    val totalSalesDebtsOwedByBuyers = saleInvoices.sumOf { it.remainingAmount }

    val totalExpensesAmount = filteredExpenses.sumOf { it.amount }

    val netProfit = totalSales - totalPurchases - totalExpensesAmount

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "التقارير المحاسبية والمبيعات والمشتريات",
                onBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = {
                            val reportSummary = buildString {
                                appendLine("📊 تقرير ابوطلال سوفت لإدارة الخردة")
                                appendLine("الفترة: ${if (dateFilter == 1) "اليوم" else if (dateFilter == 2) "هذا الشهر" else "الكل"}")
                                appendLine("--------------------------------")
                                appendLine("إجمالي المشتريات: ${formatCurrency(totalPurchases, currency)}")
                                appendLine("المسدد للموردين نقداً: ${formatCurrency(totalPurchasesPaid, currency)}")
                                appendLine("المتبقي لهم (ديون للموردين): ${formatCurrency(totalPurchasesDebtsOwedToSuppliers, currency)}")
                                appendLine("--------------------------------")
                                appendLine("إجمالي المبيعات: ${formatCurrency(totalSales, currency)}")
                                appendLine("المستلم من المشترين: ${formatCurrency(totalSalesReceived, currency)}")
                                appendLine("المتبقي عليهم (ديون على المشترين): ${formatCurrency(totalSalesDebtsOwedByBuyers, currency)}")
                                appendLine("--------------------------------")
                                appendLine("إجمالي المصروفات التشغيلية: ${formatCurrency(totalExpensesAmount, currency)}")
                                appendLine("صافي الأرباح التقديرية: ${formatCurrency(netProfit, currency)}")
                            }
                            shareText(context, "التقرير المحاسبي", reportSummary)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة التقرير", tint = Color.White)
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dateFilter == 0,
                    onClick = { dateFilter = 0 },
                    label = { Text("كافة الفترات") }
                )
                FilterChip(
                    selected = dateFilter == 1,
                    onClick = { dateFilter = 1 },
                    label = { Text("اليوم فقط") }
                )
                FilterChip(
                    selected = dateFilter == 2,
                    onClick = { dateFilter = 2 },
                    label = { Text("هذا الشهر") }
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = BluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("تقارير المشتريات", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_purchases")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("تقارير المبيعات", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_sales")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("الأرباح والصندوق", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_report_financial")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // Purchases Report
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ملخص مشتريات الخردة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)

                            ReportMetricRow("عدد فواتير الشراء:", "${purchaseInvoices.size} فاتورة", BluePrimary)
                            ReportMetricRow("إجمالي قيمة المشتريات:", formatCurrency(totalPurchases, currency), BluePrimary)
                            ReportMetricRow("المبلغ المسلَّم نقداً للموردين:", formatCurrency(totalPurchasesPaid, currency), GreenSuccess)
                            ReportMetricRow("المتبقي لهم في ذمة المحل (آجل):", formatCurrency(totalPurchasesDebtsOwedToSuppliers, currency), RedAccent)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("أصناف الخردة المشتراة بالمحل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                            Text("يتم تصنيف وحساب الأوزان وقيم الأصناف الثمانية بشكل فوري بناءً على فواتير الشراء المسجلة بدون أي بيانات تجريبية.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                } else if (selectedTab == 1) {
                    // Sales Report
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ملخص مبيعات الخردة للتجار والمصانع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenSuccess)

                            ReportMetricRow("عدد فواتير البيع:", "${saleInvoices.size} فاتورة", GreenSuccess)
                            ReportMetricRow("إجمالي قيمة المبيعات:", formatCurrency(totalSales, currency), GreenSuccess)
                            ReportMetricRow("المبلغ المستلم نقداً:", formatCurrency(totalSalesReceived, currency), GreenSuccess)
                            ReportMetricRow("المتبقي عليهم للمحل (آجل):", formatCurrency(totalSalesDebtsOwedByBuyers, currency), RedAccent)
                        }
                    }
                } else {
                    // Financial Balance & Profits
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("حركة الصندوق وصافي النتائج", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)

                            ReportMetricRow("(+) إجمالي المبيعات:", formatCurrency(totalSales, currency), GreenSuccess)
                            ReportMetricRow("(-) إجمالي المشتريات:", formatCurrency(totalPurchases, currency), BluePrimary)
                            ReportMetricRow("(-) إجمالي المصروفات التشغيلية:", formatCurrency(totalExpensesAmount, currency), Color(0xFFD97706))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("صافي الأرباح التقديرية:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = formatCurrency(netProfit, currency),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netProfit >= 0) GreenSuccess else RedAccent
                                )
                            }
                        }
                    }

                    // Debts Comparison
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("موقف الديون الإجمالي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)

                            ReportMetricRow("ديون الموردين (متبقي لهم في ذمتنا):", formatCurrency(totalPurchasesDebtsOwedToSuppliers, currency), RedAccent)
                            ReportMetricRow("ديون المشترين (متبقي عليهم للمحل):", formatCurrency(totalSalesDebtsOwedByBuyers, currency), GreenSuccess)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
