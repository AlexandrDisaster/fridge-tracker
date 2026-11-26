# Fridge Tracker

A native Android app for keeping a live inventory of groceries, tracking quantities and expiry dates, and maintaining a shopping list automatically.

## Features

- Add and edit groceries with quantity, unit, and expiry date.
- Adjust pantry amounts as food is used.
- Automatically queue finished and expired groceries on the in-app shopping list.
- Receive on-device Android notifications when groceries expire or run out.
- Keep data on-device; no account or network connection is required for pantry tracking.

## Build

Requires JDK 17 and Android SDK Platform 35. Use the Gradle wrapper:

```sh
./gradlew test
./gradlew assembleDebug
```

The app targets Android 15 (API 35) and supports Android 8.0 (API 26) and later.
