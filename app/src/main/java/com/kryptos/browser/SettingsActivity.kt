package com.kryptos.browser

import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)

        val switchSuggestions = findViewById<Switch>(R.id.switch_suggestions)
        val switchDarkMode = findViewById<Switch>(R.id.switch_dark_mode)
        val txtClearData = findViewById<TextView>(R.id.txt_clear_data_settings)

        // Load current states
        switchSuggestions.isChecked = prefs.getBoolean("search_suggestions", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", true)

        // Toggle Suggestions
        switchSuggestions.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("search_suggestions", isChecked).apply()
        }

        // Toggle Dark Mode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Clear Data
        txtClearData.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Data")
                .setMessage("Are you sure you want to clear all browsing data?")
                .setPositiveButton("Clear") { _, _ ->
                    WebStorage.getInstance().deleteAllData()
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()
                    Toast.makeText(this, "Data cleared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
