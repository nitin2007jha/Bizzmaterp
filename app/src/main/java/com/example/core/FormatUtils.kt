package com.example.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpInvoice
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun todayStr(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentMonthStr(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    fun addDaysToDateStr(dateStr: String, days: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        runCatching { fmt.parse(dateStr)?.let { cal.time = it } }
        cal.add(Calendar.DAY_OF_YEAR, days)
        return fmt.format(cal.time)
    }

    fun addMonthsToDateStr(dateStr: String, months: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        runCatching { fmt.parse(dateStr)?.let { cal.time = it } }
        cal.add(Calendar.MONTH, months)
        return fmt.format(cal.time)
    }

    fun amountInWordsInr(amount: Double): String {
        val n = amount.coerceAtLeast(0.0).roundToLong()
        if (n == 0L) return "Rupees Zero Only"
        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        fun twoDigits(v: Int): String = if (v < 20) ones[v] else "${tens[v / 10]} ${ones[v % 10]}".trim()
        fun convert(num: Long): String = when {
            num >= 10000000 -> "${convert(num / 10000000)} Crore ${convert(num % 10000000)}".trim()
            num >= 100000 -> "${convert(num / 100000)} Lakh ${convert(num % 100000)}".trim()
            num >= 1000 -> "${convert(num / 1000)} Thousand ${convert(num % 1000)}".trim()
            num >= 100 -> "${ones[(num / 100).toInt()]} Hundred ${convert(num % 100)}".trim()
            else -> twoDigits(num.toInt())
        }
        return "Rupees ${convert(n)} Only"
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun printInvoice(
        context: Context,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        paperSize: String = "a4",
        defaultTerms: String = ""
    ): Result<Unit> = InvoicePrintHelper.printInvoice(context, invoice, settings, paperSize, defaultTerms)

    fun exportAndShareInvoicePdf(
        context: Context,
        invoice: ErpInvoice,
        settings: BusinessSettings,
        paperSize: String = "a4",
        defaultTerms: String = ""
    ): Result<File> = InvoicePrintHelper.exportAndShareInvoicePdf(context, invoice, settings, paperSize, defaultTerms)

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
