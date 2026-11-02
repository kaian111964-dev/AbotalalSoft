package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmployeeEntity
import com.example.data.local.entity.EmployeeTransactionEntity
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val shopProfile by viewModel.shopProfile.collectAsState()
    val allEmployees by viewModel.allEmployees.collectAsState()
    val allTransactions by viewModel.allEmployeeTransactions.collectAsState()

    val currency = shopProfile?.currencySymbol ?: "ريال"

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var payingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var viewingHistoryEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }

    val filteredEmployees = remember(allEmployees, searchQuery) {
        if (searchQuery.isBlank()) allEmployees
        else allEmployees.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery) ||
            it.jobTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeCount = remember(allEmployees) { allEmployees.count { it.isActive } }
    val totalSalaries = remember(allEmployees) { allEmployees.filter { it.isActive }.sumOf { it.salary } }
    val totalDisbursedThisMonth = remember(allTransactions) {
        allTransactions.filter { it.transactionType != "DEDUCTION" }.sumOf { it.amount }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
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
                        modifier = Modifier.size(38.dp).testTag("employees_back_btn")
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
                                text = "إدارة الموظفين والرواتب",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "متابعة كادر العمل، الرواتب، السلف والمستحقات",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "إضافة موظف",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0256D0),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_employee_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة موظف جديد")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---------------- Summary Cards (3 Cards) ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // عدد الموظفين
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("كادر العمل", fontSize = 10.sp, color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$activeCount موظف",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0256D0)
                        )
                    }
                }

                // إجمالي الرواتب
                Surface(
                    modifier = Modifier.weight(1.2f),
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("إجمالي الرواتب الشهرية", fontSize = 10.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatCurrency(totalSalaries, "")} $currency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309),
                            maxLines = 1
                        )
                    }
                }

                // إجمالي المصروفات والسلف
                Surface(
                    modifier = Modifier.weight(1.2f),
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("المدفوع والمسدد", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatCurrency(totalDisbursedThisMonth, "")} $currency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF16A34A),
                            maxLines = 1
                        )
                    }
                }
            }

            // ---------------- Search Field ----------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن موظف بالاسم، المسمى، أو الهاتف...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )

            // ---------------- Employees List ----------------
            if (filteredEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (allEmployees.isEmpty()) "لم يتم إضافة موظفين بعد، اضغط على زر (+) لإضافة موظف" else "لا توجد نتائج مطابقة للبحث",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEmployees, key = { it.id }) { employee ->
                        EmployeeItemCard(
                            employee = employee,
                            currency = currency,
                            onPay = { payingEmployee = employee },
                            onEdit = { editingEmployee = employee },
                            onDelete = { viewModel.deleteEmployee(employee) },
                            onViewHistory = { viewingHistoryEmployee = employee },
                            onCall = { if (employee.phone.isNotBlank()) openPhoneCall(context, employee.phone) },
                            onWhatsApp = { if (employee.phone.isNotBlank()) openWhatsApp(context, employee.phone) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(70.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddEditEmployeeDialog(
            employee = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { emp ->
                viewModel.addOrUpdateEmployee(emp)
                showAddDialog = false
            }
        )
    }

    if (editingEmployee != null) {
        AddEditEmployeeDialog(
            employee = editingEmployee,
            onDismiss = { editingEmployee = null },
            onConfirm = { emp ->
                viewModel.addOrUpdateEmployee(emp)
                editingEmployee = null
            }
        )
    }

    if (payingEmployee != null) {
        EmployeePaymentDialog(
            employee = payingEmployee!!,
            currency = currency,
            onDismiss = { payingEmployee = null },
            onConfirm = { type, amount, notes ->
                viewModel.payEmployee(payingEmployee!!, type, amount, notes)
                payingEmployee = null
            }
        )
    }

    if (viewingHistoryEmployee != null) {
        val empTx = allTransactions.filter { it.employeeId == viewingHistoryEmployee!!.id }
        EmployeeHistoryDialog(
            employee = viewingHistoryEmployee!!,
            transactions = empTx,
            currency = currency,
            onDismiss = { viewingHistoryEmployee = null },
            onDeleteTx = { tx -> viewModel.deleteEmployeeTransaction(tx) }
        )
    }
}

@Composable
fun EmployeeItemCard(
    employee: EmployeeEntity,
    currency: String,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Avatar, Name, Job, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF0256D0),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (employee.isActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (employee.isActive) "نشط" else "متوقف",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (employee.isActive) Color(0xFF166534) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = employee.jobTitle,
                                fontSize = 11.sp,
                                color = Color(0xFF0256D0),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (employee.phone.isNotBlank()) {
                                Text(" • ${employee.phone}", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Phone & WhatsApp icons
                if (employee.phone.isNotBlank()) {
                    Row {
                        IconButton(onClick = onCall, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onWhatsApp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Chat, contentDescription = "واتساب", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Salary & Balance Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // الراتب
                Column {
                    Text("الراتب المتفق عليه", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text(
                        text = "${formatCurrency(employee.salary, "")} $currency / ${employee.salaryType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // الرصيد (مستحقات أو سلف)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        employee.balance < 0 -> Color(0xFFFEE2E2) // عليه سلفة
                        employee.balance > 0 -> Color(0xFFDCFCE7) // له مستحقات
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                employee.balance < 0 -> "عليه سلفة: "
                                employee.balance > 0 -> "له مستحق: "
                                else -> "الحساب: "
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                employee.balance < 0 -> Color(0xFFDC2626)
                                employee.balance > 0 -> Color(0xFF16A34A)
                                else -> Color(0xFF64748B)
                            }
                        )
                        Text(
                            text = when {
                                employee.balance < 0 -> "${formatCurrency(-employee.balance, "")} $currency"
                                employee.balance > 0 -> "${formatCurrency(employee.balance, "")} $currency"
                                else -> "خالص"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                employee.balance < 0 -> Color(0xFFDC2626)
                                employee.balance > 0 -> Color(0xFF16A34A)
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                }
            }

            // Action Buttons Row: صرف راتب/سلفة | السجل | تعديل | حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // صرف راتب / دفعة (Blue)
                Button(
                    onClick = onPay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.3f).height(36.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صرف دفعة / راتب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // كشف حركات (Outline Blue)
                OutlinedButton(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0256D0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0256D0)),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.weight(0.9f).height(36.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("السجل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // تعديل
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                }

                // حذف
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditEmployeeDialog(
    employee: EmployeeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (EmployeeEntity) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var phone by remember { mutableStateOf(employee?.phone ?: "") }
    var jobTitle by remember { mutableStateOf(employee?.jobTitle ?: "عامل فرز وميزان") }
    var salaryText by remember { mutableStateOf(employee?.salary?.let { if (it > 0) it.toString() else "" } ?: "") }
    var salaryType by remember { mutableStateOf(employee?.salaryType ?: "شهري") }
    var isActive by remember { mutableStateOf(employee?.isActive ?: true) }
    var notes by remember { mutableStateOf(employee?.notes ?: "") }

    val commonJobs = listOf("عامل فرز وميزان", "سائق نقل خردة", "حارس ومستودع", "محاسب / كاشير", "مشرف موقع")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (employee == null) "إضافة موظف جديد" else "تعديل بيانات الموظف",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0256D0),
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الموظف *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("المسمى الوظيفي / المهمة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("الراتب") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("نوع الراتب", fontSize = 10.sp, color = Color(0xFF64748B))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = salaryType == "شهري",
                                onClick = { salaryType = "شهري" },
                                label = { Text("شهري", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = salaryType == "يومي",
                                onClick = { salaryType = "يومي" },
                                label = { Text("يومي", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حالة الموظف نشط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val emp = (employee ?: EmployeeEntity(name = "")).copy(
                            name = name.trim(),
                            phone = phone.trim(),
                            jobTitle = jobTitle.trim(),
                            salary = salaryText.toDoubleOrNull() ?: 0.0,
                            salaryType = salaryType,
                            isActive = isActive,
                            notes = notes.trim()
                        )
                        onConfirm(emp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ الموظف", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun EmployeePaymentDialog(
    employee: EmployeeEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var paymentType by remember { mutableStateOf("SALARY") } // SALARY, ADVANCE, BONUS, DEDUCTION
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("صرف مستحقات / سلفة", fontWeight = FontWeight.Bold, color = Color(0xFF0256D0), fontSize = 16.sp)
                Text("الموظف: ${employee.name} (${employee.jobTitle})", fontSize = 11.sp, color = Color(0xFF64748B))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("نوع العملية المالية:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = paymentType == "SALARY",
                        onClick = { paymentType = "SALARY" },
                        label = { Text("صرف راتب", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentType == "ADVANCE",
                        onClick = { paymentType = "ADVANCE" },
                        label = { Text("سلفة موظف", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentType == "BONUS",
                        onClick = { paymentType = "BONUS" },
                        label = { Text("مكافأة", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.8f)
                    )
                    FilterChip(
                        selected = paymentType == "DEDUCTION",
                        onClick = { paymentType = "DEDUCTION" },
                        label = { Text("خصم", fontSize = 10.sp) },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ ($currency) *") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0256D0))
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("بيان وملاحظات العملية") },
                    placeholder = { Text("مثال: راتب شهر مايو أو سلفة عاجلة...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ملاحظة: سيتم خصم هذا المبلغ تلقائياً من الصندوق وتسجيله في المصروفات التشغيلية لتصفية صافي الربح بدقة.",
                        fontSize = 10.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(paymentType, amt, notes.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تأكيد وصرف المبلغ", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}

@Composable
fun EmployeeHistoryDialog(
    employee: EmployeeEntity,
    transactions: List<EmployeeTransactionEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onDeleteTx: (EmployeeTransactionEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("سجل حركات الموظف", fontWeight = FontWeight.Bold, color = Color(0xFF0256D0), fontSize = 15.sp)
                    Text(employee.name, fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد حركات سابقة مسجلة لهذا الموظف", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transactions) { tx ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onDeleteTx(tx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = when (tx.transactionType) {
                                                "SALARY" -> "صرف راتب: ${formatCurrency(tx.amount, "")} $currency"
                                                "ADVANCE" -> "سلفة: ${formatCurrency(tx.amount, "")} $currency"
                                                "BONUS" -> "مكافأة: ${formatCurrency(tx.amount, "")} $currency"
                                                else -> "خصم: ${formatCurrency(tx.amount, "")} $currency"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tx.transactionType) {
                                                "SALARY" -> Color(0xFF0256D0)
                                                "ADVANCE" -> Color(0xFFDC2626)
                                                "BONUS" -> Color(0xFF16A34A)
                                                else -> Color(0xFF92400E)
                                            }
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(tx.notes, fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                        Text(tx.dateString, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إغلاق", color = Color.White)
            }
        }
    )
}
