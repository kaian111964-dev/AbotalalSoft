package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ShopProfileEntity
import com.example.ui.theme.*

@Composable
fun PrintInvoiceDialog(
    invoice: InvoiceEntity,
    items: List<InvoiceItemEntity>,
    shopProfile: ShopProfileEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currency = shopProfile?.currencySymbol ?: "ريال"
    val isPurchase = invoice.invoiceType == "PURCHASE"
    val invoiceTypeName = if (isPurchase) "سند مشتريات خردة" else "فاتورة مبيعات خردة"
    val balanceLabel = if (isPurchase) "المتبقي له في ذمتنا" else "المتبقي عليه لنا"

    val formattedShareText = buildString {
        appendLine("════════════════════════════════")
        appendLine("ابوطلال سوفت - ${shopProfile?.shopName ?: "لتجارة الخردة"}")
        appendLine("فاتورة خردة رسمية")
        appendLine("هاتف: ${shopProfile?.phoneNumber ?: "778215553"} | ${shopProfile?.address ?: "الفرع الرئيسي"}")
        appendLine("════════════════════════════════")
        appendLine("$invoiceTypeName: ${invoice.invoiceNumber}")
        appendLine("التاريخ: ${invoice.dateString}")
        appendLine("العميل: ${invoice.customerName}")
        if (invoice.customerPhone.isNotBlank()) appendLine("الهاتف: ${invoice.customerPhone}")
        appendLine("────────────────────────────────")
        appendLine("الأصناف الموزونة:")
        items.forEachIndexed { index, item ->
            appendLine("${index + 1}. ${item.scrapName} | ${item.weightKg} كجم × ${item.unitPrice} = ${item.subtotal} $currency")
        }
        appendLine("────────────────────────────────")
        appendLine("إجمالي الفاتورة: ${invoice.totalAmount} $currency")
        appendLine("المبلغ المدفوع نقدياً: ${invoice.paidAmount} $currency")
        appendLine("$balanceLabel: ${invoice.remainingAmount} $currency")
        if (invoice.notes.isNotBlank()) appendLine("ملاحظات: ${invoice.notes}")
        appendLine("════════════════════════════════")
        appendLine(shopProfile?.invoiceFooterNote ?: "شكراً لتعاملكم معنا - نشتري كافة أنواع الخردة بأفضل الأسعار")
        appendLine("معاً .. نحو بيئة أنظف ♻")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar of the Dialog
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0256D0),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }

                        Text(
                            text = "معاينة الفاتورة والطباعة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Scrollable Invoice Body exactly matching the user's uploaded invoice screenshot
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Top Header Card (ابوطلال سوفت)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0256D0)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Recycle Logo
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Recycling, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }

                                // Center Shop Titles
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "ابوطلال سوفت",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "- ${shopProfile?.shopName?.ifBlank { "ابو هتاف لبيع وشراء الخردة" } ?: "ابو هتاف لبيع وشراء الخردة"} -",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE047),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "هاتف: ${shopProfile?.phoneNumber ?: "778215553"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = "العنوان: ${shopProfile?.address ?: "الفرع الرئيسي"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 9.sp
                                    )
                                }

                                // Logo Circle
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 2. Capsule Badge: فاتورة خردة رسمية
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF003E9E),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "فاتورة خردة رسمية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 3. Two Info Boxes: Invoice & Customer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Customer Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "العميل: ${invoice.customerName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الهاتف: ${invoice.customerPhone.ifBlank { "غير مسجل" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        // Invoice meta Box
                        Surface(
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$invoiceTypeName: ${invoice.invoiceNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A),
                                        fontSize = 10.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "التاريخ: ${invoice.dateString}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    // 4. Official Table
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column {
                            // Header Row
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF0256D0)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الإجمالي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                    Text("سعر الكيلو", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                                    Text("الكمية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("الصنف", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("م", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.4f), textAlign = TextAlign.Center)
                                }
                            }

                            // Items Rows
                            items.forEachIndexed { idx, item ->
                                val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatCurrency(item.subtotal, "")} $currency",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0256D0),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${formatCurrency(item.unitPrice, "")} $currency",
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${item.weightKg} كجم",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.weight(1.5f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(text = item.scrapName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Image(
                                            painter = painterResource(id = getScrapDrawableRes(item.scrapName)),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.weight(0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }

                    // 5. Financial Summary & Thank You Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left 3 Stacked Pills (Total, Paid, Remaining)
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Total Pill
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDBEAFE),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "إجمالي الفاتورة: ${formatCurrency(invoice.totalAmount, "")} $currency",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0256D0),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Paid Pill
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "المبلغ المدفوع نقدياً: ${formatCurrency(invoice.paidAmount, "")} $currency",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Remaining Pill
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "$balanceLabel: ${formatCurrency(invoice.remainingAmount, "")} $currency",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Right: Thank You Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF0256D0), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("شكراً لتعاملكم معنا", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0256D0))
                                Text("نشتري كافة أنواع الخردة والمعادن بأفضل الأسعار", fontSize = 8.sp, color = Color(0xFF475569), textAlign = TextAlign.Center)
                            }
                        }
                    }

                    // 6. Footer (توقيع المسؤول | الختم | معاً نحو بيئة أنظف)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "توقيع المسؤول:\n...........................",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )

                        // Circular seal
                        Surface(
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0256D0)),
                            color = Color(0xFFF0F7FF),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("ابوطلال سوفت", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0256D0))
                                Text("Abu Talal", fontSize = 6.sp, color = Color(0xFF0256D0))
                            }
                        }

                        Text(
                            text = "معاً .. نحو بيئة أنظف ♻",
                            fontSize = 10.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Action Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAFC),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share Text Button
                        OutlinedButton(
                            onClick = {
                                shareText(context, invoiceTypeName, formattedShareText)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0256D0)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0256D0))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة كنص", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Print & Share PDF Button
                        Button(
                            onClick = {
                                InvoicePdfHelper.shareInvoicePdf(context, invoice, items, shopProfile)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).height(42.dp).testTag("print_pdf_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0256D0))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة ومشاركة PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
