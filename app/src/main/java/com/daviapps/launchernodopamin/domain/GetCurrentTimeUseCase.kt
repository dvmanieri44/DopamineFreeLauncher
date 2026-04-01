package com.daviapps.launchernodopamin.domain

class GetCurrentTimeUseCase {
    fun execute(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss")
        return sdf.format(java.util.Date())
    }
}