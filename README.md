# Kryptos Browser

A lightweight, ultra-minimal, and privacy-conscious Android web browser. Built with a premium Sci-Fi aesthetic and native Android WebView.

## Features (V2)

- **Premium Sci-Fi UI:** Deep matte dark theme (#0B0B0D) with elegant cyan accents, minimal search layout, and a unique rotating orbital logo.
- **Native Performance:** Powered by Android WebView with hardware-accelerated rendering.
- **Seamless Splash Screen:** Custom premium intro screen.
- **Smart Search Engine Switcher:** Choose between 6 major engines (Google, Bing, DuckDuckGo, Brave, Yahoo, Ecosia) on the fly.
- **Auto-Updater:** Integrated with GitHub Releases API. The app automatically checks for new versions and prompts the user to update seamlessly, without leaving the app or visiting a browser.
- **Secure & Private:** Cleartext HTTP traffic is strictly blocked. Privacy Shield design elements included (Coming soon: actual tracker blocking).

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
- **UI:** Native Android XML (Sci-Fi Minimal Design)
- **Auto-Updater:** Custom Github API integration (No third-party libraries)
- **Build System:** Gradle + GitHub Actions

## Disclaimer

Private browsing mode or tracking protection does not make you completely anonymous. Websites, internet providers, and network administrators may still see your activity. The "Privacy Shield" is currently a UI representation; full network-level tracker blocking is planned for a future version.
