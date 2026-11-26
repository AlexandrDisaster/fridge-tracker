# Fridge Tracker

A native Android app for keeping a live inventory of groceries, tracking quantities and expiry dates, and maintaining a shopping list automatically.

## Features

- Add and edit groceries with quantity, a selectable unit, and expiry date.
- Suggest approximate fridge expiry dates for common groceries; always follow package labeling and use your judgment.
- Adjust pantry amounts as food is used.
- Get optional in-app shopping suggestions for low-stock and soon-expiring groceries.
- Automatically queue finished and expired groceries on the in-app shopping list.
- Select checked shopping-list items and add them to the fridge with quantity, unit, and expiry-date selection.
- Receive on-device Android notifications when groceries expire or run out.
- Keep data on-device; no account or network connection is required for pantry tracking.

## Build

Requires JDK 17 and Android SDK Platform 35. Use the Gradle wrapper:

```sh
./gradlew test
./gradlew assembleDebug
```

The app targets Android 15 (API 35) and supports Android 8.0 (API 26) and later.
