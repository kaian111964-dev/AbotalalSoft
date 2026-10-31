package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var ownerName by remember(shopProfile) { mutableStateOf(shopProfile?.ownerName ?: "") }
    var shopName by remember(shopProfile) { mutableStateOf(shopProfile?.shopName ?: "") }
    var phone by remember(shopProfile) { mutableStateOf(shopProfile?.phoneNumber ?: "") }
    var address by remember(shopProfile) { mutableStateOf(shopProfile?.address ?: "") }

    // Invoice Settings state
    var currencySymbol by remember(shopProfile) { mutableStateOf(shopProfile?.currencySymbol ?: "ر.ي") }
    var invoiceHeaderNote by remember(shopProfile) { mutableStateOf(shopProfile?.invoiceHeaderNote ?: "فاتورة خردة رسمية") }
    var invoiceFooterNote by remember(shopProfile) { mutableStateOf(shopProfile?.invoiceFooterNote ?: "شكراً لتعاملكم معنا - نشتري كافة أنواع الخردة والمعادن بأفضل الأسعار") }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var showDevDialog by remember { mutableStateOf(false) }

    // SAF Document Picker for Restore Backup
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonContent = reader.readText()
                reader.close()
                inputStream?.close()

                viewModel.restoreBackup(jsonContent) { success ->
                    if (success) {
                        Toast.makeText(context, "تمت استعادة النسخة الاحتياطية بنجاح", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "فشل استعادة النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "حدث خطأ أثناء قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "الإعدادات وإعدادات الفواتير والنسخ الاحتياطي",
                onBack = { viewModel.navigateBack() }
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Shop Profile
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بيانات المحل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("اسم صاحب المحل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("اسم المحل") },
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

                    Button(
                        onClick = {
                            viewModel.saveProfile(ownerName, shopName, phone, address)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_profile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ بيانات المحل", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Invoice & Voucher Settings (طلب المستخدم الصريح: اعدادات الفواتير والسندات)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات الفواتير والسندات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("رمز العملة في الفواتير") },
                        placeholder = { Text("مثال: ر.ي أو ريال يمني أو $") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = invoiceHeaderNote,
                        onValueChange = { invoiceHeaderNote = it },
                        label = { Text("عنوان / ترويسة الفاتورة الإضافية") },
                        placeholder = { Text("مثال: فاتورة خردة رسمية معتمدة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = invoiceFooterNote,
                        onValueChange = { invoiceFooterNote = it },
                        label = { Text("تذييل وملاحظات الفواتير والسندات") },
                        placeholder = { Text("مثال: شكراً لتعاملكم معنا") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.saveInvoiceSettings(invoiceHeaderNote, invoiceFooterNote, currencySymbol)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_invoice_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ إعدادات الفواتير والسندات", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 3: Backup & Restore
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي والاستعادة من الهاتف",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    Text(
                        text = "احتفظ بنسخة من جميع فواتير الخردة والعملاء والأسعار والمصروفات على ذاكرة هاتفك أو عبر مشاركتها لأي تطبيق خارجي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Export & Share Backup
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val json = viewModel.getBackupJson()
                                shareText(context, "نسخة احتياطية - ابوطلال سوفت", json)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ومشاركة النسخة الاحتياطية (ملف JSON)", fontWeight = FontWeight.Bold)
                    }

                    // Restore Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                restoreFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_file_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استعادة من ملف", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("restore_from_text_btn")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لصق نص النسخة", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Section 4: Pricing Quick Access
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
                    Text(
                        text = "إدارة التسعيرات اليومية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    ListItem(
                        headlineContent = { Text("أسعار مشتريات الخردة", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("إضافة وتحديث واعتماد تسعيرة الشراء اليومية") },
                        leadingContent = { Icon(Icons.Default.PriceCheck, contentDescription = null, tint = CopperRed) },
                        trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                        modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.PRICING_PURCHASE) }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("أسعار مبيعات الخردة", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("إضافة وتحديث واعتماد تسعيرة البيع اليومية") },
                        leadingContent = { Icon(Icons.Default.Sell, contentDescription = null, tint = GreenSuccess) },
                        trailingContent = { Icon(Icons.Default.ChevronLeft, contentDescription = null) },
                        modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.PRICING_SALE) }
                    )
                }
            }

            // Section 5: Developer Info Card
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BlueLight,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(30.dp))
                        }
                    }

                    Text(
                        text = "حقوق المبرمج والمطور",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Text(
                        text = "عبدالحميد داوؤد",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    Text(
                        text = "ابوطلال سوفت لإدارة حسابات ومبيعات ومشتريات الخردة",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedAccent,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "أرقام التواصل (اتصال وواتساب):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    PhoneContactRow(
                        phoneNumber = "778215553",
                        onCall = { openPhoneCall(context, "778215553") },
                        onWhatsApp = { openWhatsApp(context, "778215553") }
                    )

                    PhoneContactRow(
                        phoneNumber = "700897775",
                        onCall = { openPhoneCall(context, "700897775") },
                        onWhatsApp = { openWhatsApp(context, "700897775") }
                    )
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("استعادة النسخة الاحتياطية من نص", fontWeight = FontWeight.Bold, color = BluePrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصق نص النسخة الاحتياطية (JSON) هنا:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\"version\": 1, ...}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreBackup(restoreJsonText) { success ->
                                if (success) {
                                    Toast.makeText(context, "تمت استعادة البيانات بنجاح", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                } else {
                                    Toast.makeText(context, "فشل استعادة البيانات. تأكد من صحة النص", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("استعادة الآن", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showDevDialog) {
        DeveloperInfoDialog(onDismiss = { showDevDialog = false })
    }
}
