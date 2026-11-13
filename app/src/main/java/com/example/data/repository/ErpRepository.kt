package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpAppointment
import com.example.data.model.ErpBranch
import com.example.data.model.ErpClient
import com.example.data.model.ErpCoupon
import com.example.data.model.ErpEmployee
import com.example.data.model.ErpExpense
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpOnlineOrder
import com.example.data.model.ErpProduct
import com.example.data.model.ErpPurchaseOrder
import com.example.data.model.ErpRecurringInvoice
import com.example.data.model.ErpWarranty
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ErpRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T : Any> observeCollection(
        userId: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val colRef = db.collection("users").document(userId).collection(collectionName)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeSettings(userId: String): Flow<BusinessSettings> =
        observeSettingsWithExists(userId).map { it.first }

    fun observeSettingsWithExists(userId: String): Flow<Pair<BusinessSettings, Boolean>> = flow {
        val docRef = db.collection("users").document(userId).collection("config").document("main")
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    val settings = snap.toObject(BusinessSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: BusinessSettings(userId = userId)
                    settings to snap.exists()
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeInvoices(userId: String) = observeCollection(userId, "invoices", ErpInvoice::class.java)
    fun observeProducts(userId: String) = observeCollection(userId, "products", ErpProduct::class.java)
    fun observeClients(userId: String) = observeCollection(userId, "clients", ErpClient::class.java)
    fun observeExpenses(userId: String) = observeCollection(userId, "expenses", ErpExpense::class.java)
    fun observePurchaseOrders(userId: String) = observeCollection(userId, "purchase_orders", ErpPurchaseOrder::class.java)
    fun observeAppointments(userId: String) = observeCollection(userId, "appointments", ErpAppointment::class.java)
    fun observeBranches(userId: String) = observeCollection(userId, "branches", ErpBranch::class.java)
    fun observeEmployees(userId: String) = observeCollection(userId, "employees", ErpEmployee::class.java)
    fun observeRecurring(userId: String) = observeCollection(userId, "recurring", ErpRecurringInvoice::class.java)
    fun observeWarranties(userId: String) = observeCollection(userId, "warranties", ErpWarranty::class.java)
    fun observeCoupons(userId: String) = observeCollection(userId, "coupons", ErpCoupon::class.java)
    fun observeOnlineOrders(userId: String) = observeCollection(userId, "online_orders", ErpOnlineOrder::class.java)

    suspend fun saveSettings(settings: BusinessSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("config").document("main")
        val existing = docRef.get().await()
        val isCreate = !existing.exists()
        val existingCreatedAt = existing.getTimestamp("createdAt")
        val payload = settings.copy(userId = uid, createdAt = existingCreatedAt).toFirestoreMap(isCreate)
        docRef.set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/config/main")
    }

    suspend fun saveInvoice(invoice: ErpInvoice, incrementCounter: Boolean = false): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = invoice.id.isBlank() || invoice.createdAt == null
        val docId = invoice.id.ifBlank { "inv_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("invoices").document(docId)
        docRef.set(invoice.copy(userId = uid).toFirestoreMap(isCreate)).await()
        if (isCreate && incrementCounter && invoice.status == "final") {
            val cfgRef = db.collection("users").document(uid).collection("config").document("main")
            val cfgSnap = cfgRef.get().await()
            if (cfgSnap.exists()) {
                val cur = cfgSnap.getLong("nextInvoiceNumber") ?: 1L
                val existingSettings = cfgSnap.toObject(BusinessSettings::class.java) ?: BusinessSettings(userId = uid)
                cfgRef.set(existingSettings.copy(userId = uid, nextInvoiceNumber = cur + 1L).toFirestoreMap(false)).await()
            }
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/invoices")
    }

    suspend fun saveProduct(product: ErpProduct): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = product.id.isBlank() || product.createdAt == null
        val docId = product.id.ifBlank { "prd_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("products").document(docId)
        docRef.set(product.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/products")
    }

    suspend fun adjustStockAtomic(deltas: List<Pair<String, Double>>): Result<List<ErpProduct>> = runCatching {
        val uid = requireUserId()
        if (deltas.isEmpty()) return@runCatching emptyList()
        val colRef = db.collection("users").document(uid).collection("products")
        val merged = deltas.groupBy({ it.first }, { it.second }).mapValues { (_, list) -> list.sum() }
        db.runTransaction { tx ->
            val updated = mutableListOf<ErpProduct>()
            val pending = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, ErpProduct>>()
            merged.forEach { (prodId, delta) ->
                if (prodId.isNotBlank() && delta != 0.0) {
                    val docRef = colRef.document(prodId)
                    val snap = tx.get(docRef)
                    val prod = snap.toObject(ErpProduct::class.java)
                    if (prod != null && !prod.isService && !prod.isDeleted) {
                        val nextStock = (prod.stock + delta).coerceAtLeast(0.0)
                        val nextProd = prod.copy(userId = uid, stock = nextStock)
                        pending += docRef to nextProd
                        updated += nextProd
                    }
                }
            }
            pending.forEach { (docRef, nextProd) ->
                tx.set(docRef, nextProd.toFirestoreMap(isCreate = false))
            }
            updated
        }.await()
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/products")
    }

    suspend fun renameProductCategory(
        oldCategory: String,
        newCategory: String,
        products: List<ErpProduct>
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        val target = newCategory.trim().take(60).ifBlank { "General" }
        val matching = products.filter {
            !it.isDeleted && it.id.isNotBlank() && it.category.ifBlank { "General" }.equals(oldCategory.trim(), ignoreCase = true)
        }
        if (matching.isEmpty()) return@runCatching 0
        val colRef = db.collection("users").document(uid).collection("products")
        matching.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { prod ->
                val docRef = colRef.document(prod.id)
                batch.set(docRef, prod.copy(userId = uid, category = target).toFirestoreMap(isCreate = false))
            }
            batch.commit().await()
        }
        matching.size
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/products")
    }

    suspend fun saveClient(client: ErpClient): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = client.id.isBlank() || client.createdAt == null
        val docId = client.id.ifBlank { "cli_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("clients").document(docId)
        docRef.set(client.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/clients")
    }

    suspend fun saveExpense(expense: ErpExpense): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = expense.id.isBlank() || expense.createdAt == null
        val docId = expense.id.ifBlank { "exp_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("expenses").document(docId)
        docRef.set(expense.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/expenses")
    }

    suspend fun savePurchaseOrder(po: ErpPurchaseOrder): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = po.id.isBlank() || po.createdAt == null
        val docId = po.id.ifBlank { "po_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("purchase_orders").document(docId)
        docRef.set(po.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/purchase_orders")
    }

    suspend fun saveAppointment(appointment: ErpAppointment): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = appointment.id.isBlank() || appointment.createdAt == null
        val docId = appointment.id.ifBlank { "apt_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("appointments").document(docId)
        docRef.set(appointment.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/appointments")
    }

    suspend fun saveBranch(branch: ErpBranch): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = branch.id.isBlank() || branch.createdAt == null
        val docId = branch.id.ifBlank { "br_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("branches").document(docId)
        docRef.set(branch.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/branches")
    }

    suspend fun saveEmployee(employee: ErpEmployee): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = employee.id.isBlank() || employee.createdAt == null
        val docId = employee.id.ifBlank { "emp_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("employees").document(docId)
        docRef.set(employee.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/employees")
    }

    suspend fun saveRecurring(item: ErpRecurringInvoice): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = item.id.isBlank() || item.createdAt == null
        val docId = item.id.ifBlank { "rec_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("recurring").document(docId)
        docRef.set(item.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/recurring")
    }

    suspend fun saveWarranty(warranty: ErpWarranty): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = warranty.id.isBlank() || warranty.createdAt == null
        val docId = warranty.id.ifBlank { "war_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("warranties").document(docId)
        docRef.set(warranty.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/warranties")
    }

    suspend fun saveCoupon(coupon: ErpCoupon): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = coupon.id.isBlank() || coupon.createdAt == null
        val docId = coupon.id.ifBlank { "cp_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("coupons").document(docId)
        docRef.set(coupon.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/coupons")
    }

    suspend fun saveOnlineOrder(order: ErpOnlineOrder): Result<String> = runCatching {
        val uid = requireUserId()
        val isCreate = order.id.isBlank() || order.createdAt == null
        val docId = order.id.ifBlank { "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("users").document(uid).collection("online_orders").document(docId)
        docRef.set(order.copy(userId = uid).toFirestoreMap(isCreate)).await()
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/online_orders")
    }

    suspend fun deleteDocument(collectionName: String, docId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection(collectionName).document(docId)
        docRef.delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "users/${auth.currentUser?.uid}/$collectionName/$docId")
    }
}
