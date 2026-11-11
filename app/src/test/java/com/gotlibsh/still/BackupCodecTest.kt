package com.gotlibsh.still

import com.gotlibsh.still.data.BackupCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupCodecTest {
    @Test fun roundTripKeepsTypes() {
        val values = mapOf<String, Any>(
            "favorites" to "com.a/com.a.Main/0\ncom.b/com.b.Main/0",
            "note_text" to "milk \"and\" eggs\nline two",
            "fahrenheit" to true,
            "auto_launch" to false,
            "wallpaper_dim" to 40,
        )
        assertEquals(values, BackupCodec.decode(BackupCodec.encode(values)))
    }

    @Test fun rejectsOtherFiles() {
        assertNull(BackupCodec.decode("not json"))
        assertNull(BackupCodec.decode("""{"app":"other","settings":{}}"""))
        assertNull(BackupCodec.decode("""{"app":"still"}"""))
    }

    @Test fun skipsEntriesItDoesNotUnderstand() {
        val json = """{"app":"still","version":9,"settings":{
            "a":{"type":"text","value":"x"}, "b":{"type":"future","value":1}, "c":5}}"""
        assertEquals(mapOf<String, Any>("a" to "x"), BackupCodec.decode(json))
    }
}
