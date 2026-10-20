package com.drs.nexus.core.util

/**
 * Pure decision logic for the biometric app lock.
 * Kept framework-free so the locking rules are unit-testable; the Android
 * layer only performs the hardware checks (KeyguardManager / BiometricPrompt)
 * and asks this policy what to do.
 */
object AppLockPolicy {

    /**
     * The lock can only engage when the user turned it on AND the device has
     * a screen lock to authenticate against. Without a device credential the
     * prompt has nothing to verify, so locking would brick the app.
     */
    fun shouldLock(appLockEnabled: Boolean, deviceSecure: Boolean): Boolean =
        appLockEnabled && deviceSecure

    /**
     * Whether the app must relock when coming back to the foreground.
     *
     * @param backgroundedAtMs when the activity last left the foreground
     *        (null = never backgrounded this process, i.e. fresh start)
     * @param graceMs delay during which a backgrounded app returns unlocked;
     *        0 means every foreground return requires authentication.
     */
    fun shouldRelock(
        appLockEnabled: Boolean,
        deviceSecure: Boolean,
        backgroundedAtMs: Long?,
        nowMs: Long,
        graceMs: Long = 0L
    ): Boolean {
        if (!shouldLock(appLockEnabled, deviceSecure)) return false
        if (backgroundedAtMs == null) return true // fresh process start
        return nowMs - backgroundedAtMs >= graceMs
    }
}
