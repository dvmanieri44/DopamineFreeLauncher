package com.daviapps.launchernodopamin.data

import android.accessibilityservice.AccessibilityService
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.daviapps.launchernodopamin.accessibility.RedZoneAccessibilityService

class RedZoneEnforcementRepository(
    private val context: Context
) {
    fun getEnforcementState(): RedZoneEnforcementState {
        return RedZoneEnforcementState(
            isUsageAccessGranted = isUsageAccessGranted(),
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled()
        )
    }

    fun isUsageAccessGranted(): Boolean {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                context.applicationInfo.uid,
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1

        if (!accessibilityEnabled) {
            return false
        }

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        val targetComponent = ComponentName(
            context,
            RedZoneAccessibilityService::class.java
        ).flattenToString()

        return enabledServices
            .split(':')
            .any { serviceName ->
                serviceName.equals(targetComponent, ignoreCase = true)
            }
    }

    fun getForegroundPackageName(lookbackMillis: Long = 15_000L): String? {
        if (!isUsageAccessGranted()) {
            return null
        }

        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - lookbackMillis
        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundPackage: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPackage = event.packageName
            }
        }

        return lastForegroundPackage
    }
}
