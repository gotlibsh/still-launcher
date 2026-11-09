package com.gotlibsh.still

import com.gotlibsh.still.data.WhatsApp
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppTest {
    @Test fun digitsForWhatsApp() {
        assertEquals("972541234567", WhatsApp.digits("+972 54-123-4567"))
        assertEquals("972541234567", WhatsApp.digits("00972541234567"))
        assertEquals("14155550100", WhatsApp.digits("+1 (415) 555-0100"))
    }
}
