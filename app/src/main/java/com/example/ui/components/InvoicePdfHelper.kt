package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.InvoiceItemEntity
import com.example.data.local.entity.ShopProfileEntity
import java.io.File
import java.io.FileOutputStream

object InvoicePdfHelper {

    fun generateInvoicePdf(
        context: Context,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        shopProfile: ShopProfileEntity?
    ): File? {
        try {
            val pdfDoc = PdfDocument()
            // Standard A4 page size in PostScript points: 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            drawInvoiceContent(canvas, pageWidth, pageHeight, invoice, items, shopProfile)

            pdfDoc.finishPage(page)

            val dir = File(context.cacheDir, "invoices")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "invoice_${invoice.invoiceNumber}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawInvoiceContent(
        canvas: Canvas,
        width: Int,
        height: Int,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        shopProfile: ShopProfileEntity?
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val currency = shopProfile?.currencySymbol ?: "ريال"
        val isPurchase = invoice.invoiceType == "PURCHASE"
        val invoiceTitle = if (isPurchase) "سند مشتريات خردة" else "فاتورة مبيعات خردة"

        // 1. White Background
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 2. Top Header Wave / Banner
        paint.color = Color.parseColor("#0256D0")
        val headerRect = RectF(20f, 20f, (width - 20).toFloat(), 110f)
        canvas.drawRoundRect(headerRect, 16f, 16f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ابوطلال سوفت", width / 2f, 52f, paint)

        // Subtitle
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        val shopSub = shopProfile?.shopName?.ifBlank { "ابو هتاف لبيع وشراء الخردة" } ?: "ابو هتاف لبيع وشراء الخردة"
        canvas.drawText("- $shopSub -", width / 2f, 72f, paint)

        // Phone & Address
        paint.textSize = 10f
        val phone = shopProfile?.phoneNumber?.ifBlank { "778215553" } ?: "778215553"
        val address = shopProfile?.address?.ifBlank { "الفرع الرئيسي" } ?: "الفرع الرئيسي"
        canvas.drawText("هاتف: $phone   |   العنوان: $address", width / 2f, 92f, paint)

        // 3. Official Invoice Capsule Badge
        val badgeRect = RectF(180f, 120f, (width - 180).toFloat(), 152f)
        paint.color = Color.parseColor("#003E9E")
        canvas.drawRoundRect(badgeRect, 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("فاتورة خردة رسمية", width / 2f, 142f, paint)

        // 4. Two Info Cards Side by Side (Customer & Invoice meta)
        // Right Card (Invoice Number & Date)
        val rightCard = RectF((width / 2f + 10), 164f, (width - 24).toFloat(), 216f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(rightCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rightCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$invoiceTitle: ${invoice.invoiceNumber}", (width - 34).toFloat(), 186f, paint)
        canvas.drawText("التاريخ: ${invoice.dateString}", (width - 34).toFloat(), 204f, paint)

        // Left Card (Customer Name & Phone)
        val leftCard = RectF(24f, 164f, (width / 2f - 10), 216f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(leftCard, 8f, 8f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(leftCard, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        val custName = invoice.customerName.ifBlank { "عميل نقدي" }
        canvas.drawText("العميل: $custName", (width / 2f - 20), 186f, paint)
        val custPhone = invoice.customerPhone.ifBlank { "---" }
        canvas.drawText("الهاتف: $custPhone", (width / 2f - 20), 204f, paint)

        // 5. Items Table
        var tableY = 230f
        val colM = 40f
        val colItem = 180f
        val colWeight = 300f
        val colPrice = 420f
        val colTotal = (width - 30).toFloat()

        // Table Header
        paint.color = Color.parseColor("#0256D0")
        val thRect = RectF(24f, tableY, (width - 24).toFloat(), tableY + 28f)
        canvas.drawRoundRect(thRect, 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("م", 38f, tableY + 18f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("الصنف", colItem, tableY + 18f, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("الكمية", (colItem + colWeight) / 2f, tableY + 18f, paint)
        canvas.drawText("سعر الكيلو", (colWeight + colPrice) / 2f, tableY + 18f, paint)
        canvas.drawText("الإجمالي", (colPrice + colTotal) / 2f, tableY + 18f, paint)

        tableY += 30f

        // Table Rows
        items.forEachIndexed { index, item ->
            val rowHeight = 26f
            val isEven = index % 2 == 0
            paint.color = if (isEven) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
            canvas.drawRect(24f, tableY, (width - 24).toFloat(), tableY + rowHeight, paint)

            paint.color = Color.parseColor("#E2E8F0")
            paint.style = Paint.Style.STROKE
            canvas.drawRect(24f, tableY, (width - 24).toFloat(), tableY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10.5f

            // م
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("${index + 1}", 38f, tableY + 18f, paint)

            // الصنف
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(item.scrapName, colItem, tableY + 18f, paint)

            // الكمية
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("${item.weightKg} كجم", (colItem + colWeight) / 2f, tableY + 18f, paint)

            // سعر الكيلو
            canvas.drawText("${item.unitPrice} $currency", (colWeight + colPrice) / 2f, tableY + 18f, paint)

            // الإجمالي
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#0256D0")
            canvas.drawText("${item.subtotal} $currency", (colPrice + colTotal) / 2f, tableY + 18f, paint)

            tableY += rowHeight
        }

        tableY += 16f

        // 6. Summary Section (Matching uploaded screenshot)
        // Left Column: 3 stacked pills (إجمالي الفاتورة, المبلغ المدفوع نقدياً, المتبقي)
        val pillWidth = 240f
        val pillHeight = 32f

        // 1. Total Pill (Blue)
        val pillTotal = RectF(24f, tableY, 24f + pillWidth, tableY + pillHeight)
        paint.color = Color.parseColor("#DBEAFE")
        canvas.drawRoundRect(pillTotal, 8f, 8f, paint)
        paint.color = Color.parseColor("#0256D0")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("إجمالي الفاتورة:  ${invoice.totalAmount} $currency", 24f + pillWidth - 14f, tableY + 21f, paint)

        // 2. Paid Pill (Green)
        val pillPaid = RectF(24f, tableY + 38f, 24f + pillWidth, tableY + 38f + pillHeight)
        paint.color = Color.parseColor("#DCFCE7")
        canvas.drawRoundRect(pillPaid, 8f, 8f, paint)
        paint.color = Color.parseColor("#15803D")
        canvas.drawText("المبلغ المدفوع نقدياً:  ${invoice.paidAmount} $currency", 24f + pillWidth - 14f, tableY + 59f, paint)

        // 3. Remaining Pill (Red)
        val balanceText = if (isPurchase) "المتبقي له في ذمتنا" else "المتبقي عليه لنا"
        val pillRem = RectF(24f, tableY + 76f, 24f + pillWidth, tableY + 76f + pillHeight)
        paint.color = Color.parseColor("#FEE2E2")
        canvas.drawRoundRect(pillRem, 8f, 8f, paint)
        paint.color = Color.parseColor("#B91C1C")
        canvas.drawText("$balanceText:  ${invoice.remainingAmount} $currency", 24f + pillWidth - 14f, tableY + 97f, paint)

        // Right Column: Thank you message box
        val thankCard = RectF(24f + pillWidth + 16f, tableY, (width - 24).toFloat(), tableY + 108f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(thankCard, 10f, 10f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(thankCard, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0256D0")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("شكراً لتعاملكم معنا", (thankCard.left + thankCard.right) / 2f, tableY + 44f, paint)

        paint.color = Color.parseColor("#475569")
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        val footerNote = shopProfile?.invoiceFooterNote?.ifBlank { "نشتري كافة أنواع الخردة والمعادن بأفضل الأسعار" } ?: "نشتري كافة أنواع الخردة والمعادن بأفضل الأسعار"
        canvas.drawText(footerNote, (thankCard.left + thankCard.right) / 2f, tableY + 72f, paint)

        // 7. Footer: توقيع المسؤول | ختم رسمي | معاً نحو بيئة أنظف
        val footerY = (height - 80).toFloat()

        // Signature line
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 11f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("توقيع المسؤول: _________________", 200f, footerY + 20f, paint)

        // Environmental slogan
        paint.color = Color.parseColor("#15803D")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("معاً .. نحو بيئة أنظف ♻", (width - 200).toFloat(), footerY + 20f, paint)

        // Official Stamp Circle in center
        paint.color = Color.parseColor("#0256D0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(width / 2f, footerY + 16f, 32f, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ابوطلال سوفت", width / 2f, footerY + 12f, paint)
        canvas.drawText("Abu Talal Soft", width / 2f, footerY + 24f, paint)
    }

    fun shareInvoicePdf(
        context: Context,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        shopProfile: ShopProfileEntity?
    ) {
        val file = generateInvoicePdf(context, invoice, items, shopProfile)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "فشل إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "فاتورة خردة رقم ${invoice.invoiceNumber}")
                putExtra(Intent.EXTRA_TEXT, "مرفق فاتورة خردة رقم ${invoice.invoiceNumber} من تطبيق ابوطلال سوفت")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "إرسال فاتورة PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء مشاركة الفاتورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
