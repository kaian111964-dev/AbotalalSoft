package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.entity.InvoiceEntity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double, symbol: String = "ر.ي"): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $symbol"
}

fun formatWeight(weight: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    formatter.maximumFractionDigits = 2
    return "${formatter.format(weight)} كجم"
}

fun getScrapColor(scrapName: String): Color {
    return when {
        scrapName.contains("أحمر") -> CopperRed
        scrapName.contains("أصفر") -> CopperYellow
        scrapName.contains("خفيف") -> MetalLight
        scrapName.contains("ثقيل") -> MetalHeavy
        scrapName.contains("ألمنيوم") || scrapName.contains("المنيوم") -> AluminumColor
        scrapName.contains("بطاريات") || scrapName.contains("بطارية") -> BatteryColor
        scrapName.contains("حديد") -> IronColor
        scrapName.contains("بلاستيك") -> PlasticColor
        else -> BlueSecondary
    }
}

fun getScrapDrawableRes(scrapName: String): Int {
    return when {
        scrapName.contains("أحمر") -> com.example.R.drawable.ic_scrap_copper_red
        scrapName.contains("أصفر") -> com.example.R.drawable.ic_scrap_copper_yellow
        scrapName.contains("خفيف") -> com.example.R.drawable.ic_scrap_metal_light
        scrapName.contains("ثقيل") -> com.example.R.drawable.ic_scrap_metal_heavy
        scrapName.contains("ألمنيوم") || scrapName.contains("المنيوم") -> com.example.R.drawable.ic_scrap_aluminum
        scrapName.contains("بطاريات") || scrapName.contains("بطارية") -> com.example.R.drawable.ic_scrap_battery
        scrapName.contains("حديد") -> com.example.R.drawable.ic_scrap_iron
        scrapName.contains("بلاستيك") -> com.example.R.drawable.ic_scrap_plastic
        else -> com.example.R.drawable.ic_scrap_copper_red
    }
}

fun openPhoneCall(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

fun openWhatsApp(context: Context, phoneNumber: String, message: String = "السلام عليكم ورحمة الله وبركاته") {
    try {
        val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").trim()
        val fullNumber = if (cleanNumber.startsWith("7")) "967$cleanNumber" else cleanNumber
        val url = "https://api.whatsapp.com/send?phone=$fullNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

fun shareText(context: Context, title: String, content: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbuTalalTopBar(
    title: String = "ابوطلال سوفت",
    subtitle: String = "",
    onBack: (() -> Unit)? = null,
    showDeveloperAction: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var showDevDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "ابوطلال سوفت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = "شعار التطبيق",
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            actions()
            if (showDeveloperAction) {
                IconButton(
                    onClick = { showDevDialog = true },
                    modifier = Modifier.testTag("developer_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "حقوق المطور",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BluePrimary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )

    if (showDevDialog) {
        DeveloperInfoDialog(onDismiss = { showDevDialog = false })
    }
}

@Composable
fun DeveloperInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حقوق المطور والبرمجة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo_abotalal),
                    contentDescription = "ابوطلال سوفت",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = CircleShape,
                    color = BlueLight,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "المبرمج والمطور:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "عبدالحميد داوؤد",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    text = "ابوطلال سوفت لإدارة الخردة",
                    style = MaterialTheme.typography.bodySmall,
                    color = RedAccent,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "أرقام التواصل (اتصال وواتساب):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                PhoneContactRow(
                    phoneNumber = "778215553",
                    onCall = { openPhoneCall(context, "778215553") },
                    onWhatsApp = { openWhatsApp(context, "778215553", "مرحباً يا باشمهندس عبدالحميد بخصوص تطبيق ابوطلال سوفت") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                PhoneContactRow(
                    phoneNumber = "700897775",
                    onCall = { openPhoneCall(context, "700897775") },
                    onWhatsApp = { openWhatsApp(context, "700897775", "مرحباً يا باشمهندس عبدالحميد بخصوص تطبيق ابوطلال سوفت") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.testTag("close_dev_dialog_btn")
            ) {
                Text("إغلاق", color = Color.White)
            }
        }
    )
}

@Composable
fun PhoneContactRow(
    phoneNumber: String,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .background(BlueLight, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "اتصال",
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .size(38.dp)
                        .background(GreenLight, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "واتساب",
                        tint = GreenSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceHistoryCard(
    invoice: InvoiceEntity,
    currency: String,
    onView: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete & View actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onView, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "عرض", tint = Color(0xFF0256D0), modifier = Modifier.size(18.dp))
                    }
                }

                // Type & Number badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (invoice.invoiceType == "SALE") Color(0xFFDBEAFE) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (invoice.invoiceType == "SALE") "فاتورة بيع" else "فاتورة شراء",
                            color = if (invoice.invoiceType == "SALE") Color(0xFF0256D0) else Color(0xFF16A34A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatCurrency(invoice.totalAmount, "")} $currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0256D0)
                )

                Text(
                    text = invoice.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (invoice.remainingAmount > 0) {
                    Text(
                        text = "متبقي: ${formatCurrency(invoice.remainingAmount, "")} $currency",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    Text(
                        text = "خالص نقدياً",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                Text(
                    text = invoice.dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun AddCustomCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("كجم") }
    var buyPriceText by remember { mutableStateOf("") }
    var sellPriceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة صنف خردة جديد",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0256D0),
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم صنف الخردة *") },
                    placeholder = { Text("مثال: رصاص، نحاس أبيض، زنك...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("وحدة القياس") },
                    placeholder = { Text("كجم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceText,
                        onValueChange = { buyPriceText = it },
                        label = { Text("سعر الشراء") },
                        placeholder = { Text("0.0") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("سعر البيع") },
                        placeholder = { Text("0.0") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val bp = buyPriceText.toDoubleOrNull() ?: 0.0
                        val sp = sellPriceText.toDoubleOrNull() ?: 0.0
                        onConfirm(name.trim(), unit.trim().ifBlank { "كجم" }, bp, sp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ الصنف", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF64748B))
            }
        }
    )
}

