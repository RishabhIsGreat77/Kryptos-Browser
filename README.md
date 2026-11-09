# Kryptos Browser

A lightweight, ultra-minimal, and privacy-conscious Android web browser. Built with a premium Hacker/Sci-Fi aesthetic and native Android WebView.

## Features (V3.2.0)

- **Premium Green Theme:** Deep matte dark theme (#0B0B0D) with vibrant neon green accents and a smooth ambient glow behind the fingerprint logo.
- **Smart Auto Speed Dial:** Your most frequently visited websites are automatically added to the home screen. No manual saving required!
- **Real-time Tracker Blocker:** Live counter showing how many trackers and ads have been blocked.
- **History & Bookmarks:** Fully functional, local SQLite databases to manage your browsing data securely.
- **Download Manager:** Seamlessly download files using Android's native Download Manager.
- **Smart Tabs System:** Open multiple tabs with a dynamic tab counter. 
- **Auto Cache Cleaner:** The app automatically clears its cache when it exceeds 100MB to save phone storage.
- **Custom Dark Popups:** All menus and dialogs (including engine selectors) are styled to match the dark theme perfectly.
- **Google CAPTCHA Fix:** Updated User-Agent ensures smoother, CAPTCHA-free Google searches.
- **Auto-Updater:** Check for new updates manually via Settings and update the app seamlessly without losing your data.

## Installation (For Users)

You do not need any development tools to use Kryptos Browser.

1. Go to the [Releases](../../releases) page.
2. Download the latest `Kryptos-Browser-vX.Y.Z.apk` file.
3. Open the APK on your Android device.
4. If prompted, allow your browser or file manager to "Install unknown apps".
5. Install and open Kryptos Browser.

**Important Note for V3 Users:**
If you are upgrading from a version older than V3.1.0, you must uninstall the old app before installing this version. This is because the app's digital signature was updated to support the new Auto-Updater. After this, future updates will overwrite seamlessly.

## Building from Source (For Developers)

The build process is fully automated via GitHub Actions. When code is pushed to the `main` branch, a workflow runs and generates a debug APK.

If you want to build locally:
1. Install JDK 17.
2. Clone this repository.
3. Run `./gradlew assembleDebug` (or `gradle assembleDebug`).
4. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Tech Stack

- **Language:** Kotlin
- **Engine:** Android WebView
- **UI:** Native Android XML (ConstraintLayout, Custom Drawables)
- **Database:** SQLite (for History & Bookmarks)
- **Auto-Updater:** Custom GitHub API integration
- **Build System:** Gradle + GitHub Actions

## Disclaimer

This is a Beta release. Features like Incognito Mode and Hardware Panic Shake are currently under development and will be added in future updates (V4).
