package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiInsightsRepository"

class AiInsightsRepository {
    private val model by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    suspend fun generateBusinessReport(
        businessName: String,
        periodLabel: String,
        totalRevenue: Double,
        totalExpenses: Double,
        netProfit: Double,
        invoiceCount: Int,
        unpaidAmount: Double,
        lowStockCount: Int,
        topProductsSummary: String,
        expenseBreakdown: String
    ): Result<String> {
        val prompt = """
            You are an expert Indian Chartered Accountant and ERP Business Advisor.
            Analyze this business performance data for "$businessName" ($periodLabel) and provide a clear, actionable executive report in clean plain-text sections (Executive Summary, Revenue & Margin Health, Expense Optimization, Inventory & GST Action Items, Top 3 Next-Week Recommendations).
            Data:
            - Total Revenue: ₹${"%.2f".format(totalRevenue)}
            - Total Expenses: ₹${"%.2f".format(totalExpenses)}
            - Net Profit: ₹${"%.2f".format(netProfit)}
            - Final Invoices Count: $invoiceCount
            - Unpaid Receivables: ₹${"%.2f".format(unpaidAmount)}
            - Low Stock Items: $lowStockCount
            - Top Selling Products: $topProductsSummary
            - Expense Categories: $expenseBreakdown
            Keep the response concise, insightful, and specific to these numbers.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("AI returned an empty report."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI report generation timed out", e)
            Result.failure(Exception("Report generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI report generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun askAriaAssistant(
        query: String,
        contextSummary: String
    ): Result<String> {
        val prompt = """
            You are Aria, the friendly bilingual (English/Hinglish) voice ERP assistant inside My Business ERP.
            Answer the user's question concisely in 1-3 short spoken-friendly sentences using ONLY the live ERP snapshot below.
            Live ERP Snapshot:
            $contextSummary
            User Query: "$query"
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Aria could not generate a response."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Aria query timed out", e)
            Result.failure(Exception("Aria timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Aria query failed", e)
            Result.failure(e)
        }
    }
}
