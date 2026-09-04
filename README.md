# xxSlashboardxx for Android

Slashboard is a native Sinhala keyboard for Android. It works entirely on-device
and supports three input methods:

- **Smart Phonetic** — adaptive romanized Sinhala input
- **Phonetic** — direct phonetic transliteration
- **Wijesekara** — the standard Sinhala keyboard layout (SLS)

The application ID is `lk.org.slashboard.keyboard`, and Android displays the input
method as **slashboard Sinhala**.

## Features

- Local Sinhala word and next-word suggestions
- Sinhala-aware emoji suggestions and a built-in emoji picker
- Optional, on-device learning from accepted words
- Optional clipboard history with pinned items
- Number, symbol, email, URL, phone, and decimal layouts
- Light, dark, and system themes
- Adjustable key spacing and keyboard height
- Full-width and one-handed layouts
- Configurable vibration, key sounds, and high-contrast keys
- Forgiving touch detection with local touch personalization

## Privacy

slashboard has no internet permission, analytics SDK, or cloud dependency. Text
composition, suggestions, learned words, emoji search, touch personalization,
preferences, and clipboard history are processed and stored on the device.

Clipboard history is disabled by default and is unavailable in restricted input
fields. Recent entries can be cleared from settings; pinned entries remain until
they are removed individually. slashboard's local application data is excluded from
Android cloud backup and device transfer.

## Requirements

- Android Studio or the Android command-line tools
- JDK 17
- Android SDK Platform 36

The included Gradle wrapper downloads the required Gradle version automatically.

## Build and test

Clone the repository, open this directory in Android Studio, or run the following
from a terminal:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device or emulator with:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Enable the keyboard

1. Open **Slashboard** from the app launcher.
2. Tap **Enable slashboard**.
3. Enable **slashboard Sinhala** in Android's on-screen keyboard settings.
4. Return to slashboard and tap **Select active keyboard**.
5. Choose **slashboard Sinhala** from the keyboard picker.

Android does not allow an application to enable its own input method, so the
settings confirmation is required.
