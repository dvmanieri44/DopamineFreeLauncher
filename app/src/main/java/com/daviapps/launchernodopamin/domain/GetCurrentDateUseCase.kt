package com.daviapps.launchernodopamin.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetCurrentDateUseCase {
    fun execute(date: Date = Date()): String {
        val locale = Locale("pt", "BR")
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", locale)
        val formattedDate = formatter.format(date)

        return formattedDate.replaceFirstChar { character ->
            if (character.isLowerCase()) {
                character.titlecase(locale)
            } else {
                character.toString()
            }
        }
    }
}
