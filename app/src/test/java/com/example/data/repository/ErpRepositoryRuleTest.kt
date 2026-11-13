package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BusinessSettings
import com.example.data.model.ErpClient
import com.example.data.model.ErpExpense
import com.example.data.model.ErpInvoice
import com.example.data.model.ErpProduct
import com.example.data.model.InvoiceLineItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ErpRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedOwner_canSaveAndObserveSettingsProductsClientsInvoicesExpenses() = runBlocking {
        val uid = signInTestUser("owner_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = ErpRepository(firestore, auth)

        val settingsResult = withTimeout(5000L) {
            repo.saveSettings(BusinessSettings(userId = uid, businessName = "Herbal Hub"))
        }
        assertTrue(settingsResult.isSuccess)

        val prodRes = withTimeout(5000L) {
            repo.saveProduct(ErpProduct(name = "Chyawanprash", sellingPrice = 350.0, gstPercent = 12.0, stock = 25.0))
        }
        assertTrue(prodRes.isSuccess)

        val cliRes = withTimeout(5000L) {
            repo.saveClient(ErpClient(name = "Riya Patel", phone = "9876543210"))
        }
        assertTrue(cliRes.isSuccess)

        val invRes = withTimeout(5000L) {
            repo.saveInvoice(
                ErpInvoice(
                    invoiceNumber = "INV-101",
                    docType = "invoice",
                    status = "final",
                    paymentStatus = "paid",
                    paymentMode = "upi",
                    clientName = "Riya Patel",
                    dateStr = "2026-11-12",
                    grandTotal = 392.0,
                    items = listOf(InvoiceLineItem(name = "Chyawanprash", qty = 1.0, rate = 350.0, gstPercent = 12.0))
                )
            )
        }
        assertTrue(invRes.isSuccess)

        val expRes = withTimeout(5000L) {
            repo.saveExpense(
                ErpExpense(
                    title = "Packaging Boxes",
                    category = "Supplies",
                    amount = 1200.0,
                    paymentMode = "upi",
                    dateStr = "2026-11-12"
                )
            )
        }
        assertTrue(expRes.isSuccess)

        val observedInvoices = withTimeout(3000L) {
            repo.observeInvoices(uid).first { list -> list.any { it.invoiceNumber == "INV-101" } }
        }
        assertTrue(observedInvoices.isNotEmpty())
    }

    @Test
    fun unauthenticatedUser_observeInvoices_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = ErpRepository(firestore, auth)

        var caught: Exception? = null
        try {
            withTimeout(3000L) { repo.observeInvoices("some_uid_123").first() }
        } catch (e: Exception) {
            caught = e
        }
        val fsErr = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }

    @Test
    fun crossUser_observeClients_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
        val aliceRepo = ErpRepository(firestore, auth)
        withTimeout(5000L) {
            aliceRepo.saveClient(ErpClient(name = "Private Client", phone = "9999999999"))
        }

        signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
        val bobRepo = ErpRepository(firestore, auth)
        var caught: Exception? = null
        try {
            withTimeout(3000L) { bobRepo.observeClients(aliceUid).first() }
        } catch (e: Exception) {
            caught = e
        }
        val fsErr = checkNotNull(caught?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $caught"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }
}
