package com.daviapps.launchernodopamin.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daviapps.launchernodopamin.data.InstalledAppsRepository
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
                isRedZoneSelectionVisible = false
            )
        }
    }

    fun hideSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = false,
                isRedZoneSelectionVisible = false
            )
        }
    }

    fun showRedZoneSelection() {
        _uiState.update { currentState ->
            currentState.copy(
                isSettingsVisible = true,
                isRedZoneSelectionVisible = true,
                isAppListVisible = false
            )
        }
    }

    fun hideRedZoneSelection() {
        _uiState.update { currentState ->
            currentState.copy(isRedZoneSelectionVisible = false)
        }
    }

    fun toggleRedZoneApp(packageName: String) {
        _uiState.update { currentState ->
            val updatedPackages = currentState.redZonePackageNames.toMutableSet().apply {
                if (contains(packageName)) {
                    remove(packageName)
                } else {
                    add(packageName)
                }
            }.toSet()

            redZoneRepository.saveSelectedPackageNames(updatedPackages)

            currentState.copy(redZonePackageNames = updatedPackages)
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

    init {
        loadRedZoneSelection()
        loadApps()
        startClock()
    }

    private fun loadRedZoneSelection() {
        _uiState.update { currentState ->
            currentState.copy(
                redZonePackageNames = redZoneRepository.getSelectedPackageNames()
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
                            )
                        ) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
