package com.daviapps.launchernodopamin.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daviapps.launchernodopamin.data.InstalledAppsRepository
import com.daviapps.launchernodopamin.data.RedZoneAppConfig
import com.daviapps.launchernodopamin.data.RedZoneEnforcementRepository
import com.daviapps.launchernodopamin.data.RedZoneRepository
import com.daviapps.launchernodopamin.domain.GetCurrentDateUseCase
import com.daviapps.launchernodopamin.domain.GetCurrentTimeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val redZoneRepository: RedZoneRepository,
    private val redZoneEnforcementRepository: RedZoneEnforcementRepository,
    private val getCurrentTimeUseCase: GetCurrentTimeUseCase = GetCurrentTimeUseCase(),
    private val getCurrentDateUseCase: GetCurrentDateUseCase = GetCurrentDateUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun showAppList() {
        _uiState.update { currentState ->
            currentState.copy(
                isAppListVisible = true,
                isSettingsVisible = false
            )
        }
    }

    fun hideAppList() {
        _uiState.update { currentState ->
            currentState.copy(isAppListVisible = false)
        }
    }

    fun showSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = true,
                isAppListVisible = false,
                isRedZoneSelectionVisible = false,
                isAddictiveUsageVisible = false,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun hideSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = false,
                isRedZoneSelectionVisible = false,
                isAddictiveUsageVisible = false,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun showRedZoneSelection() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = true,
                isRedZoneSelectionVisible = true,
                isAppListVisible = false,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun hideRedZoneSelection() {
        _uiState.update { currentState ->
            currentState.copy(
                isRedZoneSelectionVisible = false,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun showAddictiveUsage() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = true,
                isAddictiveUsageVisible = true,
                isRedZoneSelectionVisible = false,
                isAppListVisible = false,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun hideAddictiveUsage() {
        _uiState.update { currentState ->
            currentState.copy(isAddictiveUsageVisible = false)
        }
    }

    fun showRedZoneLimitDialog(
        packageName: String,
        appLabel: String
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                isRedZoneLimitDialogVisible = true,
                redZoneDialogPackageName = packageName,
                redZoneDialogAppLabel = appLabel,
                redZoneDialogSelectedLimitMinutes =
                    currentState.redZoneConfigs[packageName]?.timeLimitMinutes
            )
        }
    }

    fun hideRedZoneLimitDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun selectRedZoneLimit(limitMinutes: Int?) {
        _uiState.update { currentState ->
            currentState.copy(redZoneDialogSelectedLimitMinutes = limitMinutes)
        }
    }

    fun confirmRedZoneLimitSelection() {
        val currentState = _uiState.value
        val packageName = currentState.redZoneDialogPackageName ?: return
        val updatedConfig = RedZoneAppConfig(
            packageName = packageName,
            timeLimitMinutes = currentState.redZoneDialogSelectedLimitMinutes
        )

        redZoneRepository.saveRedZoneAppConfig(updatedConfig)

        _uiState.update { state ->
            val updatedConfigs = state.redZoneConfigs + (packageName to updatedConfig)
            state.copy(
                redZonePackageNames = updatedConfigs.keys,
                redZoneConfigs = updatedConfigs,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun removeRedZoneAppFromDialog() {
        val packageName = _uiState.value.redZoneDialogPackageName ?: return
        redZoneRepository.removeRedZoneAppConfig(packageName)

        _uiState.update { state ->
            val updatedConfigs = state.redZoneConfigs - packageName
            state.copy(
                redZonePackageNames = updatedConfigs.keys,
                redZoneConfigs = updatedConfigs,
                isRedZoneLimitDialogVisible = false,
                redZoneDialogPackageName = null,
                redZoneDialogAppLabel = null,
                redZoneDialogSelectedLimitMinutes = null
            )
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isLoadingApps = true)
            }

            val apps = runCatching {
                installedAppsRepository.getLaunchableApps()
            }.getOrDefault(emptyList())

            _uiState.update { currentState ->
                currentState.copy(
                    apps = apps,
                    isLoadingApps = false
                )
            }
        }
    }

    fun refreshEnforcementState() {
        _uiState.update { currentState ->
            currentState.copy(
                redZoneEnforcementState = redZoneEnforcementRepository.getEnforcementState()
            )
        }
    }

    init {
        loadRedZoneSelection()
        refreshEnforcementState()
        loadApps()
        startClock()
    }

    private fun loadRedZoneSelection() {
        val redZoneConfigs = redZoneRepository.getRedZoneAppConfigs()

        _uiState.update { currentState ->
            currentState.copy(
                redZonePackageNames = redZoneConfigs.keys,
                redZoneConfigs = redZoneConfigs
            )
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _uiState.update { currentState ->
                    currentState.copy(
                        currentTime = getCurrentTimeUseCase.execute(),
                        currentDate = getCurrentDateUseCase.execute()
                    )
                }
                delay(1000)
            }
        }
    }

    companion object {
        fun factory(
            context: Context,
            packageManager: PackageManager,
            selfPackageName: String
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                        return HomeViewModel(
                            installedAppsRepository = InstalledAppsRepository(
                                packageManager = packageManager,
                                selfPackageName = selfPackageName
                            ),
                            redZoneRepository = RedZoneRepository(
                                context = context.applicationContext
                            ),
                            redZoneEnforcementRepository = RedZoneEnforcementRepository(
                                context = context.applicationContext
                            )
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
