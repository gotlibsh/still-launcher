package com.gotlibsh.still.search

import android.content.Context

/** One thing a helper row can do when tapped, e.g. "Call" or "WhatsApp". */
class HelperAction(val label: String, val run: (Context) -> Unit)

/**
 * A row a helper adds to the app list while you type. With one action, a tap runs it;
 * with several, a tap shows them as a small menu.
 */
class HelperRow(
    val key: String,
    val title: String,
    val hint: String,
    val actions: List<HelperAction>,
    /** Shown above the apps (an answer) instead of below them. */
    val onTop: Boolean = false,
    /** True if this row means you may be looking for it, so a single app match shouldn't auto-open. */
    val holdsAutoOpen: Boolean = false,
)

/**
 * Something extra the search box can do besides finding apps. To add one:
 *  1. Create an `object MyHelper : SearchHelper` with a unique [id].
 *  2. Add it to [SearchHelpers.all].
 * It then gets its own on/off switch in Settings > Search helpers.
 */
interface SearchHelper {
    /** Stable id, saved in settings. Never change it once released. */
    val id: String

    /** Name and one-line explanation shown in settings. */
    val title: String
    val description: String

    val enabledByDefault: Boolean get() = true

    /** An Android permission to ask for when the helper is switched on, or null. */
    val permission: String? get() = null

    /** Rows for what was typed (never blank). Runs off the main thread; return empty for "nothing". */
    suspend fun rows(context: Context, query: String): List<HelperRow>
}

object SearchHelpers {
    val all: List<SearchHelper> = listOf(MathHelper, ContactsHelper, WebSearchHelper)

    val defaults: Set<String> = all.filter { it.enabledByDefault }.map { it.id }.toSet()

    fun enabled(ids: Set<String>): List<SearchHelper> = all.filter { it.id in ids }
}
