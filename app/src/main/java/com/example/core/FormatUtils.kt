package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpInvoice
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object FormatUtils {
    private val indianLocale = Locale("en", "IN")
    private val currencyFmt = NumberFormat.getCurrencyInstance(indianLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun inr(amount: Double): String {
        val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        return try {
            currencyFmt.format(safe)
        } catch (_: Exception) {
            "₹${safe.roundToLong()}"
        }
    }

    fun todayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun currentMonthStr(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun exportAndShareInvoicePdf(
        context: Context,
        invoice: ErpInvoice,
        settings: BusinessSettings
    ): Result<File> = runCatching {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c: Canvas = page.canvas

        val primaryPaint = Paint().apply {
            color = Color.parseColor("#0A6874")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#D1FAE5")
            textSize = 11f
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#0B1B22")
            textSize = 11f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#0B1B22")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#5B6B72")
            textSize = 10f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        // Header banner
        c.drawRect(0f, 0f, 595f, 96f, primaryPaint)
        c.drawText(settings.businessName.ifBlank { "My Business" }, 32f, 40f, titlePaint)
        val meta = listOfNotNull(
            settings.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" },
            settings.phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" }
        ).joinToString("  |  ")
        c.drawText(meta, 32f, 60f, subTitlePaint)
        c.drawText(settings.address.take(70), 32f, 76f, subTitlePaint)

        titlePaint.textSize = 15f
        c.drawText(invoice.docType.uppercase(), 420f, 40f, titlePaint)
        c.drawText("#${invoice.invoiceNumber}", 420f, 60f, subTitlePaint)
        c.drawText("Date: ${invoice.dateStr}", 420f, 76f, subTitlePaint)

        // Bill To
        var y = 126f
        c.drawText("BILL TO", 32f, y, mutedPaint)
        y += 16f
        c.drawText(invoice.clientName.ifBlank { "Walk-in Customer" }, 32f, y, boldPaint)
        y += 15f
        if (invoice.clientPhone.isNotBlank()) {
            c.drawText("Phone: ${invoice.clientPhone}", 32f, y, bodyPaint)
            y += 14f
        }
        if (invoice.clientGstin.isNotBlank()) {
            c.drawText("GSTIN: ${invoice.clientGstin}", 32f, y, bodyPaint)
            y += 14f
        }
        if (invoice.clientAddress.isNotBlank()) {
            c.drawText(invoice.clientAddress.take(80), 32f, y, mutedPaint)
            y += 16f
        }

        // Table Header
        y += 14f
        val thPaint = Paint().apply { color = Color.parseColor("#E6F7F9"); style = Paint.Style.FILL }
        c.drawRect(32f, y - 14f, 563f, y + 8f, thPaint)
        c.drawText("Item", 40f, y, boldPaint)
        c.drawText("HSN", 270f, y, boldPaint)
        c.drawText("Qty", 335f, y, boldPaint)
        c.drawText("Rate", 385f, y, boldPaint)
        c.drawText("GST%", 450f, y, boldPaint)
        c.drawText("Total", 505f, y, boldPaint)
        y += 22f

        invoice.items.forEach { item ->
            val lineTotal = item.qty * item.rate * (1.0 + item.gstPercent / 100.0)
            c.drawText(item.name.take(32), 40f, y, bodyPaint)
            c.drawText(item.hsnSac.take(8), 270f, y, mutedPaint)
            c.drawText(item.qty.toString(), 335f, y, bodyPaint)
            c.drawText(inr(item.rate), 385f, y, bodyPaint)
            c.drawText("${item.gstPercent.toInt()}%", 450f, y, mutedPaint)
            c.drawText(inr(lineTotal), 505f, y, boldPaint)
            y += 10f
            c.drawLine(32f, y, 563f, y, linePaint)
            y += 16f
        }

        y += 12f
        c.drawText("Subtotal: ${inr(invoice.subtotal)}", 385f, y, bodyPaint); y += 16f
        if (invoice.discountAmount > 0) {
            c.drawText("Discount: -${inr(invoice.discountAmount)}", 385f, y, bodyPaint); y += 16f
        }
        if (invoice.gstMode == "inter") {
            c.drawText("IGST: ${inr(invoice.igstTotal)}", 385f, y, bodyPaint); y += 16f
        } else {
            c.drawText("CGST: ${inr(invoice.cgstTotal)}", 385f, y, bodyPaint); y += 16f
            c.drawText("SGST: ${inr(invoice.sgstTotal)}", 385f, y, bodyPaint); y += 16f
        }
        if (invoice.shippingAmount > 0) {
            c.drawText("Shipping: ${inr(invoice.shippingAmount)}", 385f, y, bodyPaint); y += 16f
        }
        y += 6f
        c.drawLine(380f, y - 10f, 563f, y - 10f, primaryPaint)
        boldPaint.textSize = 15f
        boldPaint.color = Color.parseColor("#0A6874")
        c.drawText("Grand Total: ${inr(invoice.grandTotal)}", 385f, y + 6f, boldPaint)

        y += 38f
        if (settings.bankName.isNotBlank() || settings.upiId.isNotBlank()) {
            c.drawText("Payment Details", 32f, y, boldPaint); y += 15f
            if (settings.upiId.isNotBlank()) {
                c.drawText("UPI ID: ${settings.upiId}", 32f, y, bodyPaint); y += 14f
            }
            if (settings.bankName.isNotBlank()) {
                c.drawText("Bank: ${settings.bankName} | A/C: ${settings.accountNo} | IFSC: ${settings.ifsc}", 32f, y, mutedPaint)
            }
        }

        doc.finishPage(page)
        val outFile = File(context.cacheDir, "${invoice.invoiceNumber.replace("/", "_")}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share Invoice PDF"))
        outFile
    }

    fun exportAndShareTextFile(
        context: Context,
        fileName: String,
        mimeType: String,
        content: String
    ): Result<File> = runCatching {
        val outFile = File(context.cacheDir, fileName)
        outFile.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Export $fileName"))
        outFile
    }
}
