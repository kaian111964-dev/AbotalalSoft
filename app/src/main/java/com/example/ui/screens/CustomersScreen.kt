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
import com.example.data.local.entity.CustomerEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ر.ي"

    val allCustomers by viewModel.allCustomers.collectAsState()
    val allInvoices by viewModel.allInvoices.collectAsState()
    val allSettlements by viewModel.allSettlements.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableIntStateOf(0) } // 0: All, 1: Debt Owed to them (لهم), 2: Debt Owed by them (عليهم)
    var showAddDialog by remember { mutableStateOf(false) }

    // Compute balance for all customers in real time
    val customersWithBalances = remember(allCustomers, allInvoices, allSettlements) {
        allCustomers.map { customer ->
            val custInvoices = allInvoices.filter { it.customerId == customer.id || it.customerName.trim() == customer.name.trim() }
            val custSettlements = allSettlements.filter { it.customerId == customer.id }

            // Purchases: shop owes supplier remaining
            val purchaseRemaining = custInvoices.filter { it.invoiceType == "PURCHASE" }.sumOf { it.remainingAmount }
            val purchaseSettlements = custSettlements.filter { it.type == "PAYMENT_TO_SUPPLIER" }.sumOf { it.amount }
            val netOwedToSupplier = (purchaseRemaining - purchaseSettlements).coerceAtLeast(0.0)

            // Sales: buyer owes shop remaining
            val saleRemaining = custInvoices.filter { it.invoiceType == "SALE" }.sumOf { it.remainingAmount }
            val saleSettlements = custSettlements.filter { it.type == "RECEIPT_FROM_BUYER" }.sumOf { it.amount }
            val netOwedByBuyer = (saleRemaining - saleSettlements).coerceAtLeast(0.0)

            Triple(customer, netOwedToSupplier, netOwedByBuyer)
        }
    }

    val filteredList = customersWithBalances.filter { item ->
        val matchesSearch = item.first.name.contains(searchQuery, ignoreCase = true) ||
                item.first.phone.contains(searchQuery)
        val matchesFilter = when (filterType) {
            1 -> item.second > 0 // لهم ديون
            2 -> item.third > 0 // عليهم ديون
            else -> true
        }
        matchesSearch && matchesFilter
    }

    val totalOwedToSuppliers = customersWithBalances.sumOf { it.second }
    val totalOwedByBuyers = customersWithBalances.sumOf { it.third }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "العملاء والديون والتسديد (كم متبقي لهم)",
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل")
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Debt Overview Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي ديون الموردين (لهم):",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatCurrency(totalOwedToSuppliers, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RedAccent
                        )
                        Text(
                            text = "محل الخردة مدين للموردين",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = RedAccent
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(50.dp).padding(horizontal = 8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إجمالي ديون المشترين (عليهم):",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatCurrency(totalOwedByBuyers, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenSuccess
                        )
                        Text(
                            text = "مستحقات للمحل لدى المشترين",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = GreenSuccess
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن عميل بالاسم أو الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_search_input"),
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == 0,
                    onClick = { filterType = 0 },
                    label = { Text("الكل (${allCustomers.size})") }
                )
                FilterChip(
                    selected = filterType == 1,
                    onClick = { filterType = 1 },
                    label = { Text("متبقي لهم") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedLight, selectedLabelColor = RedAccent)
                )
                FilterChip(
                    selected = filterType == 2,
                    onClick = { filterType = 2 },
                    label = { Text("متبقي عليهم") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenLight, selectedLabelColor = GreenSuccess)
                )
            }

            // Customers List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (allCustomers.isEmpty()) "لا يوجد عملاء مسجلين بعد. سيتم تسجيل العملاء تلقائياً عند إنشاء فواتير الشراء أو اضغط على (+)" else "لا توجد نتائج مطابقة للبحث",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { (customer, owedToThem, owedByThem) ->
                        CustomerItemCard(
                            customer = customer,
                            owedToThem = owedToThem,
                            owedByThem = owedByThem,
                            currency = currency,
                            onClick = { viewModel.selectCustomer(customer.id) },
                            onCall = { if (customer.phone.isNotBlank()) openPhoneCall(context, customer.phone) },
                            onWhatsApp = { if (customer.phone.isNotBlank()) openWhatsApp(context, customer.phone) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, address, notes ->
                viewModel.addOrUpdateCustomer(
                    CustomerEntity(
                        name = name,
                        phone = phone,
                        address = address,
                        notes = notes
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: CustomerEntity,
    owedToThem: Double,
    owedByThem: Double,
    currency: String,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = BlueLight,
                        modifier = Modifier.size(42.dp)
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
                            color = TextPrimary
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
                        IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = BluePrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onWhatsApp, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = GreenSuccess, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Balance Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (owedToThem > 0) {
                    Surface(
                        color = RedLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "متبقي له في ذمتنا: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatCurrency(owedToThem, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = RedAccent,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else if (owedByThem > 0) {
                    Surface(
                        color = GreenLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "متبقي عليه للمحل: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatCurrency(owedByThem, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = GreenSuccess,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "الحساب خالص (لا توجد ديون)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عميل / مورد جديد", fontWeight = FontWeight.Bold, color = BluePrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, address, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("حفظ العميل", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
