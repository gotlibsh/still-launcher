# Still

A quiet, text-only Android home screen. No icons, no badges, no feed: just the time,
the weather, a note to yourself, and a handful of apps. Everything else is one swipe away.

## What's on the home screen

Top to bottom (you can reorder or switch each one off in settings):

| Widget | What it does |
| --- | --- |
| **Weather** | Two quiet lines, like `18°C  Partly cloudy` and `H 21° · L 12° · Tel Aviv`. Tap for the full forecast (now, next 12 hours, next 7 days). Swipe left or right on it to switch between your saved cities. Long-press for settings. It uses [Open-Meteo](https://open-meteo.com) (free, no API key, no account) and cities you type in, so it never asks for your location. It refreshes at most every 30 minutes. |
| **Clock & date** | Tap the time to open alarms, tap the date to open your calendar. |
| **Note & notes link** | A short note on the home screen (tap to edit). An optional `open notes →` link opens your real notes, either an app (Keep, Obsidian…) or any link (`https://…`, `obsidian://…`, `notion://…`). |
| **Google Home switches** | Off by default. A row of small switches (a line icon plus a short word) for chosen Google Home devices. See below. |
| **Favorite apps** | Up to 8 apps shown as plain words. They shrink a little if the screen runs out of room. |

## Google Home switches (optional)

The **Google Home switches** widget shows chosen Google Home devices (AC, boiler, plugs, lights)
as small switches, up to four per row: a thin line icon and a short word, like `❄ living`.
Bright means on, faint means off, crossed out means offline. Tap one to flip it.
Still guesses the icon and word from the device's name and type; change either in
*Settings → Google Home* (tap the icon to cycle, type in the box for the word).
It uses Google's [Home APIs](https://developers.home.google.com/apis/android/overview) and only
listens for changes while the home screen is showing (plus two minutes after you leave it).
Turn it on in *Settings → Google Home → Connect Google Home*, then *+ add device*.

One-time Google setup (Google only lets registered apps ask for Home access):

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project (e.g. "Still").
2. *APIs & Services → OAuth consent screen*: choose **External**, fill in the app name and your email,
   add no scopes, and under **Audience → Test users** add your own Google account. Leave it in *Testing*.
3. *APIs & Services → Credentials → Create credentials → OAuth client ID → Android*, twice:
   - Package `com.gotlibsh.still`, SHA-1 of the release key (from `keytool -list -v -keystore %USERPROFILE%\.still-signing\still-release.jks`).
   - Package `com.gotlibsh.still.dev`, SHA-1 of the debug key (from `gradlew signingReport`), for local test builds.

The SDK isn't on a Maven server; the build downloads it from Google into `.home-sdk/` on first sync
and checks its SHA-256 (see `settings.gradle.kts`).

## Gestures

- **Swipe up**: all apps, with the keyboard already open for search. If only one app matches, it opens right away (you can turn that off).
- **Swipe down**: pull down notifications.
- **Swipe left / right**: open the apps you chose in settings (e.g. camera and phone). On the weather line, the same swipe switches city.
- **Triple tap empty space**: open a WhatsApp chat with the contact you chose in settings.
- **Long-press empty space**: settings.
- **Long-press an app** (in the list or on home): add/remove from home, rename, hide, app info, uninstall.
- **Home button**: always brings you back to a clean home screen.

## Settings worth knowing

- **Background**: black, a few calm dark colors, or your phone's own wallpaper with a dim level.
- **Rename apps**: long-press an app in the list and pick *Rename*. Search finds it by either name.
- **Backup**: *Save settings to a file* writes everything (favorites, names, cities, note, gestures)
  to one JSON file you choose; *Restore settings from a file* loads it back. The file includes your
  note and the triple-tap phone number, so keep it somewhere private. Google Home has to be
  connected again on a new phone.

## Build and install

You need Android Studio (Ladybug or newer) or JDK 17 plus the Android SDK.

1. Open this folder in Android Studio and let it sync.
2. Plug in your phone with USB debugging on and press **Run**.
   From a terminal you can instead run `gradlew.bat installDebug` on Windows, or `./gradlew installDebug` elsewhere.
3. Press the home button and pick **Still**, or go to *Settings → Set Still as the home app*.

If you want to go back to your old launcher, change the default home app in your phone's settings.

## Updates (from anywhere)

Every push to `main` makes GitHub build a signed APK and publish it as a
[release](https://github.com/gotlibsh/still-launcher/releases) (see `.github/workflows/release.yml`).
On the phone, [Obtainium](https://github.com/ImranR98/Obtainium) watches the repo and offers each new version.

- Obtainium: *Add app*, paste `https://github.com/gotlibsh/still-launcher`, then install.
- The release signing key lives only on the PC in `%USERPROFILE%\.still-signing\` (keep a backup!) and
  in the repo's encrypted Actions secrets `STILL_KEYSTORE_BASE64`, `STILL_KEYSTORE_PASSWORD`,
  `STILL_KEY_ALIAS` and `STILL_KEY_PASSWORD`. It is never committed.
- Local `installDebug` builds install as a separate app called **Still dev**, so you can try changes
  without touching the release version.
- The build is pinned: the release workflow uses GitHub Actions by exact commit, and the Google Home
  SDK download must match a known SHA-256, so a tampered download fails the build instead of shipping.

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
app/src/test/              unit tests for search, settings storage, backup, weather and switch labels
```

Run the unit tests with `gradlew test`.

## Privacy

Still makes two kinds of network calls: the weather request to Open-Meteo with the coordinates
of the cities you chose, and, only if you connect it, Google Home through Google's own SDK.
There is no analytics or tracking. Contacts are never read; the triple-tap setting uses Android's
picker, which hands over just the one number you pick.
