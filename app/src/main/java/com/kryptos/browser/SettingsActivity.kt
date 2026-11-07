package com.kryptos.browser

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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

        // NAYA: Manual Update Check Logic
        txtCheckUpdates.setOnClickListener {
            showCustomToast("Checking for updates...")
            checkForUpdates()
        }
    }

    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/RishabhIsGreat77/Kryptos-Browser/releases/latest")
                val connection = url.openConnection() as HttpsURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "KryptosBrowser-Updater")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name").replace("v", "").replace("-beta", "").trim()
                    val currentVersion = "3.1.0"

                    runOnUiThread {
                        if (latestVersion > currentVersion) {
                            val assets = json.getJSONArray("assets")
                            if (assets.length() > 0) {
                                var downloadUrl = ""
                                for (i in 0 until assets.length()) {
                                    if (assets.getJSONObject(i).getString("name").endsWith(".apk")) {
                                        downloadUrl = assets.getJSONObject(i).getString("browser_download_url")
                                        break
                                    }
                                }
                                if (downloadUrl.isNotEmpty()) {
                                    showUpdateDialog(latestVersion, downloadUrl)
                                }
                            }
                        } else {
                            showCustomToast("You are on the latest version (v$currentVersion)")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showCustomToast("Failed to check updates: ${e.message}") }
            }
        }.start()
    }

    private fun showUpdateDialog(version: String, downloadUrl: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Update Available"
        message.text = "A new version ($version) is available. Update now?"
        btnConfirm.text = "Update"
        btnConfirm.setTextColor(Color.parseColor("#00E676"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            downloadAndInstallApk(downloadUrl)
        }
        dialog.show()
    }

    private fun downloadAndInstallApk(apkUrl: String) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Downloading update...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        Thread {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpsURLConnection
                connection.connect()
                val apkFile = File(externalCacheDir, "kryptos_update.apk")
                connection.inputStream.use { input -> apkFile.outputStream().use { output -> input.copyTo(output) } }
                runOnUiThread {
                    progressDialog.dismiss()
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    showCustomToast("Update failed: ${e.message}")
                }
            }
        }.start()
    }

    private fun installApk(apkFile: File) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                return
            }
        }
        val apkUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

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
