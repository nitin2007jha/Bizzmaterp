package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class BusinessSettings(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val businessName: String = "My Business",
    val ownerName: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val pan: String = "",
    val address: String = "",
    val state: String = "",
    val bankName: String = "",
    val accountNo: String = "",
    val ifsc: String = "",
    val upiId: String = "",
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Long = 1L,
    val invoiceTemplate: String = "modern",
    val gstInclusive: Boolean = false,
    val dailyTarget: Double = 10000.0,
    val language: String = "en",
    val pinLockEnabled: Boolean = false,
    val darkMode: String = "light",
    val storeSlug: String = "",
    val storeTagline: String = "Quality products & fast service",
    val storeEnabled: Boolean = false,
    val whatsappOrderPhone: String = "",
    val storeThemeColor: String = "#0A6874",
    val enabledModules: List<String> = listOf(
        "dashboard", "invoice", "records", "inventory", "services",
        "clients", "daybook", "expenses", "purchase-orders", "appointments",
        "branches", "employees", "gst", "ai-report", "website", "data-module"
    ),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "businessName" to businessName.ifBlank { "My Business" }.take(120),
            "ownerName" to ownerName.take(100),
            "phone" to phone.take(30),
            "email" to email.take(120),
            "gstin" to gstin.take(20),
            "pan" to pan.take(20),
            "address" to address.take(300),
            "state" to state.take(80),
            "bankName" to bankName.take(100),
            "accountNo" to accountNo.take(40),
            "ifsc" to ifsc.take(20),
            "upiId" to upiId.take(100),
            "invoicePrefix" to invoicePrefix.ifBlank { "INV" }.take(20),
            "nextInvoiceNumber" to nextInvoiceNumber.coerceAtLeast(1L),
            "invoiceTemplate" to invoiceTemplate.ifBlank { "modern" }.take(30),
            "gstInclusive" to gstInclusive,
            "dailyTarget" to dailyTarget.coerceAtLeast(0.0),
            "language" to if (language in listOf("en", "hi", "hinglish")) language else "en",
            "pinLockEnabled" to pinLockEnabled,
            "darkMode" to if (darkMode in listOf("system", "light", "dark")) darkMode else "light",
            "storeSlug" to storeSlug.take(60),
            "storeTagline" to storeTagline.take(160),
            "storeEnabled" to storeEnabled,
            "whatsappOrderPhone" to whatsappOrderPhone.take(30),
            "storeThemeColor" to storeThemeColor.take(20),
            "enabledModules" to enabledModules.take(30),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class InvoiceLineItem(
    val productId: String = "",
    val name: String = "",
    val hsnSac: String = "",
    val qty: Double = 1.0,
    val rate: Double = 0.0,
    val gstPercent: Double = 18.0,
    val isService: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "name" to name.take(120),
        "hsnSac" to hsnSac.take(20),
        "qty" to qty,
        "rate" to rate,
        "gstPercent" to gstPercent,
        "isService" to isService
    )
}

