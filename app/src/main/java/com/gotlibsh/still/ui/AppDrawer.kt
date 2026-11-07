package com.gotlibsh.still.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gotlibsh.still.data.AppInfo
import com.gotlibsh.still.data.AppSearch
import com.gotlibsh.still.data.LauncherSettings

/** All apps as a plain list, with the keyboard up and search focused. */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onHide: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible = remember(apps, settings.hiddenApps) { apps.filter { it.key !in settings.hiddenApps } }
    val results = remember(visible, query) { AppSearch.filter(visible, query) { it.label } }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(results, query) {
        if (settings.autoLaunchSingleMatch && query.isNotBlank() && results.size == 1) onLaunch(results.first())
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        StillTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "search",
            fontSize = 20,
            imeAction = ImeAction.Go,
            onImeAction = { results.firstOrNull()?.let(onLaunch) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(results, key = { it.key }) { app ->
                DrawerRow(
                    app = app,
                    isFavorite = app.key in settings.favorites,
                    canAddFavorite = settings.favorites.size < LauncherSettings.MAX_FAVORITES,
                    onLaunch = { onLaunch(app) },
                    onToggleFavorite = { onToggleFavorite(app) },
                    onHide = { onHide(app) },
                    onAppInfo = { onAppInfo(app) },
                    onUninstall = { onUninstall(app) },
                )
            }
            if (results.isEmpty()) {
                item { Text("No apps match", color = StillColors.faint, fontSize = 16.sp) }
            }
        }
    }
}

@Composable
private fun DrawerRow(
    app: AppInfo,
    isFavorite: Boolean,
    canAddFavorite: Boolean,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Text(
            text = if (app.isWorkProfile) "${app.label} · work" else app.label,
            fontSize = 20.sp,
            color = StillColors.text,
            modifier = Modifier
                .fillMaxWidth()
                .quietClickable(onLongClick = { menu = true }, onClick = onLaunch)
                .padding(vertical = 10.dp),
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (isFavorite || canAddFavorite) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from home" else "Add to home") },
                    onClick = { menu = false; onToggleFavorite() },
                )
            }
            DropdownMenuItem(text = { Text("Hide") }, onClick = { menu = false; onHide() })
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; onAppInfo() })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; onUninstall() })
        }
    }
}
