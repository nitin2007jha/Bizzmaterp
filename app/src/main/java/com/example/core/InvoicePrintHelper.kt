package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpInvoice
import com.example.data.model.InvoiceTemplates
import java.io.File
import java.io.FileOutputStream

object InvoicePrintHelper {

    fun printInvoice(
        context: Context,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        paperSize: String = "a4",
        defaultTerms: String = ""
    ): Result<Unit> = runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: error("Print service unavailable on this device")
        val jobName = "${settings.businessName.ifBlank { "ERP" }} - ${invoice.invoiceNumber}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("${invoice.invoiceNumber.replace("/", "_")}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val doc = buildInvoicePdf(invoice, settings, paperSize, defaultTerms)
                try {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage ?: "Print failed")
                } finally {
                    doc.close()
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(
                if (paperSize == "thermal") PrintAttributes.MediaSize.ISO_A6
                else PrintAttributes.MediaSize.ISO_A4
            )
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        printManager.print(jobName, adapter, attrs)
    }

    fun exportAndShareInvoicePdf(
        context: Context,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        paperSize: String = "a4",
        defaultTerms: String = ""
    ): Result<File> = runCatching {
        val doc = buildInvoicePdf(invoice, settings, paperSize, defaultTerms)
        val outFile = File(context.cacheDir, "${invoice.invoiceNumber.replace("/", "_")}.pdf")
        try {
            FileOutputStream(outFile).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
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

    fun buildInvoicePdf(
        invoice: ErpInvoice,
        settings: BusinessSettings,
        paperSize: String = "a4",
        defaultTerms: String = ""
    ): PdfDocument {
        val doc = PdfDocument()
        val isThermal = paperSize == "thermal"
        val pageW = if (isThermal) 300 else 595
        val pageH = if (isThermal) 760 else 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = doc.startPage(pageInfo)
        val c: Canvas = page.canvas

        val tpl = InvoiceTemplates.byId(invoice.templateId.ifBlank { settings.invoiceTemplate })
        val primaryInt = tpl.primaryColor.toArgb()
        val accentInt = tpl.accentColor.toArgb()

        if (isThermal) {
            drawThermalReceipt(c, pageW.toFloat(), invoice, settings, primaryInt, defaultTerms)
        } else {
            drawA4Invoice(c, pageW.toFloat(), invoice, settings, primaryInt, accentInt, defaultTerms)
        }

        doc.finishPage(page)
        return doc
    }

    private fun drawA4Invoice(
        c: Canvas,
        pageW: Float,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        primaryInt: Int,
        accentInt: Int,
        defaultTerms: String
    ) {
        val headerPaint = Paint().apply { color = primaryInt; style = Paint.Style.FILL }
        val accentPaint = Paint().apply { color = accentInt; style = Paint.Style.FILL }
        val titlePaint = Paint().apply { color = Color.WHITE; textSize = 20f; isFakeBoldText = true; isAntiAlias = true }
        val subPaint = Paint().apply { color = Color.parseColor("#E2F8F5"); textSize = 10.5f; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = Color.parseColor("#0B1B22"); textSize = 10.5f; isAntiAlias = true }
        val boldPaint = Paint().apply { color = Color.parseColor("#0B1B22"); textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val mutedPaint = Paint().apply { color = Color.parseColor("#5B6B72"); textSize = 9.5f; isAntiAlias = true }
        val linePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }

        // Header Band
        c.drawRect(0f, 0f, pageW, 108f, headerPaint)
        c.drawRect(0f, 104f, pageW, 108f, accentPaint)
        c.drawText(settings.businessName.ifBlank { "My Business" }.take(34), 32f, 36f, titlePaint)

        val meta1 = listOfNotNull(
            settings.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN: $it" },
            settings.pan.takeIf { it.isNotBlank() }?.let { "PAN: $it" }
        ).joinToString("  |  ")
        val meta2 = listOfNotNull(
            settings.phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
            settings.email.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")
        if (meta1.isNotBlank()) c.drawText(meta1, 32f, 56f, subPaint)
        if (meta2.isNotBlank()) c.drawText(meta2, 32f, 72f, subPaint)
        val addrLine = listOfNotNull(settings.address.takeIf { it.isNotBlank() }, settings.state.takeIf { it.isNotBlank() }).joinToString(", ")
        if (addrLine.isNotBlank()) c.drawText(addrLine.take(72), 32f, 88f, subPaint)

        val docTitle = when (invoice.docType) {
            "quotation" -> "QUOTATION"
            "proforma" -> "PROFORMA INVOICE"
            "purchase" -> "PURCHASE BILL"
            "credit_note" -> "CREDIT NOTE"
            "challan" -> "DELIVERY CHALLAN"
            else -> "TAX INVOICE"
        }
        val rightTitlePaint = Paint(titlePaint).apply { textSize = 14f }
        c.drawText(docTitle, 405f, 36f, rightTitlePaint)
        c.drawText("#${invoice.invoiceNumber}", 405f, 56f, subPaint)
        c.drawText("Date: ${invoice.dateStr}", 405f, 72f, subPaint)
        if (invoice.dueDateStr.isNotBlank()) {
            c.drawText("Due: ${invoice.dueDateStr}", 405f, 88f, subPaint)
        }

        // Bill To + Status Box
        var y = 132f
        c.drawText("BILL TO", 32f, y, mutedPaint)
        c.drawText("PAYMENT & TAX MODE", 355f, y, mutedPaint)
        y += 16f
        c.drawText(invoice.clientName.ifBlank { "Walk-in Customer" }.take(38), 32f, y, boldPaint)
        c.drawText("Mode: ${invoice.paymentMode.uppercase()} • ${invoice.paymentStatus.uppercase()}", 355f, y, bodyPaint)
        y += 14f
        if (invoice.clientPhone.isNotBlank()) {
            c.drawText("Ph: ${invoice.clientPhone}", 32f, y, bodyPaint)
        }
        c.drawText(if (invoice.gstMode == "inter") "Supply: Inter-State (IGST)" else "Supply: Intra-State (CGST+SGST)", 355f, y, bodyPaint)
        y += 14f
        if (invoice.clientGstin.isNotBlank()) {
            c.drawText("GSTIN: ${invoice.clientGstin}", 32f, y, bodyPaint)
        }
        if (invoice.reverseCharge) {
            c.drawText("Reverse Charge: YES", 355f, y, boldPaint)
        }
        if (invoice.clientAddress.isNotBlank()) {
            y += 14f
            c.drawText(invoice.clientAddress.take(78), 32f, y, mutedPaint)
        }

        // Table Header
        y += 24f
        val thBg = Paint().apply { color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL }
        c.drawRect(32f, y - 14f, 563f, y + 8f, thBg)
        c.drawText("Item / Description", 38f, y, boldPaint)
        c.drawText("HSN", 248f, y, boldPaint)
        c.drawText("Qty", 304f, y, boldPaint)
        c.drawText("Rate", 346f, y, boldPaint)
        c.drawText("GST", 418f, y, boldPaint)
        c.drawText("Tax ₹", 456f, y, boldPaint)
        c.drawText("Total", 510f, y, boldPaint)
        y += 22f

        invoice.items.take(20).forEach { item ->
            val gross = item.qty * item.rate
            val taxable = if (invoice.gstInclusive && item.gstPercent > 0) gross / (1.0 + item.gstPercent / 100.0) else gross
            val taxAmt = if (invoice.gstInclusive && item.gstPercent > 0) gross - taxable else taxable * (item.gstPercent / 100.0)
            val lineTotal = taxable + taxAmt

            c.drawText(item.name.take(30), 38f, y, bodyPaint)
            c.drawText(item.hsnSac.ifBlank { "-" }.take(8), 248f, y, mutedPaint)
            c.drawText(if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString(), 304f, y, bodyPaint)
            c.drawText(FormatUtils.inr(item.rate), 346f, y, bodyPaint)
            c.drawText("${item.gstPercent.toInt()}%", 418f, y, mutedPaint)
            c.drawText(FormatUtils.inr(taxAmt), 456f, y, mutedPaint)
            c.drawText(FormatUtils.inr(lineTotal), 510f, y, boldPaint)
            y += 9f
            c.drawLine(32f, y, 563f, y, linePaint)
            y += 16f
        }

        y += 8f
        val leftYStart = y
        // Totals Right Column
        c.drawText("Taxable Value:", 365f, y, mutedPaint); c.drawText(FormatUtils.inr(invoice.subtotal), 485f, y, bodyPaint); y += 15f
        if (invoice.discountAmount > 0) {
            c.drawText("Discount:", 365f, y, mutedPaint); c.drawText("-${FormatUtils.inr(invoice.discountAmount)}", 485f, y, bodyPaint); y += 15f
        }
        if (invoice.gstMode == "inter") {
            c.drawText("IGST:", 365f, y, mutedPaint); c.drawText(FormatUtils.inr(invoice.igstTotal), 485f, y, bodyPaint); y += 15f
        } else {
            c.drawText("CGST:", 365f, y, mutedPaint); c.drawText(FormatUtils.inr(invoice.cgstTotal), 485f, y, bodyPaint); y += 15f
            c.drawText("SGST:", 365f, y, mutedPaint); c.drawText(FormatUtils.inr(invoice.sgstTotal), 485f, y, bodyPaint); y += 15f
        }
        if (invoice.shippingAmount > 0) {
            c.drawText("Shipping:", 365f, y, mutedPaint); c.drawText(FormatUtils.inr(invoice.shippingAmount), 485f, y, bodyPaint); y += 15f
        }
        if (invoice.tdsAmount > 0) {
            c.drawText("Less TDS:", 365f, y, mutedPaint); c.drawText("-${FormatUtils.inr(invoice.tdsAmount)}", 485f, y, bodyPaint); y += 15f
        }
        c.drawLine(365f, y - 4f, 563f, y - 4f, headerPaint)
        val totalPaint = Paint(boldPaint).apply { textSize = 13.5f; color = primaryInt }
        c.drawText("Grand Total:", 365f, y + 12f, totalPaint)
        c.drawText(FormatUtils.inr(invoice.grandTotal), 480f, y + 12f, totalPaint)
        y += 28f
        val bal = (invoice.grandTotal - invoice.paidAmount).coerceAtLeast(0.0)
        if (bal > 0.01) {
            c.drawText("Paid: ${FormatUtils.inr(invoice.paidAmount)} | Balance Due: ${FormatUtils.inr(bal)}", 365f, y, mutedPaint)
            y += 14f
        }

        // Left Column: Amount in words & Bank/UPI
        var ly = leftYStart
        c.drawText("Amount in Words:", 32f, ly, mutedPaint); ly += 13f
        c.drawText(FormatUtils.amountInWordsInr(invoice.grandTotal).take(54), 32f, ly, boldPaint); ly += 20f
        if (settings.upiId.isNotBlank() || settings.bankName.isNotBlank()) {
            c.drawText("Bank & UPI Payment Info", 32f, ly, boldPaint); ly += 14f
            if (settings.upiId.isNotBlank()) {
                c.drawText("UPI ID: ${settings.upiId}", 32f, ly, bodyPaint); ly += 13f
            }
            if (settings.bankName.isNotBlank()) {
                c.drawText("Bank: ${settings.bankName}", 32f, ly, mutedPaint); ly += 12f
                c.drawText("A/C: ${settings.accountNo} | IFSC: ${settings.ifsc}", 32f, ly, mutedPaint); ly += 14f
            }
        }
        val notesText = invoice.notes.ifBlank { defaultTerms }
        if (notesText.isNotBlank()) {
            ly += 4f
            c.drawText("Notes / Terms:", 32f, ly, mutedPaint); ly += 13f
            c.drawText(notesText.take(62), 32f, ly, bodyPaint)
        }

        // Footer Signatory
        val fy = maxOf(y, ly) + 32f
        val safeFy = fy.coerceAtMost(798f)
        c.drawLine(32f, safeFy - 12f, 563f, safeFy - 12f, linePaint)
        c.drawText("For ${settings.businessName.ifBlank { "Authorized Business" }}", 390f, safeFy + 4f, boldPaint)
        c.drawText("Authorized Signatory", 390f, safeFy + 18f, mutedPaint)
        c.drawText("Computer generated GST document", 32f, safeFy + 18f, mutedPaint)
    }

    private fun drawThermalReceipt(
        c: Canvas,
        pageW: Float,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        primaryInt: Int,
        defaultTerms: String
    ) {
        val centerBold = Paint().apply {
            color = primaryInt
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val centerMuted = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val body = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }
        val bold = Paint().apply { color = Color.BLACK; textSize = 9.5f; isFakeBoldText = true; isAntiAlias = true }
        val dash = Paint().apply { color = Color.GRAY; strokeWidth = 1f }

        var y = 24f
        val cx = pageW / 2f
        c.drawText(settings.businessName.ifBlank { "My Business" }.take(28), cx, y, centerBold); y += 13f
        if (settings.phone.isNotBlank()) { c.drawText("Ph: ${settings.phone}", cx, y, centerMuted); y += 11f }
        if (settings.gstin.isNotBlank()) { c.drawText("GSTIN: ${settings.gstin}", cx, y, centerMuted); y += 11f }
        c.drawLine(14f, y, pageW - 14f, y, dash); y += 14f

        c.drawText("${invoice.docType.uppercase()} #${invoice.invoiceNumber}", 14f, y, bold); y += 12f
        c.drawText("Date: ${invoice.dateStr} | ${invoice.paymentMode.uppercase()}", 14f, y, body); y += 12f
        c.drawText("Party: ${invoice.clientName.ifBlank { "Walk-in" }.take(28)}", 14f, y, body); y += 10f
        c.drawLine(14f, y, pageW - 14f, y, dash); y += 13f

        invoice.items.take(24).forEach { item ->
            val lineTotal = item.qty * item.rate * (1.0 + item.gstPercent / 100.0)
            c.drawText(item.name.take(22), 14f, y, bold)
            c.drawText(FormatUtils.inr(lineTotal), 218f, y, bold); y += 11f
            c.drawText("  ${item.qty.toInt()} x ${FormatUtils.inr(item.rate)} (GST ${item.gstPercent.toInt()}%)", 14f, y, body); y += 13f
        }
        c.drawLine(14f, y, pageW - 14f, y, dash); y += 14f
        c.drawText("Taxable: ${FormatUtils.inr(invoice.subtotal)}", 14f, y, body); y += 12f
        c.drawText("GST: ${FormatUtils.inr(invoice.cgstTotal + invoice.sgstTotal + invoice.igstTotal)}", 14f, y, body); y += 14f
        bold.textSize = 12f
        c.drawText("TOTAL: ${FormatUtils.inr(invoice.grandTotal)}", 14f, y, bold); y += 18f
        if (settings.upiId.isNotBlank()) {
            c.drawText("UPI: ${settings.upiId}", cx, y, centerMuted); y += 12f
        }
        val footer = invoice.notes.ifBlank { defaultTerms }.ifBlank { "Thank you for your business!" }
        c.drawText(footer.take(36), cx, y, centerMuted)
    }
}
