package com.kryptos.browser

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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

        switchSuggestions.isChecked = prefs.getBoolean("search_suggestions", true)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", true)

        switchSuggestions.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("search_suggestions", isChecked).apply()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        txtClearData.setOnClickListener {
            // यहाँ भी नया Sci-Fi डायलॉग लगाया गया है
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
                Toast.makeText(this, "Data cleared!", Toast.LENGTH_SHORT).show()
            }
            dialog.show()
        }
    }
}
