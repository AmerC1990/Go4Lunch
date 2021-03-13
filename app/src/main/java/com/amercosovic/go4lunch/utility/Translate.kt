package com.amercosovic.go4lunch.utility

import com.amercosovic.go4lunch.R
import java.util.*

object Translate {
    // translate english to spanish
    fun translate(spanish: String, english: String): String {
        val language = Locale.getDefault().displayLanguage

        return if (language.toString() == R.string.español.toString()) {
            spanish
        } else {
            english
        }
    }
}