package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    fun formatE164Phone(countryCode: String, rawDigits: String): String {
        val cleanCode = if (countryCode.startsWith("+")) countryCode.trim() else "+${countryCode.trim()}"
        val cleanDigits = rawDigits.filter { it.isDigit() }
        return "$cleanCode$cleanDigits"
    }

    fun formatDisplayPhone(countryCode: String, rawDigits: String): String {
        val cleanCode = if (countryCode.startsWith("+")) countryCode.trim() else "+${countryCode.trim()}"
        val cleanDigits = rawDigits.filter { it.isDigit() }
        return "$cleanCode $cleanDigits"
    }

    fun isValidPhoneDigits(rawDigits: String): Boolean {
        val clean = rawDigits.filter { it.isDigit() }
        return clean.length in 7..15
    }

    fun normalizePhoneDigits(countryCode: String, rawInput: String): String {
        val trimmed = rawInput.trim()
        val cleanCode = if (countryCode.startsWith("+")) countryCode.trim() else "+${countryCode.trim()}"
        val codeDigits = cleanCode.filter { it.isDigit() }

        val withoutExplicitCode = if (trimmed.startsWith(cleanCode)) {
            trimmed.removePrefix(cleanCode)
        } else {
            trimmed
        }

        var digits = withoutExplicitCode.filter { it.isDigit() }
        if (codeDigits.isNotEmpty() && digits.startsWith(codeDigits) && digits.length >= codeDigits.length + 10) {
            digits = digits.removePrefix(codeDigits)
        } else if (digits.length == 11 && digits.startsWith("0")) {
            digits = digits.drop(1)
        }
        return digits.take(15)
    }

    fun extractPhoneParts(
        rawPhone: String,
        supportedCodes: List<String>,
        defaultCode: String = "+91"
    ): Pair<String, String> {
        val trimmed = rawPhone.trim()
        if (trimmed.isBlank() || trimmed.contains("@") || trimmed.any { it.isLetter() }) {
            return defaultCode to ""
        }
        val matchedCode = supportedCodes.firstOrNull { trimmed.startsWith(it) } ?: defaultCode
        val digits = normalizePhoneDigits(matchedCode, trimmed)
        return matchedCode to digits
    }

    fun isValidOtpCode(rawOtp: String): Boolean {
        val clean = rawOtp.filter { it.isDigit() }
        return clean.length == 6
    }

    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                cont.resume(task.result as T)
            } else {
                cont.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
            }
        }
    }

    fun sendPhoneVerificationCode(
        activity: Activity,
        e164Phone: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationFailed: (String) -> Unit
    ) {
        try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationCompleted(credential)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    Log.w(TAG, "Phone verification failed: ${exception.message}", exception)
                    onVerificationFailed(mapPhoneAuthError(exception))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId, token)
                }
            }

            val optionsBuilder = PhoneAuthOptions.newBuilder(Firebase.auth)
                .setPhoneNumber(e164Phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (resendToken != null) {
                optionsBuilder.setForceResendingToken(resendToken)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate phone verification", e)
            onVerificationFailed(e.localizedMessage ?: "Unable to start phone verification.")
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<String> {
        return try {
            val result = Firebase.auth.signInWithCredential(credential).awaitResult()
            val verifiedPhone = result.user?.phoneNumber ?: ""
            Result.success(verifiedPhone)
        } catch (e: Exception) {
            Log.w(TAG, "OTP credential sign-in failed: ${e.message}", e)
            Result.failure(Exception(mapPhoneAuthError(e)))
        }
    }

    suspend fun verifyOtpCode(verificationId: String, otpCode: String): Result<String> {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
        return signInWithPhoneCredential(credential)
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (accountLabel: String) -> Unit,
        onUnauthenticated: () -> Unit = {}
    ) {
        val existingUser = try {
            Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }
        if (existingUser != null) {
            val label = existingUser.phoneNumber?.takeIf { it.isNotBlank() }
                ?: existingUser.email?.takeIf { it.isNotBlank() }
                ?: existingUser.displayName?.takeIf { it.isNotBlank() }
                ?: existingUser.uid
            onAuthSuccess(label)
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
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).awaitResult()
                    val user = authResult.user
                    val label = user?.email?.takeIf { it.isNotBlank() }
                        ?: user?.displayName?.takeIf { it.isNotBlank() }
                        ?: googleIdTokenCredential.id
                    onAuthSuccess(label)
                } else {
                    onUnauthenticated()
                }
            } catch (_: NoCredentialException) {
                onUnauthenticated()
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: (accountLabel: String) -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Unable to locate Activity context for Google Sign-In")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).awaitResult()
                    val user = authResult.user
                    val label = user?.email?.takeIf { it.isNotBlank() }
                        ?: user?.displayName?.takeIf { it.isNotBlank() }
                        ?: googleIdTokenCredential.id
                    onAuthSuccess(label)
                } else {
                    onAuthError("Unexpected credential type returned by Google Sign-In")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credentials available: ${e.message}", e)
                onAuthError("No Google account found on this device. Please login with your mobile number or skip to offline ledger.")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager error", e)
                onAuthError("Google Sign-In unavailable on this device. Use mobile number login or Continue Offline.")
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    withTimeoutOrNull(2500L) {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    try {
                        Firebase.auth.signOut()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to sign out of Firebase: ${e.message}")
                    }
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onSignOutComplete()
                    }
                }
            }
        }
    }

    fun isFirebaseSignedIn(): Boolean {
        return try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
    }

    fun currentVerifiedLabel(): String? {
        return try {
            val user = Firebase.auth.currentUser ?: return null
            user.phoneNumber?.takeIf { it.isNotBlank() }
                ?: user.email?.takeIf { it.isNotBlank() }
                ?: user.displayName?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun mapPhoneAuthError(exception: Exception): String {
        val code = (exception as? FirebaseAuthException)?.errorCode
        val rawMsg = exception.message.orEmpty()
        return when {
            rawMsg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ->
                "Firebase Phone SMS requires the Blaze (pay-as-you-go) billing plan on project shayari-146fc (or a Firebase Console test phone number). Upgrade billing in Firebase Console or use Sign in with Google below."
            code == "ERROR_INVALID_VERIFICATION_CODE" ->
                "Incorrect OTP code. Please check the SMS and try again."
            code == "ERROR_SESSION_EXPIRED" ->
                "OTP has expired. Please tap Resend OTP to get a new code."
            code == "ERROR_OPERATION_NOT_ALLOWED" ||
                rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Phone Sign-In is not enabled yet in Firebase Console (Authentication -> Sign-in method -> Phone) for project shayari-146fc. Enable it there or use Sign in with Google below."
            rawMsg.contains("APP_NOT_AUTHORIZED", ignoreCase = true) ||
                rawMsg.contains("INVALID_APP_CREDENTIAL", ignoreCase = true) ->
                "App signature / Play Integrity check failed for this build. Verify SHA-1/SHA-256 in Firebase Console or use Sign in with Google below."
            exception is FirebaseTooManyRequestsException || code == "ERROR_TOO_MANY_REQUESTS" ->
                "Too many SMS requests from this device/project. Please wait a bit or use Sign in with Google."
            exception is FirebaseAuthInvalidCredentialsException ->
                exception.localizedMessage ?: "Invalid phone number or verification credentials."
            else ->
                exception.localizedMessage ?: "Phone verification failed. Please try again."
        }
    }
}
