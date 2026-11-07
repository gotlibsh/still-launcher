package com.gotlibsh.still.widgets.notes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.ui.StillColors
import com.gotlibsh.still.ui.StillTextField
import com.gotlibsh.still.ui.quietClickable
import com.gotlibsh.still.widgets.HomeWidget
import com.gotlibsh.still.widgets.WidgetHost

/**
 * A short note that lives on the home screen (tap to edit), plus an optional
 * "open notes" link to your real notes: an app (Keep, Obsidian…) or any URL.
 */
object NotesWidget : HomeWidget {
    override val id = "notes"
    override val title = "Note & notes link"

    @Composable
    override fun Content(host: WidgetHost) {
        val settings = host.settings
        var editing by remember { mutableStateOf(false) }
        val notesApp = settings.notesAppKey.takeIf { it.isNotBlank() }?.let(host::findApp)
        val hasLink = notesApp != null || settings.notesUrl.isNotBlank()

        Column(Modifier.fillMaxWidth()) {
            if (editing) {
                NoteEditor(
                    initial = settings.noteText,
                    onChange = { text -> host.editSettings { setNoteText(text) } },
                    onDone = { editing = false },
                )
            } else {
                val note = settings.noteText.trim()
                Text(
                    text = note.ifEmpty { "Tap to write a note" },
                    fontSize = 16.sp,
                    color = if (note.isEmpty()) StillColors.faint else StillColors.dim,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.quietClickable { editing = true },
                )
            }
            if (hasLink && !editing) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "open notes →",
                    fontSize = 14.sp,
                    color = StillColors.dim,
                    modifier = Modifier.quietClickable {
                        if (notesApp != null) host.launch(notesApp)
                        else host.startIntent(Intent(Intent.ACTION_VIEW, Uri.parse(settings.notesUrl)))
                    },
                )
            }
        }
    }

    @Composable
    private fun NoteEditor(initial: String, onChange: (String) -> Unit, onDone: () -> Unit) {
        var text by remember { mutableStateOf(initial) }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        BackHandler(onBack = onDone)

        StillTextField(
            value = text,
            onValueChange = { text = it; onChange(it) },
            placeholder = "Write something…",
            singleLine = false,
            fontSize = 16,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Row {
            Spacer(Modifier.weight(1f))
            Text("done", fontSize = 14.sp, color = StillColors.dim, modifier = Modifier.quietClickable(onClick = onDone))
            Spacer(Modifier.width(4.dp))
        }
    }
}
