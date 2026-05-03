package com.daviapps.launchernodopamin.ui.screens

import com.daviapps.launchernodopamin.data.LaunchableApp
import com.daviapps.launchernodopamin.data.RedZoneAppConfig
import com.daviapps.launchernodopamin.data.RedZoneEnforcementState

data class HomeUiState(
    val currentTime: String = "--:--",
    val currentDate: String = "",
    val apps: List<LaunchableApp> = emptyList(),
    val isLoadingApps: Boolean = true,
    val isAppListVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val isRedZoneSelectionVisible: Boolean = false,
    val isAddictiveUsageVisible: Boolean = false,
    val redZonePackageNames: Set<String> = emptySet(),
    val redZoneConfigs: Map<String, RedZoneAppConfig> = emptyMap(),
    val redZoneEnforcementState: RedZoneEnforcementState = RedZoneEnforcementState(),
    val isRedZoneLimitDialogVisible: Boolean = false,
    val redZoneDialogPackageName: String? = null,
    val redZoneDialogAppLabel: String? = null,
    val redZoneDialogSelectedLimitMinutes: Int? = null
)
