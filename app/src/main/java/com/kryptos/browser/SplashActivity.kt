package com.kryptos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
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
                // 1. Initialize a dummy WebView to activate the data directory
                val tempWebView = WebView(this)
                tempWebView.clearCache(true)
                
                // 2. Clear Cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { cookieManager.flush() }
                
                // 3. Clear LocalStorage & WebStorage
                WebStorage.getInstance().deleteAllData()
                
                // 4. Clear History DB
                val dbHelper = HistoryDatabaseHelper(this)
                dbHelper.clearHistory()
                
                // 5. Clear Incognito Tabs
                TabManager.tabsList.removeAll { it.isIncognito }
                
                // 6. Destroy temp WebView
                tempWebView.destroy()
                
                // 7. Reset the flag using commit() to ensure it saves immediately
                prefs.edit().putBoolean("wipe_on_next_launch", false).commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            // Ensure MainActivity is the root of the task
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 1500)
    }
}
