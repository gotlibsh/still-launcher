# Still

A quiet, text-only Android home screen. No icons, no badges, no feed: just the time,
the weather, a note to yourself, and a handful of apps. Everything else is one swipe away.

## What's on the home screen

Top to bottom (you can reorder or switch each one off in settings):

| Widget | What it does |
| --- | --- |
| **Weather** | One line, like `18°C  Partly cloudy · H 21° · L 12° · Tel Aviv`. Tap to refresh, long-press to change the city. It uses [Open-Meteo](https://open-meteo.com) (free, no API key, no account) and a city you type in, so it never asks for your location. It refreshes at most every 30 minutes. |
| **Clock & date** | Tap the time to open alarms, tap the date to open your calendar. |
| **Note & notes link** | A short note on the home screen (tap to edit). An optional `open notes →` link opens your real notes, either an app (Keep, Obsidian…) or any link (`https://…`, `obsidian://…`, `notion://…`). |
| **Favorite apps** | Up to 8 apps shown as plain words. |

## Gestures

- **Swipe up**: all apps, with the keyboard already open for search. If only one app matches, it opens right away (you can turn that off).
- **Swipe down**: pull down notifications.
- **Long-press empty space**: settings.
- **Long-press an app** (in the list or on home): add/remove from home, hide, app info, uninstall.
- **Home button**: always brings you back to a clean home screen.

## Build and install

You need Android Studio (Ladybug or newer) or JDK 17 plus the Android SDK.

1. Open this folder in Android Studio and let it sync.
2. Plug in your phone with USB debugging on and press **Run**.
   From a terminal you can instead run `gradlew.bat installDebug` on Windows, or `./gradlew installDebug` elsewhere.
3. Press the home button and pick **Still**, or go to *Settings → Set Still as the home app*.

If you want to go back to your old launcher, change the default home app in your phone's settings.

## Adding your own widget

Each block on the home screen is a `HomeWidget`. To add one:

1. Create a file such as `widgets/quote/QuoteWidget.kt`:

   ```kotlin
   object QuoteWidget : HomeWidget {
       override val id = "quote"          // saved in settings, never change it
       override val title = "Daily quote" // shown in Settings → Home screen

       @Composable
       override fun Content(host: WidgetHost) {
           Text("Do less, better.", color = StillColors.dim, fontSize = 16.sp)
       }
   }
   ```

2. Add it to the list in `widgets/WidgetRegistry.kt`.

That's it. It now shows on the home screen and in settings, where you can reorder it or switch it off.

Through `host`, a widget can read settings (`host.settings`), find and launch apps
(`host.findApp`, `host.launch`), open any intent safely (`host.startIntent`), open settings,
and save settings (`host.editSettings { ... }`). If your widget needs a new setting, add a
field to `LauncherSettings` and a key plus setter in `SettingsRepository`.

## Project layout

```
app/src/main/java/com/gotlibsh/still/
  MainActivity.kt          switches between home, app list and settings
  LauncherViewModel.kt     app list + settings state
  StillApp.kt              single instances of the repositories
  data/                    apps (LauncherApps), settings (DataStore), search
  ui/                      home screen, app list, settings, shared components
  widgets/                 HomeWidget interface, registry, and each widget
app/src/test/              unit tests for search, settings storage and weather parsing
```

Run the unit tests with `gradlew test`.

## Privacy

The only network call is the weather request to Open-Meteo with the coordinates of the
city you chose. Nothing else leaves the phone.
