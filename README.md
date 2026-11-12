<div align="center">
  <img src="app/src/main/res/drawable/app_icon.png" width="120" alt="Kryptos Logo">
  <h1>Kryptos Browser</h1>
  <p><b>Next-Gen Privacy. Dark Sci-Fi UI. Unmatched Speed.</b></p>
  
  [![Version](https://img.shields.io/badge/Version-4.0.0-00E676?style=for-the-badge)]()
  [![Android](https://img.shields.io/badge/Android-8.0%2B-141418?style=for-the-badge&logo=android)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-100%25-00E676?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
</div>

<br>

**Kryptos** is a high-performance, privacy-focused Android browser designed with a deep matte black and neon green dark sci-fi theme. Built entirely from a mobile phone using GitHub Actions.

---

## 🚀 What's New in V4

V4 is a massive update that takes privacy and UI to the next level:

*   🕵️ **Incognito Mode (Fully Functional):** Normal and incognito tabs are now completely separated. Incognito mode features a custom hat/glasses icon and all incognito tabs are auto-killed when the app is closed.
*   🎭 **Auto-Hiding Dock & Pill Bar:** Implemented an iOS-style auto-hiding dock to prevent UI overlap on websites like YouTube. The dock hides when loading a website, replaced by a swipable pill bar.
*   🛡️ **Advanced Ad-Blocker (500+ Trackers):** Integrated a massive hosts list that blocks over 500 known trackers, ad networks, and analytics domains.
*   ✂️ **Link Defanger:** Automatically strips tracking parameters like `utm_*`, `fbclid`, etc., from URLs before loading them.
*   🗑️ **Smart UI Updates:** Added empty states (No history yet, No bookmarks yet) and a 'Clear All' trash icon in Bookmarks.
*   ❌ **Removed:** Panic Shake and Ghost Vault features have been completely removed to keep the app lightweight and clean.

---

## ✨ Core Features

*   **Dark Sci-Fi UI:** Deep matte black (#0B0B0D) and Neon Green (#00E676) accents.
*   **Floating Dock:** Custom glowing fire button, tabs counter, and back/home controls.
*   **Speed Dial:** Automatically fetches your top 5 most visited sites from SQLite database with grayscale favicons.
*   **Auto Cache Cleaner:** Background thread automatically clears cache if it exceeds 100MB.
*   **Custom WebView:** Spoofs a Pixel 8 user agent to fix CAPTCHA issues and load desktop-grade mobile sites.
*   **Incognito Auto-Kill:** Option to kill the app and clear data instantly when the screen turns off.
*   **Built-in Updater:** Check for updates directly from GitHub releases within the Settings menu.

---

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI:** Native Android XML (ConstraintLayout, LinearLayout, GridLayout)
*   **Database:** SQLite (SQLiteOpenHelper)
*   **Preferences:** SharedPreferences
*   **Build System:** GitHub Actions (JDK 17, Gradle 8.7)
*   **Min SDK:** 26 (Android 8.0)
*   **Target SDK:** 34 (Android 14)

---

## 📂 Architecture Overview

*   `MainActivity.kt`: Core WebView, Toolbar, Auto-Hiding Dock, Tracker Interception.
*   `DockController.kt`: Handles all animations for the floating dock and pill bar.
*   `TrackerList.kt`: 500+ blacklisted domains for the ad-blocker.
*   `TabManager.kt`: Manages normal and incognito tabs in memory.
*   `HistoryDatabaseHelper.kt` & `BookmarkDatabaseHelper.kt`: Manages local SQLite databases.
*   `LinkDefanger` (Logic in MainActivity): Cleans URLs before loading.

---

## ⚙️ How to Build

This project runs on a completely cloud-based workflow. No PC is required.

1. Edit code directly via the GitHub Web UI.
2. Commit changes to the `main` branch.
3. GitHub Actions will automatically trigger and generate `Kryptos-Debug.apk`.

---

## 👨‍💻 Developer

**Rishabh**
*Built with passion, purely from an Android Phone.* 📱
