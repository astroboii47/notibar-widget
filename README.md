# Notibar Widget

An Android home screen widget that shows a compact row of app icons with notification counts, sorted by the most recent notification.

## What It Does

- Shows your most recent apps with active notifications in a single row widget
- Displays per-app badge counts
- Filters out ongoing and low-priority noise
- Lets you choose how many icons appear before the `+X` overflow
- Lets you control badge text size and overlay size
- Lets you limit the widget to selected apps only
- Supports custom icons per app
- Supports optional icon packs
- Shows icon previews in settings
- Keeps widget refreshes light by caching effective widget icons

## New Icon Features

- `Custom icons per app`
  Pick any image for a specific app and use it in the widget.
- `Optional icon pack support`
  Choose an installed icon pack. If the pack has a match for an app, the widget uses it. If not, it falls back to the app's normal icon.
- `Icon preview in settings`
  The settings screen now shows a live preview row so you can see the current icon style quickly.
- `No extra widget lag`
  The widget uses cached rendered icons, so custom images and icon-pack matches do not add repeated per-refresh work.

## Setup

1. Install the app.
2. Open the app and grant Notification Access.
3. Add the widget from your home screen widgets list.
4. Open Widget Display Settings to adjust:
   - icon count
   - badge font size
   - overlay size
   - included apps
   - icon pack
   - per-app icon overrides

## Icon Customization

Open `Widget display settings` and:

- Tap `Choose icon pack` to select an installed pack
- Tap `Choose apps`
- Tap `Icon` next to any app to:
  - use the app's normal icon
  - use the selected icon pack icon
  - pick a custom image
  - remove a custom image

## Build From Source

### Requirements

- Android Studio / Android SDK
- JDK 17
- Android SDK 34

### Build

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Project Notes

- Minimum SDK: `26`
- Target SDK: `34`
- Language: `Java 17`
- Widget updates are driven by a `NotificationListenerService`

## Planned Improvements

- screenshots / demo GIF
- release builds and GitHub Releases
- backup / import of widget settings

