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

        // User explicitly launched the app via Launcher icon: clear the lock-screen kill guard
        prefs.edit().putBoolean("pending_incognito_exit", false).commit()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        if (prefs.getBoolean("wipe_on_next_launch", false)) {
            try {
                val tempWebView = WebView(this)
                tempWebView.clearCache(true)
                tempWebView.clearHistory()
                tempWebView.clearFormData()
                tempWebView.clearSslPreferences()

                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies { cookieManager.flush() }
                cookieManager.flush()

                WebStorage.getInstance().deleteAllData()

                val dbHelper = HistoryDatabaseHelper(this)
                dbHelper.clearHistory()

                TabManager.resetAllTabs()

                tempWebView.destroy()

                prefs.edit().putBoolean("wipe_on_next_launch", false).commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_splash", true)
            }
            startActivity(intent)
            finish()
        }, 1500)
    }
}
