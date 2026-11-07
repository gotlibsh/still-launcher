package com.gotlibsh.still.data

import java.text.Normalizer

/** Search used by the app drawer: prefix matches first, then word starts, then anywhere. */
object AppSearch {

    fun <T> filter(items: List<T>, query: String, label: (T) -> String): List<T> {
        val q = normalize(query)
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item -> rank(normalize(label(item)), q)?.let { item to it } }
            .sortedBy { it.second }
            .map { it.first }
    }

    private fun rank(label: String, q: String): Int? {
        val compact = label.replace(" ", "")
        return when {
            label.startsWith(q) -> 0
            label.split(' ').any { it.startsWith(q) } -> 1
            label.split(' ').filter { it.isNotEmpty() }.joinToString("") { it.take(1) }.startsWith(q) -> 2
            compact.contains(q.replace(" ", "")) -> 3
            else -> null
        }
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .replace(Regex("\\s+"), " ")
}
