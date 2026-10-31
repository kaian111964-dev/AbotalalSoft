package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AbuTalalTopBar
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*

@Composable
fun ExpensesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ر.ي"
    val allExpenses by viewModel.allExpenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val totalExpenses = allExpenses.sumOf { it.amount }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "مصروفات المحل التشغيلية",
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFD97706),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة مصروف")
            }
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
            // Total Expenses Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي المصروفات التشغيلية:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            text = formatCurrency(totalExpenses, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "سجل المصروفات (${allExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )

            if (allExpenses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد مصروفات مسجلة بعد", color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text("اضغط على زر (+) بالأسفل لإضافة مصروف جديد", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allExpenses) { exp ->
                        ExpenseItemCard(
                            expense = exp,
                            currency = currency,
                            onDelete = { viewModel.deleteExpense(exp) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            currency = currency,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, amount, notes ->
                viewModel.addExpense(title, category, amount, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    currency: String,
    onDelete: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = expense.dateString, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (expense.notes.isNotBlank()) {
                    Text(text = "ملاحظات: ${expense.notes}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            Text(
                text = formatCurrency(expense.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD97706)
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String) -> Unit
) {
    val categories = listOf("أجور وعمالة", "نقل وشحن", "إيجار المحل", "كهرباء ومياه", "صيانة موازين", "ضيافة وشاي", "أخرى")
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل مصروف جديد", fontWeight = FontWeight.Bold, color = BluePrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("التصنيف:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("بيان المصروف *") },
                    placeholder = { Text("مثال: أجرة عمال تحميل خردة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ ($currency) *") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    val a = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && a > 0) {
                        onConfirm(title, selectedCategory, a, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("حفظ المصروف", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
