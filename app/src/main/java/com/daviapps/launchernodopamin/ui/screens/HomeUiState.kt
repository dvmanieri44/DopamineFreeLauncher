package com.daviapps.launchernodopamin.ui.screens

import com.daviapps.launchernodopamin.data.LaunchableApp

data class HomeUiState(
    val currentTime: String = "--:--",
    val currentDate: String = "",
    val apps: List<LaunchableApp> = emptyList(),
    val isLoadingApps: Boolean = true,
    val isAppListVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val isRedZoneSelectionVisible: Boolean = false,
    val redZonePackageNames: Set<String> = emptySet()
)