@IgnoreExtraProperties
data class ErpInvoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val docType: String = "invoice",
    val status: String = "final",
    val paymentStatus: String = "paid",
    val paymentMode: String = "upi",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientGstin: String = "",
    val clientAddress: String = "",
    val dateStr: String = "",
    val dueDateStr: String = "",
    val gstMode: String = "intra",
    val gstInclusive: Boolean = false,
    val reverseCharge: Boolean = false,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val tdsAmount: Double = 0.0,
    val shippingAmount: Double = 0.0,
    val cgstTotal: Double = 0.0,
    val sgstTotal: Double = 0.0,
    val igstTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val upiSplit: Double = 0.0,
    val cashSplit: Double = 0.0,
    val creditSplit: Double = 0.0,
    val notes: String = "",
    val templateId: String = "modern",
    val branchId: String = "",
    val isDeleted: Boolean = false,
    val items: List<InvoiceLineItem> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "invoiceNumber" to invoiceNumber.ifBlank { "INV-001" }.take(40),
            "docType" to if (docType in listOf("invoice", "quotation", "proforma", "purchase", "credit_note", "challan")) docType else "invoice",
            "status" to if (status in listOf("final", "draft", "cancelled")) status else "final",
            "paymentStatus" to if (paymentStatus in listOf("paid", "partial", "unpaid")) paymentStatus else "paid",
            "paymentMode" to if (paymentMode in listOf("upi", "cash", "credit", "split", "bank")) paymentMode else "upi",
            "clientId" to clientId.take(128),
            "clientName" to clientName.take(120),
            "clientPhone" to clientPhone.take(30),
            "clientGstin" to clientGstin.take(20),
            "clientAddress" to clientAddress.take(300),
            "dateStr" to dateStr.take(20),
            "dueDateStr" to dueDateStr.take(20),
            "gstMode" to if (gstMode in listOf("intra", "inter")) gstMode else "intra",
            "gstInclusive" to gstInclusive,
            "reverseCharge" to reverseCharge,
            "subtotal" to subtotal,
            "discountAmount" to discountAmount,
            "tdsAmount" to tdsAmount,
            "shippingAmount" to shippingAmount,
            "cgstTotal" to cgstTotal,
            "sgstTotal" to sgstTotal,
            "igstTotal" to igstTotal,
            "grandTotal" to grandTotal,
            "paidAmount" to paidAmount,
            "upiSplit" to upiSplit,
            "cashSplit" to cashSplit,
            "creditSplit" to creditSplit,
            "notes" to notes.take(500),
            "templateId" to templateId.take(30),
            "branchId" to branchId.take(128),
            "isDeleted" to isDeleted,
            "items" to items.take(50).map { it.toMap() },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val hsnSac: String = "",
    val category: String = "General",
    val isService: Boolean = false,
    val sellingPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val gstPercent: Double = 18.0,
    val stock: Double = 0.0,
    val lowStockThreshold: Double = 10.0,
    val unit: String = "pcs",
    val durationMins: Long = 30L,
    val batchNo: String = "",
    val expiryDate: String = "",
    val showInStore: Boolean = true,
    val description: String = "",
    val isDeleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name.take(120),
            "sku" to sku.take(60),
            "barcode" to barcode.take(60),
            "hsnSac" to hsnSac.take(20),
            "category" to category.take(60),
            "isService" to isService,
            "sellingPrice" to sellingPrice.coerceAtLeast(0.0),
            "costPrice" to costPrice.coerceAtLeast(0.0),
            "gstPercent" to gstPercent.coerceIn(0.0, 100.0),
            "stock" to stock,
            "lowStockThreshold" to lowStockThreshold.coerceAtLeast(0.0),
            "unit" to unit.take(20),
            "durationMins" to durationMins.coerceAtLeast(0L),
            "batchNo" to batchNo.take(40),
            "expiryDate" to expiryDate.take(20),
            "showInStore" to showInStore,
            "description" to description.take(400),
            "isDeleted" to isDeleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpClient(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val address: String = "",
    val state: String = "",
    val openingBalance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val birthday: String = "",
    val notes: String = "",
    val isDeleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name.take(120),
            "phone" to phone.take(30),
            "email" to email.take(120),
            "gstin" to gstin.take(20),
            "address" to address.take(300),
            "state" to state.take(80),
            "openingBalance" to openingBalance,
            "creditLimit" to creditLimit.coerceAtLeast(0.0),
            "birthday" to birthday.take(20),
            "notes" to notes.take(400),
            "isDeleted" to isDeleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpExpense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Operations",
    val vendor: String = "",
    val amount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val paymentMode: String = "upi",
    val dateStr: String = "",
    val notes: String = "",
    val approvalStatus: String = "approved",
    val submittedBy: String = "Owner",
    val isDeleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to title.take(120),
            "category" to category.ifBlank { "Operations" }.take(60),
            "vendor" to vendor.take(120),
            "amount" to amount.coerceAtLeast(0.0),
            "gstAmount" to gstAmount.coerceAtLeast(0.0),
            "paymentMode" to if (paymentMode in listOf("cash", "upi", "bank", "card")) paymentMode else "upi",
            "dateStr" to dateStr.take(20),
            "notes" to notes.take(400),
            "approvalStatus" to if (approvalStatus in listOf("approved", "pending", "rejected")) approvalStatus else "approved",
            "submittedBy" to submittedBy.take(100),
            "isDeleted" to isDeleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class PoLineItem(
    val productId: String = "",
    val name: String = "",
    val qty: Double = 1.0,
    val rate: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "name" to name.take(120),
        "qty" to qty,
        "rate" to rate
    )
}

