package com.gotlibsh.still.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import java.util.Locale

/** Opening a WhatsApp chat with one phone number. */
object WhatsApp {

    private val PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * Digits WhatsApp expects: country code + number, no "+" or leading zeros,
     * e.g. "+972 54-123-4567" -> "972541234567". Pure, for tests.
     */
    fun digits(international: String): String {
        val d = international.filter(Char::isDigit)
        return if (international.trim().startsWith("00")) d.removePrefix("00") else d
    }

    /** Turns a contact's number (maybe local, like "054-1234567") into international form. */
    fun toInternational(context: Context, number: String): String {
        val trimmed = number.trim()
        if (trimmed.startsWith("+") || trimmed.startsWith("00")) return trimmed
        val tm = context.getSystemService(TelephonyManager::class.java)
        val country = (tm?.simCountryIso?.takeIf { it.isNotBlank() } ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country).uppercase(Locale.ROOT)
        return PhoneNumberUtils.formatNumberToE164(trimmed, country) ?: trimmed
    }

    /** Opens the chat in WhatsApp (or WhatsApp Business). Returns false if neither is installed. */
    fun openChat(context: Context, phoneDigits: String): Boolean {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneDigits")
        for (pkg in PACKAGES) {
            val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // not installed, try the next one
            }
        }
        return false
    }
}
