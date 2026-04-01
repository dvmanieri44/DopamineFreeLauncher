package com.daviapps.launchernodopamin.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daviapps.launchernodopamin.domain.GetCurrentTimeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val getCurrentTimeUseCase = GetCurrentTimeUseCase()

    private val _time = MutableStateFlow("")
    val time: StateFlow<String> = _time

    init {
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                _time.value = getCurrentTimeUseCase.execute()
                delay(1000)
            }
        }
    }
}