@IgnoreExtraProperties
data class ErpPurchaseOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val poNumber: String = "",
    val vendorName: String = "",
    val vendorPhone: String = "",
    val dateStr: String = "",
    val status: String = "draft",
    val items: List<PoLineItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "poNumber" to poNumber.ifBlank { "PO-001" }.take(40),
            "vendorName" to vendorName.take(120),
            "vendorPhone" to vendorPhone.take(30),
            "dateStr" to dateStr.take(20),
            "status" to if (status in listOf("draft", "sent", "received")) status else "draft",
            "items" to items.take(50).map { it.toMap() },
            "totalAmount" to totalAmount.coerceAtLeast(0.0),
            "notes" to notes.take(400),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpAppointment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val serviceName: String = "",
    val dateStr: String = "",
    val timeStr: String = "",
    val amount: Double = 0.0,
    val status: String = "scheduled",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "clientName" to clientName.take(120),
            "clientPhone" to clientPhone.take(30),
            "serviceName" to serviceName.take(120),
            "dateStr" to dateStr.take(20),
            "timeStr" to timeStr.take(20),
            "amount" to amount.coerceAtLeast(0.0),
            "status" to if (status in listOf("scheduled", "completed", "cancelled")) status else "scheduled",
            "notes" to notes.take(400),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpBranch(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val manager: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name.take(120),
            "address" to address.take(300),
            "phone" to phone.take(30),
            "manager" to manager.take(100),
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpEmployee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "Staff",
    val phone: String = "",
    val email: String = "",
    val isActive: Boolean = true,
    val permissions: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "name" to name.take(120),
            "role" to role.ifBlank { "Staff" }.take(60),
            "phone" to phone.take(30),
            "email" to email.take(120),
            "isActive" to isActive,
            "permissions" to permissions.take(80),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpRecurringInvoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val frequency: String = "monthly",
    val nextRunDate: String = "",
    val items: List<InvoiceLineItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "clientName" to clientName.take(120),
            "clientPhone" to clientPhone.take(30),
            "frequency" to if (frequency in listOf("weekly", "monthly", "quarterly", "yearly")) frequency else "monthly",
            "nextRunDate" to nextRunDate.take(20),
            "items" to items.take(50).map { it.toMap() },
            "totalAmount" to totalAmount.coerceAtLeast(0.0),
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpWarranty(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "warranty",
    val clientName: String = "",
    val mobile: String = "",
    val productName: String = "",
    val serialNo: String = "",
    val startDate: String = "",
    val months: Long = 12L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "type" to if (type in listOf("warranty", "amc")) type else "warranty",
            "clientName" to clientName.take(120),
            "mobile" to mobile.take(30),
            "productName" to productName.take(120),
            "serialNo" to serialNo.take(60),
            "startDate" to startDate.take(20),
            "months" to months.coerceIn(1L, 120L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpCoupon(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val discountPercent: Double = 10.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "code" to code.uppercase().take(30),
            "discountPercent" to discountPercent.coerceIn(0.0, 100.0),
            "isActive" to isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}

@IgnoreExtraProperties
data class ErpOnlineOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val address: String = "",
    val itemsSummary: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isCreate: Boolean): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to userId,
            "customerName" to customerName.take(120),
            "customerPhone" to customerPhone.take(30),
            "address" to address.take(300),
            "itemsSummary" to itemsSummary.take(500),
            "totalAmount" to totalAmount.coerceAtLeast(0.0),
            "status" to if (status in listOf("pending", "accepted", "completed", "cancelled")) status else "pending",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            map["createdAt"] = FieldValue.serverTimestamp()
        } else if (createdAt != null) {
            map["createdAt"] = createdAt
        }
        return map
    }
}
