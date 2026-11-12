<div align="center">
  <img src="app/src/main/res/drawable/app_icon.png" width="120" alt="Kryptos Logo">
  <h1>Kryptos Browser</h1>
  <p><b>Next-Gen Privacy. Dark Sci-Fi UI. Unmatched Speed.</b></p>
  
  [![Version](https://img.shields.io/badge/Version-4.0.0-00E676?style=for-the-badge)]()
  [![Android](https://img.shields.io/badge/Android-8.0%2B-141418?style=for-the-badge&logo=android)](https://developer.android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-100%25-00E676?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
</div>

<br>

**Kryptos** ek high-performance, privacy-focused Android browser hai jo ek deep matte black aur neon green dark sci-fi theme ke saath design kiya gaya hai. Isse maine (Rishabh) completely mobile se build kiya hai using GitHub Actions.

---

## 🚀 V4 Me Kya Naya Hai? (What's New in V4)

V4 ek bahut bada update hai jisme privacy aur UI ko completely next level pe le jaya gaya hai:

*   🕵️ **Incognito Mode (Fully Functional):** Ab normal tabs aur incognito tabs completely alag hain. Incognito me ek custom hat/glasses icon dikhta hai aur app band hone pe saare incognito tabs auto-kill ho jaate hain.
*   🎭 **Auto-Hiding Dock & Pill Bar:** YouTube jaise websites pe dock controls ko overlap hone se bachane ke liye ek iOS-style auto-hiding dock lagaya gaya hai. Website khulte hi dock chhup jaata hai aur ek swipable pill bar aata hai.
*   🛡️ **Advanced Ad-Blocker (500+ Trackers):** Ek massive hosts list integrate ki gayi hai jo 500+ known trackers, ad networks aur analytics domains ko block karti hai.
*   ✂️ **Link Defanger:** Automatically URLs se `utm_*`, `fbclid`, aur baaki tracking parameters ko strip kar deta hai.
*   🗑️ **Smart UI Updates:** Empty states (No history yet, No bookmarks yet) aur Bookmarks me 'Clear All' trash icon add kiya gaya hai.
*   ❌ **Removed:** Panic Shake aur Ghost Vault (V3 ke kuch features) ko completely hata diya gaya hai taaki app lightweight aur clean rahe.

---

## ✨ Core Features

*   **Dark Sci-Fi UI:** Deep matte black (#0B0B0D) aur Neon Green (#00E676) accents.
*   **Floating Dock:** Custom glowing fire button, tabs counter, aur back/home controls.
*   **Speed Dial:** Automatically fetches your top 5 most visited sites from SQLite database with grayscale favicons.
*   **Auto Cache Cleaner:** Background thread automatically clears cache if it exceeds 100MB.
*   **Custom WebView:** Spoofs a Pixel 8 user agent to fix CAPTCHA issues and load desktop-grade mobile sites.
*   **Incognito Auto-Kill:** Option to kill the app and clear data instantly when the screen turns off.
*   **Built-in Updater:** Settings me jaakar directly GitHub releases se update check kar sakte hain.

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
*   `TabManager.kt`: Manages normal and incognito tabs memory.
*   `HistoryDatabaseHelper.kt` & `BookmarkDatabaseHelper.kt`: Manages local SQLite databases.
*   `LinkDefanger.kt` (Logic inside MainActivity): Cleans URLs before loading.

---

## ⚙️ How to Build

Ye project completely cloud-based workflow pe chalta hai. Koi PC required nahi hai.

1. Code edit karo seedha GitHub Web UI se.
2. `main` branch pe commit karo.
3. GitHub Actions automatically trigger hoga aur `Kryptos-Debug.apk` generate kar dega.

---

## 👨‍💻 Developer

**Rishabh**
*Built with passion, purely from an Android Phone.* 📱
