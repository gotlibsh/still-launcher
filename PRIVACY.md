# Still: privacy policy

_Last updated: 11 November 2026_

Still is a minimalist Android home screen (launcher). It is a personal, non-commercial project.
This page explains what data the app handles.

## The short version

Still has no accounts, no analytics, no ads and no tracking. The developer runs no server and
receives no data from the app. Your settings stay on your phone.

## What Still stores on your phone

- Your settings: favorite apps, hidden and renamed apps, home screen layout, background choice,
  weather cities, your home screen note, your notes link, and gesture choices.
- If you set up the triple-tap gesture, the one phone number you picked for it.
- If you use the Google Home switches, the ids of the devices you chose and the short names and
  icons you gave them.

This is saved in the app's private storage. If Android's own backup is switched on for your Google
account, Android may include it in your device backup. "Save settings to a file" writes the same
settings to a file in a place you choose; that file is yours to keep or delete.

## What leaves your phone

- **Weather.** To show the weather, Still sends the coordinates of the cities you typed in to
  [Open-Meteo](https://open-meteo.com), and the city name you search for to Open-Meteo's place
  search. Still never asks for or uses your phone's location. Open-Meteo's own policy applies to
  those requests.
- **Google Home (optional, off by default).** If you connect Google Home, Still uses Google's Home
  APIs on your phone to read the on/off state of the devices you chose and to switch them when you
  tap. This happens between your phone and Google under your Google account. Still does not send
  this information anywhere else. You can remove Still's access at any time in Still's settings or
  in your Google account's connected apps.
- **Opening other apps.** Gestures and links you set up (for example a WhatsApp chat or a notes
  link) simply open that app or link. What happens there is covered by that app's policy.

## Permissions

- **Internet**: weather and, if connected, Google Home.
- **See installed apps**: needed to list and open your apps, as any launcher does.
- **Set alarm / expand status bar**: so tapping the clock opens alarms and swiping down opens
  notifications.

Still does not request access to your contacts, location, camera, microphone or files. Picking a
contact or a backup file uses Android's own picker, which hands Still only the single item you choose.

## Children

Still is a general-purpose tool and does not knowingly collect data from anyone, including children.

## Changes and contact

Changes to this policy will be posted on this page. For questions, open an issue at
<https://github.com/gotlibsh/still-launcher/issues>.
