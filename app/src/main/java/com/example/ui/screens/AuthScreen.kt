package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.BusinessSettings
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealMint
import com.example.ui.theme.TealPrimary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "GoogleAuth"

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutFromErp(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: (BusinessSettings?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var createMode by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }

    // New Account Registration Fields
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    var businessName by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var upiId by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNo by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { onAuthSuccess(null) },
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TealDark, TealPrimary, Color(0xFFF6F8F9))))
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = "My Business ERP Logo",
                    tint = TealMint,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "My Business ERP",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Invoicing, GST, Inventory, CRM, POS & AI Insights",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !createMode,
                            onClick = { createMode = false; errorMsg = null },
                            label = { Text("Sign In") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("auth_tab_signin")
                        )
                        FilterChip(
                            selected = createMode,
                            onClick = { createMode = true; errorMsg = null },
                            label = { Text("Create Account") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("auth_tab_create")
                        )
                    }

                    if (!createMode) {
                        Text(
                            text = "Sign in to your workspace",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FeatureHighlightRow(Icons.Default.CloudDone, "Real-time cloud sync with offline cache")
                        FeatureHighlightRow(Icons.Default.Inventory2, "GST billing, inventory, POs & Daybook")
                        FeatureHighlightRow(Icons.AutoMirrored.Filled.TrendingUp, "AI business advisor & Aria voice assistant")
                        FeatureHighlightRow(Icons.Default.Security, "Google Sign-In workspace isolation")

                        if (errorMsg != null) {
                            ErrorBanner(errorMsg!!)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess(null)
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMsg = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color.White)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        OnboardingWizardContent(
                            step = step,
                            onStepChange = { step = it; errorMsg = null },
                            ownerName = ownerName, onOwnerName = { ownerName = it },
                            phone = phone, onPhone = { phone = it },
                            email = contactEmail, onEmail = { contactEmail = it },
                            businessName = businessName, onBusinessName = { businessName = it },
                            gstin = gstin, onGstin = { gstin = it },
                            pan = pan, onPan = { pan = it },
                            stateName = stateName, onStateName = { stateName = it },
                            address = address, onAddress = { address = it },
                            upiId = upiId, onUpiId = { upiId = it },
                            bankName = bankName, onBankName = { bankName = it },
                            accountNo = accountNo, onAccountNo = { accountNo = it },
                            ifsc = ifsc, onIfsc = { ifsc = it },
                            errorMsg = errorMsg,
                            isLoading = isLoading,
                            submitLabel = "Create Account with Google",
                            onSubmit = {
                                if (ownerName.isBlank() || businessName.isBlank()) {
                                    errorMsg = "Please enter your name and business name."
                                    return@OnboardingWizardContent
                                }
                                errorMsg = null
                                isLoading = true
                                val draft = BusinessSettings(
                                    ownerName = ownerName.trim(),
                                    phone = phone.trim(),
                                    email = contactEmail.trim(),
                                    businessName = businessName.trim(),
                                    gstin = gstin.trim(),
                                    pan = pan.trim(),
                                    state = stateName.trim(),
                                    address = address.trim(),
                                    upiId = upiId.trim(),
                                    bankName = bankName.trim(),
                                    accountNo = accountNo.trim(),
                                    ifsc = ifsc.trim()
                                )
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess(draft)
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMsg = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSetupScreen(
    initialName: String,
    initialEmail: String,
    onSaveProfile: (BusinessSettings) -> Unit,
    onSignOut: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var ownerName by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var businessName by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNo by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TealDark, TealPrimary, MaterialTheme.colorScheme.background)))
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Complete Account Setup", style = MaterialTheme.typography.headlineSmall)
                        Text("Personal, Business, UPI & Bank Details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onSignOut) { Text("Switch Account") }
                }

                OnboardingWizardContent(
                    step = step,
                    onStepChange = { step = it; errorMsg = null },
                    ownerName = ownerName, onOwnerName = { ownerName = it },
                    phone = phone, onPhone = { phone = it },
                    email = email, onEmail = { email = it },
                    businessName = businessName, onBusinessName = { businessName = it },
                    gstin = gstin, onGstin = { gstin = it },
                    pan = pan, onPan = { pan = it },
                    stateName = stateName, onStateName = { stateName = it },
                    address = address, onAddress = { address = it },
                    upiId = upiId, onUpiId = { upiId = it },
                    bankName = bankName, onBankName = { bankName = it },
                    accountNo = accountNo, onAccountNo = { accountNo = it },
                    ifsc = ifsc, onIfsc = { ifsc = it },
                    errorMsg = errorMsg,
                    isLoading = false,
                    submitLabel = "Finish & Open ERP",
                    onSubmit = {
                        if (ownerName.isBlank() || businessName.isBlank()) {
                            errorMsg = "Please enter your name and business name."
                            return@OnboardingWizardContent
                        }
                        onSaveProfile(
                            BusinessSettings(
                                ownerName = ownerName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                businessName = businessName.trim(),
                                gstin = gstin.trim(),
                                pan = pan.trim(),
                                state = stateName.trim(),
                                address = address.trim(),
                                upiId = upiId.trim(),
                                bankName = bankName.trim(),
                                accountNo = accountNo.trim(),
                                ifsc = ifsc.trim()
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingWizardContent(
    step: Int,
    onStepChange: (Int) -> Unit,
    ownerName: String, onOwnerName: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    email: String, onEmail: (String) -> Unit,
    businessName: String, onBusinessName: (String) -> Unit,
    gstin: String, onGstin: (String) -> Unit,
    pan: String, onPan: (String) -> Unit,
    stateName: String, onStateName: (String) -> Unit,
    address: String, onAddress: (String) -> Unit,
    upiId: String, onUpiId: (String) -> Unit,
    bankName: String, onBankName: (String) -> Unit,
    accountNo: String, onAccountNo: (String) -> Unit,
    ifsc: String, onIfsc: (String) -> Unit,
    errorMsg: String?,
    isLoading: Boolean,
    submitLabel: String,
    onSubmit: () -> Unit
) {
    val titles = listOf("1. Personal Details", "2. Business Details", "3. UPI & Bank Details")
    val icons = listOf(Icons.Default.Person, Icons.Default.Business, Icons.Default.AccountBalance)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = { (step + 1) / 3f },
            color = TealPrimary,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icons[step], contentDescription = null, tint = TealPrimary)
            Text(titles[step], style = MaterialTheme.typography.titleMedium, color = TealPrimary)
        }

        AnimatedContent(targetState = step, label = "onboarding_step") { current ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (current) {
                    0 -> {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = onOwnerName,
                            label = { Text("Full Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_owner_name")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = onPhone,
                            label = { Text("Mobile Number *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_phone")
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmail,
                            label = { Text("Email Address") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_email")
                        )
                    }
                    1 -> {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = onBusinessName,
                            label = { Text("Business / Shop Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_business_name")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = { onGstin(it.uppercase()) },
                                label = { Text("GSTIN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("setup_gstin")
                            )
                            OutlinedTextField(
                                value = pan,
                                onValueChange = { onPan(it.uppercase()) },
                                label = { Text("PAN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("setup_pan")
                            )
                        }
                        OutlinedTextField(
                            value = stateName,
                            onValueChange = onStateName,
                            label = { Text("State (for CGST/IGST)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_state")
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = onAddress,
                            label = { Text("Business Address") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_address")
                        )
                    }
                    else -> {
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = onUpiId,
                            label = { Text("UPI ID (e.g. business@okicici)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_upi_id")
                        )
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = onBankName,
                            label = { Text("Bank Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_bank_name")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = accountNo,
                                onValueChange = onAccountNo,
                                label = { Text("Account No.") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1.3f).testTag("setup_account_no")
                            )
                            OutlinedTextField(
                                value = ifsc,
                                onValueChange = { onIfsc(it.uppercase()) },
                                label = { Text("IFSC") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("setup_ifsc")
                            )
                        }
                    }
                }
            }
        }

        if (errorMsg != null) {
            ErrorBanner(errorMsg)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { onStepChange(step - 1) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Back")
                }
            }
            if (step < 2) {
                Button(
                    onClick = { onStepChange(step + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.weight(1.4f).height(50.dp).testTag("setup_next_button")
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    modifier = Modifier.weight(1.6f).height(50.dp).testTag("setup_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(submitLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun FeatureHighlightRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
