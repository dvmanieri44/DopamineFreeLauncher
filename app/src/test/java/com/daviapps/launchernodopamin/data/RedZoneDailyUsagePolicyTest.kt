package com.daviapps.launchernodopamin.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedZoneDailyUsagePolicyTest {
    @Test
    fun currentTotalMillis_addsStoredUsageAndCurrentSession() {
        val totalMillis = RedZoneDailyUsagePolicy.currentTotalMillis(
            accumulatedMillis = 45_000L,
            sessionStartElapsedMillis = 10_000L,
            nowElapsedMillis = 40_000L
        )

        assertEquals(75_000L, totalMillis)
    }

    @Test
    fun sessionElapsedMillis_neverReturnsNegativeDuration() {
        val elapsedMillis = RedZoneDailyUsagePolicy.sessionElapsedMillis(
            sessionStartElapsedMillis = 40_000L,
            nowElapsedMillis = 10_000L
        )

        assertEquals(0L, elapsedMillis)
    }

    @Test
    fun isLimitReached_returnsTrueWhenStoredAndCurrentSessionReachLimit() {
        val isLimitReached = RedZoneDailyUsagePolicy.isLimitReached(
            accumulatedMillis = 45_000L,
            sessionStartElapsedMillis = 10_000L,
            nowElapsedMillis = 25_000L,
            limitMinutes = 1
        )

        assertTrue(isLimitReached)
    }

    @Test
    fun isLimitReached_returnsFalseBeforeLimit() {
        val isLimitReached = RedZoneDailyUsagePolicy.isLimitReached(
            accumulatedMillis = 30_000L,
            sessionStartElapsedMillis = 10_000L,
            nowElapsedMillis = 25_000L,
            limitMinutes = 1
        )

        assertFalse(isLimitReached)
    }

    @Test
    fun isLimitReached_ignoresNonPositiveLimit() {
        val isLimitReached = RedZoneDailyUsagePolicy.isLimitReached(
            accumulatedMillis = 120_000L,
            sessionStartElapsedMillis = null,
            nowElapsedMillis = 120_000L,
            limitMinutes = 0
        )

        assertFalse(isLimitReached)
    }
}
