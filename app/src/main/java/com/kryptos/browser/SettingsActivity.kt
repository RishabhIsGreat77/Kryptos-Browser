package com.kryptos.browser

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Switch
import android.widget.TextView
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
        val txtCheckUpdates = findViewById<TextView>(R.id.txt_check_updates)

        switchSuggestions.isChecked = prefs.getBoolean("search_suggestions", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", true)

        switchSuggestions.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("search_suggestions", isChecked).apply()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Clear Data Logic
        txtClearData.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_custom)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val title = dialog.findViewById<TextView>(R.id.dialog_title)
            val message = dialog.findViewById<TextView>(R.id.dialog_message)
            val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
            val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

            title.text = "Clear Data"
            message.text = "Are you sure you want to clear all browsing data?"
            btnConfirm.text = "Clear"

            btnCancel.setOnClickListener { dialog.dismiss() }
            btnConfirm.setOnClickListener {
                dialog.dismiss()
                WebStorage.getInstance().deleteAllData()
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                showCustomToast("Browsing data cleared successfully!")
            }
            dialog.show()
        }

        // NAYA: Check for Updates Logic
        txtCheckUpdates.setOnClickListener {
            showCustomToast("Checking for updates...")
            // Yahan hum update check karne ka logic call karenge
            // Filhaal yeh message dikhega
        }
    }

    // NAYA: Dark Theme Custom Toast
    private fun showCustomToast(message: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val msg = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)
        
        title.visibility = View.GONE
        btnCancel.visibility = View.GONE
        btnConfirm.text = "OK"
        msg.text = message
        
        btnConfirm.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
