package com.daviapps.launchernodopamin.data

import kotlin.math.max

object RedZoneDailyUsagePolicy {
    private const val MILLIS_PER_MINUTE = 60_000L

    fun sessionElapsedMillis(
        sessionStartElapsedMillis: Long?,
        nowElapsedMillis: Long
    ): Long {
        return sessionStartElapsedMillis
            ?.let { startMillis -> max(0L, nowElapsedMillis - startMillis) }
            ?: 0L
    }

    fun currentTotalMillis(
        accumulatedMillis: Long,
        sessionStartElapsedMillis: Long?,
        nowElapsedMillis: Long
    ): Long {
        return max(0L, accumulatedMillis) +
            sessionElapsedMillis(sessionStartElapsedMillis, nowElapsedMillis)
    }

    fun isLimitReached(
        accumulatedMillis: Long,
        sessionStartElapsedMillis: Long?,
        nowElapsedMillis: Long,
        limitMinutes: Int
    ): Boolean {
        if (limitMinutes <= 0) {
            return false
        }

        return currentTotalMillis(
            accumulatedMillis = accumulatedMillis,
            sessionStartElapsedMillis = sessionStartElapsedMillis,
            nowElapsedMillis = nowElapsedMillis
        ) >= limitMinutes * MILLIS_PER_MINUTE
    }
}
