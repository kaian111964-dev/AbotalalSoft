package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val currency = shopProfile?.currencySymbol ?: "ر.ي"
    val invoiceTypeName = if (invoice.invoiceType == "PURCHASE") "سند مشتريات خردة" else "فاتورة مبيعات خردة"
    val balanceLabel = if (invoice.invoiceType == "PURCHASE") "المتبقي له في ذمتنا:" else "المتبقي عليه للمحل:"

    val formattedShareText = buildString {
        appendLine("=================================")
        appendLine("ابوطلال سوفت - ${shopProfile?.shopName ?: "لتجارة الخردة"}")
        if (!shopProfile?.invoiceHeaderNote.isNullOrBlank()) {
            appendLine(shopProfile?.invoiceHeaderNote)
        }
        appendLine("هاتف: ${shopProfile?.phoneNumber ?: "778215553"}")
        appendLine("العنوان: ${shopProfile?.address ?: "اليمن"}")
        appendLine("=================================")
        appendLine("$invoiceTypeName: ${invoice.invoiceNumber}")
        appendLine("التاريخ: ${invoice.dateString}")
        appendLine("العميل: ${invoice.customerName}")
        if (invoice.customerPhone.isNotBlank()) appendLine("الهاتف: ${invoice.customerPhone}")
        appendLine("---------------------------------")
        appendLine("الأصناف الموزونة:")
        items.forEachIndexed { index, item ->
            appendLine("${index + 1}. ${item.scrapName}: ${formatWeight(item.weightKg)} × ${formatCurrency(item.unitPrice, currency)} = ${formatCurrency(item.subtotal, currency)}")
        }
        appendLine("---------------------------------")
        appendLine("إجمالي الفاتورة: ${formatCurrency(invoice.totalAmount, currency)}")
        appendLine("المبلغ المدفوع نقدياً: ${formatCurrency(invoice.paidAmount, currency)}")
        appendLine("$balanceLabel ${formatCurrency(invoice.remainingAmount, currency)}")
        if (invoice.notes.isNotBlank()) appendLine("ملاحظات: ${invoice.notes}")
        appendLine("=================================")
        appendLine(shopProfile?.invoiceFooterNote ?: "شكراً لتعاملكم معنا")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ابوطلال سوفت",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BluePrimary
                    )
                    Text(
                        text = invoiceTypeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Shop Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BlueLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shopProfile?.shopName?.ifBlank { "محل ابوطلال لتجارة الخردة" } ?: "محل ابوطلال لتجارة الخردة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = "الإدارة: ${shopProfile?.ownerName?.ifBlank { "عبدالحميد داوؤد" } ?: "عبدالحميد داوؤد"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "هاتف: ${shopProfile?.phoneNumber ?: "778215553"} | ${shopProfile?.address ?: "صنعاء"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "اسم العميل:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = invoice.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "التاريخ:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = invoice.dateString, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BluePrimary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الصنف", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                    Text("الوزن", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text("السعر", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                    Text("الإجمالي", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                }

                // Table Items
                items.forEachIndexed { idx, item ->
                    val rowBg = if (idx % 2 == 0) SurfaceWhite else SurfaceVariantLight
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.scrapName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f))
                        Text(formatWeight(item.weightKg), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                        Text(formatCurrency(item.unitPrice, currency), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                        Text(formatCurrency(item.subtotal, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BluePrimary, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Financial Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي الفاتورة:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(formatCurrency(invoice.totalAmount, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("المبلغ المسلم نقداً:", style = MaterialTheme.typography.bodyMedium)
                    Text(formatCurrency(invoice.paidAmount, currency), style = MaterialTheme.typography.bodyMedium, color = GreenSuccess, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (invoice.remainingAmount > 0) RedLight else GreenLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = balanceLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.remainingAmount > 0) RedAccent else GreenSuccess
                        )
                        Text(
                            text = formatCurrency(invoice.remainingAmount, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.remainingAmount > 0) RedAccent else GreenSuccess
                        )
                    }
                }

                if (!shopProfile?.invoiceFooterNote.isNullOrBlank()) {
                    Text(
                        text = shopProfile?.invoiceFooterNote ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    shareText(context, invoiceTypeName, formattedShareText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.testTag("share_invoice_btn")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة الفاتورة", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_invoice_btn")
            ) {
                Text("إغلاق")
            }
        }
    )
}
