package com.daviapps.launchernodopamin.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class RedZoneDailyUsageRepository(
    context: Context,
    private val currentDateProvider: () -> String = ::currentRedZoneUsageDate
) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getAccumulatedUsageMillis(packageName: String): Long {
        return getCurrentRecord(packageName).accumulatedMillis
    }

    fun addUsageMillis(
        packageName: String,
        usageMillis: Long
    ): Long {
        if (packageName.isBlank() || usageMillis <= 0L) {
            return getAccumulatedUsageMillis(packageName)
        }

        val usageDate = currentDateProvider()
        val records = getUsageRecords().toMutableMap()
        val currentRecord = records[packageName]
            ?.takeIf { record -> record.usageDate == usageDate }

        val updatedAccumulatedMillis = max(0L, currentRecord?.accumulatedMillis ?: 0L) +
            usageMillis

        records[packageName] = RedZoneDailyUsageRecord(
            packageName = packageName,
            usageDate = usageDate,
            accumulatedMillis = updatedAccumulatedMillis,
            lastNotifiedLimitMinutes = currentRecord?.lastNotifiedLimitMinutes
        )
        persistUsageRecords(records.values)

        return updatedAccumulatedMillis
    }

    fun markLimitNotified(
        packageName: String,
        limitMinutes: Int
    ) {
        if (packageName.isBlank()) {
            return
        }

        val usageDate = currentDateProvider()
        val records = getUsageRecords().toMutableMap()
        val currentRecord = records[packageName]
            ?.takeIf { record -> record.usageDate == usageDate }

        records[packageName] = RedZoneDailyUsageRecord(
            packageName = packageName,
            usageDate = usageDate,
            accumulatedMillis = currentRecord?.accumulatedMillis ?: 0L,
            lastNotifiedLimitMinutes = limitMinutes
        )
        persistUsageRecords(records.values)
    }

    private fun getCurrentRecord(packageName: String): RedZoneDailyUsageRecord {
        val usageDate = currentDateProvider()
        val savedRecord = getUsageRecords()[packageName]

        return if (savedRecord?.usageDate == usageDate) {
            savedRecord
        } else {
            RedZoneDailyUsageRecord(
                packageName = packageName,
                usageDate = usageDate,
                accumulatedMillis = 0L,
                lastNotifiedLimitMinutes = null
            )
        }
    }

    private fun getUsageRecords(): Map<String, RedZoneDailyUsageRecord> {
        val savedRecords = sharedPreferences.getString(KEY_RED_ZONE_DAILY_USAGE, null)

        if (savedRecords.isNullOrBlank()) {
            return emptyMap()
        }

        return runCatching {
            parseUsageRecords(savedRecords)
        }.getOrDefault(emptyMap())
    }

    private fun parseUsageRecords(serializedRecords: String): Map<String, RedZoneDailyUsageRecord> {
        val jsonArray = JSONArray(serializedRecords)
        val records = linkedMapOf<String, RedZoneDailyUsageRecord>()

        for (index in 0 until jsonArray.length()) {
            val recordObject = jsonArray.optJSONObject(index) ?: continue
            val packageName = recordObject.optString(KEY_PACKAGE_NAME).trim()
            val usageDate = recordObject.optString(KEY_USAGE_DATE).trim()

            if (packageName.isEmpty() || usageDate.isEmpty()) {
                continue
            }

            val lastNotifiedLimitMinutes = if (
                recordObject.has(KEY_LAST_NOTIFIED_LIMIT_MINUTES) &&
                !recordObject.isNull(KEY_LAST_NOTIFIED_LIMIT_MINUTES)
            ) {
                recordObject.optInt(KEY_LAST_NOTIFIED_LIMIT_MINUTES)
            } else {
                null
            }

            records[packageName] = RedZoneDailyUsageRecord(
                packageName = packageName,
                usageDate = usageDate,
                accumulatedMillis = max(0L, recordObject.optLong(KEY_ACCUMULATED_MILLIS)),
                lastNotifiedLimitMinutes = lastNotifiedLimitMinutes
            )
        }

        return records
    }

    private fun persistUsageRecords(records: Collection<RedZoneDailyUsageRecord>) {
        val sortedRecords = records.sortedWith(
            compareBy<RedZoneDailyUsageRecord> { record -> record.usageDate }
                .thenBy { record -> record.packageName }
        )
        val jsonArray = JSONArray()

        sortedRecords.forEach { record ->
            val jsonObject = JSONObject()
                .put(KEY_PACKAGE_NAME, record.packageName)
                .put(KEY_USAGE_DATE, record.usageDate)
                .put(KEY_ACCUMULATED_MILLIS, record.accumulatedMillis)

            if (record.lastNotifiedLimitMinutes == null) {
                jsonObject.put(KEY_LAST_NOTIFIED_LIMIT_MINUTES, JSONObject.NULL)
            } else {
                jsonObject.put(KEY_LAST_NOTIFIED_LIMIT_MINUTES, record.lastNotifiedLimitMinutes)
            }

            jsonArray.put(jsonObject)
        }

        sharedPreferences
            .edit()
            .putString(KEY_RED_ZONE_DAILY_USAGE, jsonArray.toString())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_no_dopamin_preferences"
        const val KEY_RED_ZONE_DAILY_USAGE = "red_zone_daily_usage"
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_USAGE_DATE = "usageDate"
        const val KEY_ACCUMULATED_MILLIS = "accumulatedMillis"
        const val KEY_LAST_NOTIFIED_LIMIT_MINUTES = "lastNotifiedLimitMinutes"
    }
}

private fun currentRedZoneUsageDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
