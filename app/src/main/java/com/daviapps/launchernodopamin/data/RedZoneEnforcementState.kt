package com.daviapps.launchernodopamin.data

data class RedZoneEnforcementState(
    val isUsageAccessGranted: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false
) {
    val isMonitoringActive: Boolean
        get() = isUsageAccessGranted && isAccessibilityServiceEnabled
}
