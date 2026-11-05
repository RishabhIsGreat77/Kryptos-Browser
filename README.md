# Kryptos Browser

A lightweight, ultra-minimal, and privacy-conscious Android web browser. Built with a premium Hacker/Sci-Fi aesthetic and native Android WebView.

## Features (V3 Beta)

- **Premium Green Theme:** Deep matte dark theme (#0B0B0D) with vibrant neon green accents.
- **Fingerprint Branding:** Unique custom-designed fingerprint logo replacing the old orbital design.
- **Smart Speed Dial:** A minimalist home screen grid to add and access your favorite websites quickly.
- **Real-time Tracker Blocker:** Live counter showing how many trackers and ads have been blocked.
- **Enhanced Floating Dock:** A beautiful bottom navigation bar with a glowing central "Fire" (Clear Data) button.
- **Auto-Updater:** Integrated with GitHub Releases API. The app automatically checks for new versions and prompts the user to update seamlessly.

## Installation (For Users)

You do not need any development tools to use Kryptos Browser.

1. Go to the [Releases](../../releases) page.
2. Download the latest `Kryptos-Browser-vX.Y.Z.apk` file.
3. Open the APK on your Android device.
4. If prompted, allow your browser or file manager to "Install unknown apps".
5. Install and open Kryptos Browser. Future updates will be handled automatically by the app itself!

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
- **UI:** Native Android XML (ConstraintLayout & Custom Drawables)
- **Auto-Updater:** Custom GitHub API integration
- **Build System:** Gradle + GitHub Actions

## Disclaimer

This is a Beta release. Features like Full Tabs System, History, and Incognito Mode are currently under development and will be added in future updates.
