# Kryptos Browser

A lightweight, privacy-conscious Android web browser built from scratch. Powered by Android WebView.

## Features (V1)

- Clean and minimal homepage
- Fast URL and search handling
- Dedicated light and dark themes (persistent)
- Basic navigation (Back, Forward, Reload, Home)
- Helpful error pages for network and SSL issues
- Secure: Cleartext HTTP is blocked by default

## Installation (For Users)

You do not need any development tools to use Kryptos Browser.

1. Go to the [Releases](../../releases) page.
2. Download the latest `Kryptos-Browser-vX.Y.Z.apk` file.
3. Open the APK on your Android device.
4. If prompted, allow your browser or file manager to "Install unknown apps".
5. Install and open Kryptos Browser.

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
- **UI:** Native Android XML (Light / Dark modes)
- **Build System:** Gradle

## Disclaimer

Private browsing mode or tracking protection (coming in future versions) does not make you completely anonymous. Websites, internet providers, and network administrators may still see your activity.
