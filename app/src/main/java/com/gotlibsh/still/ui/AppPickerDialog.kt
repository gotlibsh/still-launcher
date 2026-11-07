package com.gotlibsh.still.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.AppSearch
import kotlinx.coroutines.delay

/** Pick one app from a searchable list. */
@Composable
fun AppPickerDialog(
    title: String,
    apps: List<AppInfo>,
    onPick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) { AppSearch.filter(apps, query) { it.label } }
    Dialog(onDismissRequest = onDismiss) {
        val focus = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            delay(100) // let the dialog window attach first
            focus.requestFocus()
            keyboard?.show()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(StillColors.background)
                .padding(24.dp),
        ) {
            Text(title, fontSize = 14.sp, color = StillColors.dim)
            StillTextField(value = query, onValueChange = { query = it }, placeholder = "search", modifier = Modifier.fillMaxWidth().focusRequester(focus))
            LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(results, key = { it.key }) { app ->
                    Text(
                        text = if (app.isWorkProfile) "${app.label} · work" else app.label,
                        fontSize = 18.sp,
                        color = StillColors.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .quietClickable { onPick(app) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}
