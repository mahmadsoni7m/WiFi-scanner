package com.wifiscanner.app.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    /**
     * Shows the system biometric / device-credential prompt.
     * [onUnavailable] is called when the device cannot authenticate (no hardware, nothing enrolled,
     * no screen lock) so the caller can offer an explicit fallback.
     * Cancelling or failing the prompt simply does nothing.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cancelText: String,
        onSuccess: () -> Unit,
        onUnavailable: () -> Unit
    ) {
        val authenticators =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            else BIOMETRIC_WEAK

        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onUnavailable()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply { if (authenticators and DEVICE_CREDENTIAL == 0) setNegativeButtonText(cancelText) }
            .build()
        prompt.authenticate(info)
    }
}
