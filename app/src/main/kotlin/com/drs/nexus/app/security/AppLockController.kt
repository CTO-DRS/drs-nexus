package com.drs.nexus.app.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.drs.nexus.core.util.AppLockPolicy

/**
 * Android-side wiring of the biometric app lock. All decisions (should we
 * lock, when to relock) come from the pure [AppLockPolicy]; this class only
 * performs hardware checks and drives the system BiometricPrompt.
 *
 * Requires the host activity to be a FragmentActivity — androidx.biometric
 * refuses to work with a plain ComponentActivity.
 */
class AppLockController(private val activity: FragmentActivity) {

    data class State(
        val locked: Boolean = false,
        val promptActive: Boolean = false,
        val errorText: String? = null
    )

    var state by mutableStateOf(State())
        private set

    private val deviceSecure: Boolean
        get() = (activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
            ?.isDeviceSecure ?: false

    /**
     * Called on cold start (backgroundedAt == null) and on every warm return
     * to the foreground. Updates the lock state according to the policy.
     */
    fun evaluateForeground(enabled: Boolean, backgroundedAt: Long?, nowMs: Long = System.currentTimeMillis()) {
        val mustLock = AppLockPolicy.shouldRelock(enabled, deviceSecure, backgroundedAt, nowMs)
        if (mustLock) state = State(locked = true)
    }

    /** Fully unlock — after a successful authentication. */
    fun unlock() {
        state = State(locked = false)
    }

    /** Shows the system authentication dialog. No-op when already showing. */
    fun authenticate(title: String, subtitle: String) {
        if (!state.locked || state.promptActive) return
        // Policy gap guard: without a device credential there is nothing to
        // verify against, so locking forever would brick the UI.
        if (!deviceSecure) {
            unlock()
            return
        }
        val manager = BiometricManager.from(activity)
        val combined = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val authenticators =
            if (manager.canAuthenticate(combined) == BiometricManager.BIOMETRIC_SUCCESS) combined
            else BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    state = state.copy(promptActive = false, errorText = errString.toString())
                }
            }
        )
        state = state.copy(promptActive = true, errorText = null)
        prompt.authenticate(info)
    }
}
