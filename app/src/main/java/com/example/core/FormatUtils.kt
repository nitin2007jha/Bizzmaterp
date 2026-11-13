package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    private val gstinStateMap = mapOf(
        "01" to "Jammu & Kashmir", "02" to "Himachal Pradesh", "03" to "Punjab",
        "04" to "Chandigarh", "05" to "Uttarakhand", "06" to "Haryana",
        "07" to "Delhi", "08" to "Rajasthan", "09" to "Uttar Pradesh",
        "10" to "Bihar", "11" to "Sikkim", "12" to "Arunachal Pradesh",
        "13" to "Nagaland", "14" to "Manipur", "15" to "Mizoram",
        "16" to "Tripura", "17" to "Meghalaya", "18" to "Assam",
        "19" to "West Bengal", "20" to "Jharkhand", "21" to "Odisha",
        "22" to "Chhattisgarh", "23" to "Madhya Pradesh", "24" to "Gujarat",
        "26" to "Dadra & Nagar Haveli and Daman & Diu", "27" to "Maharashtra",
        "29" to "Karnataka", "30" to "Goa", "31" to "Lakshadweep",
        "32" to "Kerala", "33" to "Tamil Nadu", "34" to "Puducherry",
        "35" to "Andaman & Nicobar", "36" to "Telangana", "37" to "Andhra Pradesh",
        "38" to "Ladakh"
    )

    fun inr(amount: Double, masked: Boolean = false): String {
        if (masked) return "₹ •••••"
        val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        return try {
            currencyFmt.format(safe)
        } catch (_: Exception) {
            "₹${safe.roundToLong()}"
        }
    }

    fun decodeGstinState(gstin: String): String? {
        val clean = gstin.trim().uppercase()
        if (clean.length < 2) return null
        return gstinStateMap[clean.substring(0, 2)]
    }

    fun isValidGstin(gstin: String): Boolean {
        val clean = gstin.trim().uppercase()
        return Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$").matches(clean)
    }

    fun greetingByHour(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Late Shift"
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

    fun buildUpiUri(upiId: String, payeeName: String, amount: Double, note: String): String {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", payeeName.ifBlank { "My Business" }.trim())
            .appendQueryParameter("cu", "INR")
        if (amount > 0.0) {
            builder.appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
        }
        if (note.isNotBlank()) {
            builder.appendQueryParameter("tn", note.trim().take(60))
        }
        return builder.build().toString()
    }

    fun launchUpiPayment(
        context: Context,
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String
    ): Result<Unit> = runCatching {
        require(upiId.isNotBlank()) { "Add your UPI ID in Settings first." }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buildUpiUri(upiId, payeeName, amount, note)))
        context.startActivity(Intent.createChooser(intent, "Pay with UPI"))
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
