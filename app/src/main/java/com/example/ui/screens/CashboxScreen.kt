package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.data.local.entity.SettlementEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashboxScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ريال"

    val allInvoices by viewModel.allInvoices.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allSettlements by viewModel.allSettlements.collectAsState()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    // Financial calculations
    val saleInvoices = remember(allInvoices) { allInvoices.filter { it.invoiceType == "SALE" } }
    val purchaseInvoices = remember(allInvoices) { allInvoices.filter { it.invoiceType == "PURCHASE" } }

    val salesCashIn = remember(saleInvoices) { saleInvoices.sumOf { it.paidAmount } }
    val purchasesCashOut = remember(purchaseInvoices) { purchaseInvoices.sumOf { it.paidAmount } }
    val expensesCashOut = remember(allExpenses) { allExpenses.sumOf { it.amount } }

    // Direct Cashbox transactions from settlements
    val cashDeposits = remember(allSettlements) {
        allSettlements.filter { it.type == "CASH_DEPOSIT" || it.type == "RECEIPT_FROM_BUYER" }.sumOf { it.amount }
    }
    val cashWithdrawals = remember(allSettlements) {
        allSettlements.filter { it.type == "CASH_WITHDRAWAL" || it.type == "PAYMENT_TO_SUPPLIER" }.sumOf { it.amount }
    }

    // Actual liquid cash available in the cashbox
    val totalCashboxBalance = (salesCashIn + cashDeposits - purchasesCashOut - expensesCashOut - cashWithdrawals).coerceAtLeast(0.0)
    val totalCreditReceivable = saleInvoices.sumOf { it.remainingAmount }
    val totalCreditPayable = purchaseInvoices.sumOf { it.remainingAmount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            // Fixed Top Bar strictly matching system aesthetic
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
                        modifier = Modifier.size(38.dp).testTag("cashbox_back_btn")
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
                                text = "الصندوق والسيولة النقدية",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "متابعة النقدية وسندات القبض والصرف",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.showMessage("تم تحديث رصيد الصندوق بنجاح") },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------- 1. HERO CASHBOX CARD ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0256D0),
                        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "حالة الصندوق الآن",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "الرصيد النقدي الفعلي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Savings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Body
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Safe image
                        Image(
                            painter = painterResource(id = R.drawable.ic_safe_box),
                            contentDescription = "الخزنة",
                            modifier = Modifier.size(64.dp)
                        )

                        // Center amount
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatCurrency(totalCashboxBalance, ""),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0256D0)
                            )
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Right Stats
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "متاح فوراً: ${formatCurrency(totalCashboxBalance, "")}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0256D0),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    text = "ديون الموردين: ${formatCurrency(totalCreditPayable, "")}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Two Primary Deposit & Withdraw Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Deposit Button (Green)
                        Button(
                            onClick = { showDepositDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("cashbox_deposit_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إيداع في الصندوق (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Withdraw Button (Red)
                        Button(
                            onClick = { showWithdrawDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("cashbox_withdraw_btn")
                        ) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("سحب من الصندوق (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ---------------- 2. CASHFLOW BREAKDOWN (2x2 GRID) ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // إيداعات إضافية (+)
                CashflowStatCard(
                    title = "إيداعات الصندوق (+)",
                    amount = formatCurrency(cashDeposits, currency),
                    color = Color(0xFF16A34A),
                    bg = Color(0xFFDCFCE7),
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )

                // مقبوضات المبيعات (+)
                CashflowStatCard(
                    title = "مقبوضات المبيعات (+)",
                    amount = formatCurrency(salesCashIn, currency),
                    color = Color(0xFF0256D0),
                    bg = Color(0xFFDBEAFE),
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // مسحوبات نقدية (-)
                CashflowStatCard(
                    title = "مسحوبات الصندوق (-)",
                    amount = formatCurrency(cashWithdrawals, currency),
                    color = Color(0xFFDC2626),
                    bg = Color(0xFFFEE2E2),
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )

                // مدفوعات المشتريات (-)
                CashflowStatCard(
                    title = "مدفوعات المشتريات (-)",
                    amount = formatCurrency(purchasesCashOut, currency),
                    color = Color(0xFFD97706),
                    bg = Color(0xFFFEF3C7),
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---------------- 3. RECENT CASH TRANSACTIONS ----------------
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
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
                                text = "العمليات: ${allSettlements.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "حركات وسندات الصندوق",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0256D0)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (allSettlements.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد سندات إيداع أو سحب مباشرة بعد.\nيمكنك إضافة أول عملية عبر زري الإيداع والسحب بالأعلى.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allSettlements.take(20).forEach { item ->
                                val isDeposit = item.type == "CASH_DEPOSIT" || item.type == "RECEIPT_FROM_BUYER"
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDeposit) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isDeposit) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Delete action
                                        IconButton(
                                            onClick = { viewModel.deleteSettlement(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }

                                        // Amount tag
                                        Text(
                                            text = "${if (isDeposit) "+" else "-"} ${formatCurrency(item.amount, "")} $currency",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = if (isDeposit) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )

                                        // Info
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = item.customerName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "${item.dateString} | ${item.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Deposit Dialog
    if (showDepositDialog) {
        var depositAmount by remember { mutableStateOf("") }
        var depositorName by remember { mutableStateOf("") }
        var depositReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إيداع مبلغ في الصندوق", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("المبلغ المودع ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = depositorName,
                        onValueChange = { depositorName = it },
                        label = { Text("اسم المودع (اختياري)") },
                        placeholder = { Text("مثال: صاحب المحل أو عميل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = depositReason,
                        onValueChange = { depositReason = it },
                        label = { Text("سبب الإيداع / البيان") },
                        placeholder = { Text("مثال: تغذية الصندوق أو رأس مال") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depositAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            viewModel.showMessage("يرجى إدخال مبلغ صحيح")
                        } else {
                            viewModel.addCashTransaction(
                                type = "DEPOSIT",
                                amount = amt,
                                reason = depositReason.ifBlank { "تغذية نقدية للصندوق" },
                                personName = depositorName.ifBlank { "إيداع نقدي مباشر" }
                            )
                            showDepositDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("تأكيد الإيداع")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Withdrawal Dialog
    if (showWithdrawDialog) {
        var withdrawAmount by remember { mutableStateOf("") }
        var recipientName by remember { mutableStateOf("") }
        var withdrawReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سحب مبلغ من الصندوق", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("المبلغ المسحوب ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("اسم المستلم (اختياري)") },
                        placeholder = { Text("مثال: صاحب المحل أو مسحوبات شخصية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = withdrawReason,
                        onValueChange = { withdrawReason = it },
                        label = { Text("سبب السحب / البيان") },
                        placeholder = { Text("مثال: سحب أرباح أو مصروف خاص") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            viewModel.showMessage("يرجى إدخال مبلغ صحيح")
                        } else {
                            viewModel.addCashTransaction(
                                type = "WITHDRAWAL",
                                amount = amt,
                                reason = withdrawReason.ifBlank { "سحب نقدي من الصندوق" },
                                personName = recipientName.ifBlank { "سحب نقدي مباشر" }
                            )
                            showWithdrawDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("تأكيد السحب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CashflowStatCard(
    title: String,
    amount: String,
    color: Color,
    bg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = title, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
            }
        }
    }
}
