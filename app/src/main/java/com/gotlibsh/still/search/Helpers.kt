package com.gotlibsh.still.search

import android.Manifest
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import com.gotlibsh.still.data.WhatsApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun Context.open(intent: Intent, failure: String) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(this, failure, Toast.LENGTH_SHORT).show()
    }
}

/** "12*7" shows "12*7 = 84". Tap to copy the answer. */
object MathHelper : SearchHelper {
    override val id = "math"
    override val title = "Calculator"
    override val description = "Type a sum like 12*7 to see the answer. Tap it to copy."

    override suspend fun rows(context: Context, query: String): List<HelperRow> {
        val answer = Calculator.answer(query) ?: return emptyList()
        return listOf(
            HelperRow(
                key = "math",
                title = "= $answer",
                hint = "tap to copy",
                onTop = true,
                actions = listOf(
                    HelperAction("Copy") { c ->
                        c.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("answer", answer))
                        Toast.makeText(c, "Copied $answer", Toast.LENGTH_SHORT).show()
                    },
                ),
            ),
        )
    }
}

/** Up to three people from your contacts whose name matches. Tap for call / WhatsApp. */
object ContactsHelper : SearchHelper {
    override val id = "contacts"
    override val title = "Contacts"
    override val description = "Find people by name, then call or WhatsApp them. Needs the Contacts permission; names are only searched on the phone, never stored or sent."
    override val enabledByDefault = false
    override val permission = Manifest.permission.READ_CONTACTS

    private const val MAX = 3

    override suspend fun rows(context: Context, query: String): List<HelperRow> {
        if (query.trim().length < 2) return emptyList()
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return withContext(Dispatchers.IO) {
            val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query.trim()))
            val columns = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            )
            val found = LinkedHashMap<Long, HelperRow>()
            runCatching {
                context.contentResolver.query(uri, columns, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)?.use { c ->
                    while (c.moveToNext() && found.size < MAX) {
                        val contactId = c.getLong(0)
                        if (contactId in found) continue // first number of each person
                        val name = c.getString(1).orEmpty()
                        val number = c.getString(2).orEmpty()
                        val lookup = c.getString(3).orEmpty()
                        if (name.isBlank() || number.isBlank()) continue
                        found[contactId] = row(contactId, lookup, name, number)
                    }
                }
            }
            found.values.toList()
        }
    }

    private fun row(contactId: Long, lookup: String, name: String, number: String) = HelperRow(
        key = "contact-$contactId",
        title = name,
        hint = number,
        holdsAutoOpen = true,
        actions = listOf(
            HelperAction("Call") { c ->
                // Opens the dialer with the number filled in; you press call yourself.
                c.open(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)), "No phone app found")
            },
            HelperAction("WhatsApp") { c ->
                val digits = WhatsApp.digits(WhatsApp.toInternational(c, number))
                if (!WhatsApp.openChat(c, digits)) Toast.makeText(c, "WhatsApp isn't installed", Toast.LENGTH_SHORT).show()
            },
            HelperAction("Open contact") { c ->
                c.open(
                    Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(contactId, lookup)),
                    "No contacts app found",
                )
            },
        ),
    )
}

/** A last row that hands what you typed to your search app or browser. */
object WebSearchHelper : SearchHelper {
    override val id = "web"
    override val title = "Web search"
    override val description = "A last row that searches the web for what you typed. Nothing is sent until you tap it."

    override suspend fun rows(context: Context, query: String): List<HelperRow> = listOf(
        HelperRow(
            key = "web",
            title = "search the web",
            hint = query.trim(),
            actions = listOf(
                HelperAction("Search") { c ->
                    val q = query.trim()
                    try {
                        c.startActivity(
                            Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        c.open(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))),
                            "No browser found",
                        )
                    }
                },
            ),
        ),
    )
}
