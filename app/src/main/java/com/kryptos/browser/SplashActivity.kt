package com.kryptos.browser

import android.annotation.SuppressLint
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
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start update check
        checkForUpdates()

        // After 1.5s, go to MainActivity (If dialog is not showing)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
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
                    val latestVersionTag = json.getString("tag_name") 
                    val latestVersion = latestVersionTag.replace("v", "").replace("-beta", "").trim()
                    val currentVersion = "3.0.0" // REAL VERSION

                    if (latestVersion > currentVersion) {
                        val assets = json.getJSONArray("assets")
                        if (assets.length() > 0) {
                            var downloadUrl = ""
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }
                            if (downloadUrl.isNotEmpty()) {
                                runOnUiThread { showUpdateDialog(latestVersion, downloadUrl) }
                            }
                        }
                    }
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun showUpdateDialog(version: String, downloadUrl: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Update Available"
        message.text = "A new version ($version) of Kryptos Browser is available. Update now?"
        btnConfirm.text = "Update"
        btnConfirm.setTextColor(Color.parseColor("#00E676"))

        btnCancel.setOnClickListener { 
            dialog.dismiss()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
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
                runOnUiThread { progressDialog.dismiss(); Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show() }
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
}
