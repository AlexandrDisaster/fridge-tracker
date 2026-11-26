# Fridge Tracker

An offline Android pantry and shopping-list app built with Kotlin, Jetpack Compose, Room and WorkManager.

**[Download the v1.0.0 APK](https://github.com/AlexandrDisaster/fridge-tracker/releases/tag/v1.0.0)** · Android 8.0 (API 26) or newer

## Install on your phone

1. Open the release link on your Android phone and download `fridge-tracker-v1.0.0.apk` from **Assets** (not the source-code ZIP).
2. Open the downloaded APK. If Android asks, allow this browser or file manager to install apps, then install Fridge Tracker. You can disable that permission afterward.
3. Open the app. On Android 13+, allow notifications if you want pantry alerts; the inventory still works without them.

This is a signed APK for direct installation, not a Google Play release. The release includes `SHA256SUMS.txt` for download verification. If a previous **debug build** is installed, Android may reject the release because its signing key differs. Back up or record your pantry first: uninstalling the old app erases its local data. Future releases signed with the same release key can update v1 in place.

## What v1 does

- Add and edit groceries with a quantity, unit and expiry date.
- Stack matching names, units and expiry dates into one inventory row: 5 bananas plus another 5 with the same date become 10 pcs. Different expiry dates or units remain separate.
- Reduce quantities as you use food; finished items move to the shopping list.
- Move expired groceries to the shopping list when processing runs. The entered expiry date itself is included; an item becomes expired the next day.
- Suggest approximate fridge storage dates for common products.
- Suggest shopping items for low-stock or soon-expiring groceries.
- Check shopping items and restock them with a new quantity and expiry date.
- Show optional local notifications for finished and expired items.

Storage-date suggestions are rough estimates, not food-safety guarantees. Follow the product label and storage conditions. Background expiry checks use Android WorkManager, so battery restrictions and device scheduling can delay alerts; they are not exact-time alarms.

## Data and permissions

No app account, server or internet permission is required. Pantry and shopping data are stored in an on-device Room database. Android system backup is enabled, so your device's backup settings may also back up app data. The only explicitly requested runtime permission is notifications. Uninstalling the app removes its local data.

## Build and test

Use **JDK 17**, Android SDK Platform **35**, and the included Gradle **8.9** wrapper. Android Studio versions bundled with a newer JDK may need their **Gradle JDK** set explicitly to JDK 17 under Settings > Build, Execution, Deployment > Build Tools > Gradle.

Set `ANDROID_HOME` to your Android SDK, or create an untracked `local.properties` containing `sdk.dir=...`. On Windows:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_HOME = "C:\path\to\Android\Sdk"
.\gradlew.bat testDebugUnitTest lintRelease assembleDebug assembleRelease
```

On macOS/Linux, use `./gradlew` instead. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. The default release build is unsigned: `app/build/outputs/apk/release/app-release-unsigned.apk`. Release publishing requires Android SDK `zipalign` and `apksigner` with a private signing key; keys and passwords must never be committed. The downloadable release APK is already signed.

Unit tests cover pantry persistence/transitions, matching-batch stacking, shopping restocking, product estimates and shopping suggestions. These tests and lint do not replace manual testing on physical devices.

## Development workflow

- `main` contains the published baseline; version tags identify releases.
- Feature branches and separate Git worktrees allow independent contributors or agents to work concurrently.
- Names such as `feature/astra-data-notifications` and `feature/nemotron-ui` describe temporary worker branches, not required app components or dependencies.
- Integrate worker changes once, test the combined app, and open one integration PR. A worker PR whose patch was already cherry-picked is superseded, not another feature to merge.
- Android Studio should open the same integration working folder used for development. Separate worker edits appear there after integration. Use Run or Apply Changes to update a device/emulator.
- Keep SDK paths, `.idea`, build outputs, signing material and credentials out of Git. APKs belong in GitHub Releases rather than source history.

## License

See [LICENSE](LICENSE).
