package com.kryptos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Wipe data if Incognito Auto-Kill was triggered
        if (prefs.getBoolean("wipe_on_next_launch", false)) {
            try {
                // Clear Cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { cookieManager.flush() }
                
                // Clear LocalStorage & WebStorage
                WebStorage.getInstance().deleteAllData()
                
                // Clear History DB
                val dbHelper = HistoryDatabaseHelper(this)
                dbHelper.clearHistory()
                
                // Clear Incognito Tabs
                TabManager.tabsList.removeAll { it.isIncognito }
                
                // Reset the flag
                prefs.edit().putBoolean("wipe_on_next_launch", false).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
