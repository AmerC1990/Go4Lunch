package com.amercosovic.go4lunch.utility

import java.util.*

object Translate {
    // translate to spanish
    fun translate(spanish: String, english: String): String {
        val language = Locale.getDefault().displayLanguage

        return if (language.toString() == "español") {
            spanish
        } else {
            english
        }
    }
}