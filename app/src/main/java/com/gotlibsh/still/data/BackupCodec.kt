package com.gotlibsh.still.data

import org.json.JSONObject

/**
 * Turns all saved settings into one small JSON file and back, for "Back up settings" in
 * settings. Values keep their type (text, on/off, number). Pure, so it can be unit tested.
 */
object BackupCodec {
    private const val APP = "still"
    private const val VERSION = 1

    fun encode(values: Map<String, Any>): String {
        val settings = JSONObject()
        values.toSortedMap().forEach { (key, value) ->
            val type = when (value) {
                is String -> "text"
                is Boolean -> "flag"
                is Int -> "number"
                else -> return@forEach // nothing else is stored today
            }
            settings.put(key, JSONObject().put("type", type).put("value", value))
        }
        return JSONObject().put("app", APP).put("version", VERSION).put("settings", settings).toString(2)
    }

    /** Null if [json] isn't a Still backup. Entries it doesn't understand are skipped. */
    fun decode(json: String): Map<String, Any>? = try {
        val root = JSONObject(json)
        if (root.optString("app") != APP) {
            null
        } else {
            val settings = root.getJSONObject("settings")
            buildMap {
                for (key in settings.keys()) {
                    val entry = settings.optJSONObject(key) ?: continue
                    when (entry.optString("type")) {
                        "text" -> put(key, entry.optString("value"))
                        "flag" -> put(key, entry.optBoolean("value"))
                        "number" -> put(key, entry.optInt("value"))
                    }
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}
