package com.example.ui

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BiometricHelper {
    suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            val executor = ContextCompat.getMainExecutor(activity)
            val biometricPrompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (!resumed) {
                            resumed = true
                            continuation.resume(false)
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        if (!resumed) {
                            resumed = true
                            continuation.resume(true)
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // Keep prompting till error or success
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
                
            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }

            biometricPrompt.authenticate(promptInfo)
        }
    }
}
