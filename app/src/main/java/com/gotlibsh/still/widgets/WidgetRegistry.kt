package com.gotlibsh.still.widgets

import com.gotlibsh.still.data.LauncherSettings
import com.gotlibsh.still.data.SettingsCodec
import com.gotlibsh.still.widgets.clock.ClockWidget
import com.gotlibsh.still.widgets.favorites.FavoritesWidget
import com.gotlibsh.still.widgets.notes.NotesWidget
import com.gotlibsh.still.widgets.weather.WeatherWidget

object WidgetRegistry {

    /** Every widget the launcher knows about, in default top-to-bottom order. */
    val all: List<HomeWidget> = listOf(
        WeatherWidget,
        ClockWidget,
        NotesWidget,
        FavoritesWidget,
    )

    private val byId = all.associateBy { it.id }

    /** All widgets in the user's order, whether switched on or not (for settings). */
    fun ordered(settings: LauncherSettings): List<HomeWidget> =
        SettingsCodec.reconcileOrder(settings.widgetOrder, all.map { it.id }).mapNotNull { byId[it] }

    fun isEnabled(widget: HomeWidget, settings: LauncherSettings): Boolean =
        if (widget.enabledByDefault) widget.id !in settings.disabledWidgets
        else ("+" + widget.id) in settings.disabledWidgets // opt-in widgets store "+id" when switched on

    /** The widgets to draw on the home screen, top to bottom. */
    fun visible(settings: LauncherSettings): List<HomeWidget> =
        ordered(settings).filter { isEnabled(it, settings) }

    /** The new disabled-set after flipping [widget] on or off. */
    fun toggled(widget: HomeWidget, settings: LauncherSettings): Set<String> {
        val set = settings.disabledWidgets
        return if (widget.enabledByDefault) {
            if (widget.id in set) set - widget.id else set + widget.id
        } else {
            val on = "+" + widget.id
            if (on in set) set - on else set + on
        }
    }
}
