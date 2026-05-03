package com.daviapps.launchernodopamin.data

data class RedZoneDailyUsageRecord(
    val packageName: String,
    val usageDate: String,
    val accumulatedMillis: Long,
    val lastNotifiedLimitMinutes: Int?
)
