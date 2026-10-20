package com.drs.nexus.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test
    fun `lock engages only when enabled and device is secure`() {
        assertTrue(AppLockPolicy.shouldLock(appLockEnabled = true, deviceSecure = true))
        assertFalse(AppLockPolicy.shouldLock(appLockEnabled = true, deviceSecure = false))
        assertFalse(AppLockPolicy.shouldLock(appLockEnabled = false, deviceSecure = true))
        assertFalse(AppLockPolicy.shouldLock(appLockEnabled = false, deviceSecure = false))
    }

    @Test
    fun `fresh process start always relocks when the lock can engage`() {
        val relock = AppLockPolicy.shouldRelock(
            appLockEnabled = true, deviceSecure = true,
            backgroundedAtMs = null, nowMs = 1_000, graceMs = 0
        )
        assertTrue(relock)
    }

    @Test
    fun `zero grace relocks on every foreground return`() {
        val relock = AppLockPolicy.shouldRelock(
            appLockEnabled = true, deviceSecure = true,
            backgroundedAtMs = 900, nowMs = 1_000, graceMs = 0
        )
        assertTrue(relock)
    }

    @Test
    fun `grace period keeps the app unlocked within the window`() {
        val withinGrace = AppLockPolicy.shouldRelock(
            appLockEnabled = true, deviceSecure = true,
            backgroundedAtMs = 1_000, nowMs = 1_000 + 30_000, graceMs = 60_000
        )
        assertFalse(withinGrace)

        val pastGrace = AppLockPolicy.shouldRelock(
            appLockEnabled = true, deviceSecure = true,
            backgroundedAtMs = 1_000, nowMs = 1_000 + 60_000, graceMs = 60_000
        )
        assertTrue(pastGrace)
    }

    @Test
    fun `disabled or insecure device never relocks`() {
        assertFalse(
            AppLockPolicy.shouldRelock(false, true, backgroundedAtMs = null, nowMs = 0)
        )
        assertFalse(
            AppLockPolicy.shouldRelock(true, false, backgroundedAtMs = null, nowMs = 0)
        )
    }
}
