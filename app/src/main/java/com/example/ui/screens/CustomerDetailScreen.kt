package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.SettlementEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ر.ي"

    val customerSummary by viewModel.selectedCustomerSummary.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val allSettlements by viewModel.allSettlements.collectAsState()

    val customer = customerSummary?.customer

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
        return
    }

    val custInvoices = allInvoices.filter { it.customerId == customer.id || it.customerName.trim() == customer.name.trim() }
    val custSettlements = allSettlements.filter { it.customerId == customer.id }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Invoices, 1: Settlements
    var showSettlementDialog by remember { mutableStateOf(false) }
    var settlementType by remember { mutableStateOf("PAYMENT_TO_SUPPLIER") }

    var viewingInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }
    var viewingItems by remember { mutableStateOf<List<InvoiceItemEntity>>(emptyList()) }
    var showPrintDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "كشف حساب: ${customer.name}",
                onBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = { viewModel.deleteCustomer(customer) },
                        modifier = Modifier.testTag("delete_customer_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = BlueLight,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                if (customer.phone.isNotBlank()) {
                                    Text(
                                        text = customer.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        if (customer.phone.isNotBlank()) {
                            Row {
                                IconButton(onClick = { openPhoneCall(context, customer.phone) }) {
                                    Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = BluePrimary)
                                }
                                IconButton(onClick = { openWhatsApp(context, customer.phone) }) {
                                    Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = GreenSuccess)
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Ledger Balances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = RedLight
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المتبقي له في ذمتنا", style = MaterialTheme.typography.labelSmall, color = RedAccent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCurrency(customerSummary?.owedToCustomer ?: 0.0, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RedAccent
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = GreenLight
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المتبقي عليه للمحل", style = MaterialTheme.typography.labelSmall, color = GreenSuccess, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatCurrency(customerSummary?.owedByCustomer ?: 0.0, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenSuccess
                                )
                            }
                        }
                    }

                    // Action Buttons for Settlements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                settlementType = "PAYMENT_TO_SUPPLIER"
                                showSettlementDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settlement_pay_supplier_btn")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسديد دفعة (صرف له)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                settlementType = "RECEIPT_FROM_BUYER"
                                showSettlementDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settlement_receipt_buyer_btn")
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("قبض دفعة (منه)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tabs for Invoices and Settlements
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceWhite,
                contentColor = BluePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الفواتير (${custInvoices.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سندات التسديد (${custSettlements.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                if (custInvoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد فواتير مسجلة لهذا العميل بعد", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(custInvoices) { inv ->
                            InvoiceHistoryCard(
                                invoice = inv,
                                currency = currency,
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
            } else {
                if (custSettlements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد سندات تسديد مسجلة لهذا العميل", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(custSettlements) { st ->
                            SettlementItemCard(
                                settlement = st,
                                currency = currency,
                                onDelete = { viewModel.deleteSettlement(st) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettlementDialog) {
        AddSettlementDialog(
            customerName = customer.name,
            settlementType = settlementType,
            currency = currency,
            onDismiss = { showSettlementDialog = false },
            onConfirm = { amount, notes ->
                viewModel.addSettlement(
                    customerId = customer.id,
                    customerName = customer.name,
                    type = settlementType,
                    amount = amount,
                    notes = notes
                )
                showSettlementDialog = false
            }
        )
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
fun SettlementItemCard(
    settlement: SettlementEntity,
    currency: String,
    onDelete: () -> Unit
) {
    val isPayment = settlement.type == "PAYMENT_TO_SUPPLIER"
    val typeTitle = if (isPayment) "سند صرف تسديد (مسلَّم للعميل)" else "سند قبض (مستلم من المشتري)"
    val typeColor = if (isPayment) RedAccent else GreenSuccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = typeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = typeColor)
                Text(text = settlement.dateString, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (settlement.notes.isNotBlank()) {
                    Text(text = "ملاحظة: ${settlement.notes}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                text = formatCurrency(settlement.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddSettlementDialog(
    customerName: String,
    settlementType: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isPayment = settlementType == "PAYMENT_TO_SUPPLIER"
    val title = if (isPayment) "سند صرف تسديد لـ: $customerName" else "سند قبض من: $customerName"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = if (isPayment) RedAccent else GreenSuccess) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ المدفوع ($currency) *") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان / ملاحظات") },
                    placeholder = { Text("مثال: دفعة تحت الحساب") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isPayment) RedAccent else GreenSuccess)
            ) {
                Text("حفظ السند", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
