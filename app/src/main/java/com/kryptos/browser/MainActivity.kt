package com.kryptos.browser

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var addressBarText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var homepageContainer: FrameLayout? = null
    private var errorContainer: FrameLayout? = null
    private var errorTitle: TextView? = null
    private var errorMessage: TextView? = null
    private var homepageSearchInput: EditText? = null
    private var txtCurrentEngine: TextView? = null

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences

    private val searchEngines = listOf(
        Pair("Google", "https://www.google.com/search?q="),
        Pair("Bing", "https://www.bing.com/search?q="),
        Pair("DuckDuckGo", "https://duckduckgo.com/?q="),
        Pair("Brave Search", "https://search.brave.com/search?q="),
        Pair("Yahoo", "https://search.yahoo.com/search?p="),
        Pair("Ecosia", "https://www.ecosia.org/search?q=")
    )
    private var selectedEngineIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)

        selectedEngineIndex = prefs.getInt("default_search_engine", 0)

        webView = findViewById(R.id.webview)
        addressBarText = findViewById(R.id.address_bar_text)
        progressBar = findViewById(R.id.progress_bar)
        homepageContainer = findViewById(R.id.homepage_container)
        errorContainer = findViewById(R.id.error_container)
        errorTitle = findViewById(R.id.error_title)
        errorMessage = findViewById(R.id.error_message)
        homepageSearchInput = findViewById(R.id.homepage_search_input)
        txtCurrentEngine = findViewById(R.id.txt_current_engine)

        updateEngineText()

        setupWebView()
        setupToolbar()
        setupHomepage()
        setupErrorPage()
        setupBackPressHandler()

        checkForUpdates()
    }

    private fun updateEngineText() {
        txtCurrentEngine?.text = "Engine: ${searchEngines[selectedEngineIndex].first}"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        
        // FIX: YouTube issue - Use a real Chrome Mobile User-Agent
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = KryptosWebViewClient()
        webView.webChromeClient = KryptosWebChromeClient()
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btn_forward)?.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btn_reload)?.setOnClickListener {
            if (webView.url != null) webView.reload()
        }
        findViewById<ImageButton>(R.id.btn_home)?.setOnClickListener {
            showHomepage()
        }
        findViewById<ImageButton>(R.id.btn_tabs)?.setOnClickListener {
            Toast.makeText(this, "Tabs coming in V3!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<ImageButton>(R.id.btn_shield)?.setOnClickListener {
            showClearDataDialog()
        }

        findViewById<LinearLayout>(R.id.card_privacy_shield)?.setOnClickListener {
            showClearDataDialog()
        }

        addressBarText?.setOnClickListener {
            showAddressDialog()
        }

        findViewById<ImageButton>(R.id.btn_menu)?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_browser, popup.menu)
            
            val isDark = prefs.getBoolean("dark_mode", true)
            val themeMenuItem = popup.menu.findItem(R.id.action_theme)
            themeMenuItem.title = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_theme -> {
                        toggleTheme()
                        true
                    }
                    R.id.action_about -> {
                        showAboutDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Shield")
            .setMessage("Do you want to clear all browsing data (Cookies, Cache, and History)?")
            .setPositiveButton("Clear Data") { _, _ ->
                clearBrowsingData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearBrowsingData() {
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        Toast.makeText(this, "Browsing data cleared successfully.", Toast.LENGTH_SHORT).show()
    }

    private fun showAddressDialog() {
        val input = EditText(this)
        input.setText(if (webView.url == "about:blank") "" else webView.url)
        input.setSelection(input.text.length)
        
        AlertDialog.Builder(this)
            .setTitle("Enter URL or Search")
            .setView(input)
            .setPositiveButton("Go") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) loadUrlOrSearch(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupHomepage() {
        homepageSearchInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = homepageSearchInput?.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrlOrSearch(input)
                    homepageSearchInput?.text?.clear()
                }
                true
            } else false
        }

        findViewById<TextView>(R.id.btn_switch_engine)?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            for (i in searchEngines.indices) {
                val check = if (i == selectedEngineIndex) " ✓" else ""
                popup.menu.add(0, i, i, searchEngines[i].first + check)
            }
            popup.setOnMenuItemClickListener { item ->
                selectedEngineIndex = item.itemId
                prefs.edit().putInt("default_search_engine", selectedEngineIndex).apply()
                updateEngineText()
                Toast.makeText(this, "${searchEngines[selectedEngineIndex].first} selected", Toast.LENGTH_SHORT).show()
                true
            }
            popup.show()
        }
    }

    private fun setupErrorPage() {
        findViewById<Button>(R.id.error_btn_retry)?.setOnClickListener {
            lastFailedUrl?.let { loadUrlOrSearch(it) }
        }
        findViewById<Button>(R.id.error_btn_home)?.setOnClickListener {
            showHomepage()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    errorContainer?.visibility == View.VISIBLE -> showHomepage()
                    homepageContainer?.visibility == View.VISIBLE -> finish()
                    webView.canGoBack() -> webView.goBack()
                    else -> finish()
                }
            }
        })
    }

    private fun loadUrlOrSearch(input: String) {
        val url = if (input.contains(".") && !input.contains(" ")) {
            if (input.startsWith("http://") || input.startsWith("https://")) input
            else "https://$input"
        } else {
            searchEngines[selectedEngineIndex].second + Uri.encode(input)
        }

        homepageContainer?.visibility = View.GONE
        errorContainer?.visibility = View.GONE
        webView.loadUrl(url)
    }

    private fun showHomepage() {
        webView.loadUrl("about:blank")
        homepageContainer?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        addressBarText?.text = "kryptos://home"
        progressBar?.visibility = View.GONE
    }

    private fun toggleTheme() {
        val isDark = prefs.getBoolean("dark_mode", true)
        val newMode = !isDark
        prefs.edit().putBoolean("dark_mode", newMode).apply()
        AppCompatDelegate.setDefaultNightMode(if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        recreate()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage("${getString(R.string.about_description)}\n\nVersion: 2.0.0")
            .setPositiveButton(R.string.about_close, null)
            .show()
    }

    override fun onDestroy() {
        if (this::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/RishabhIsGreat77/Kryptos-Browser/releases/latest")
                val connection = url.openConnection() as HttpsURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestVersionTag = json.getString("tag_name")
                val latestVersion = latestVersionTag.replace("v", "")
                val currentVersion = "2.0.0"

                if (latestVersion > currentVersion) {
                    val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    runOnUiThread { showUpdateDialog(latestVersion, downloadUrl) }
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun showUpdateDialog(version: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version ($version) of Kryptos Browser is available. Update now to get the latest features.")
            .setCancelable(false)
            .setPositiveButton("Update Now") { _, _ -> downloadAndInstallApk(downloadUrl) }
            .setNegativeButton("Later", null)
            .show()
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
                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this, "Please allow Kryptos to install updates, then click Update again.", Toast.LENGTH_LONG).show()
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

    inner class KryptosWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != "about:blank") {
                addressBarText?.text = url
            } else {
                addressBarText?.text = "kryptos://home"
            }
            errorContainer?.visibility = View.GONE
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar?.visibility = View.GONE
        }
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                val desc = error?.description?.toString() ?: "Unknown error"
                lastFailedUrl = request.url.toString()
                errorTitle?.text = getString(R.string.error_title)
                errorMessage?.text = "${getString(R.string.error_message_generic)}\n\n($desc)"
                errorContainer?.visibility = View.VISIBLE
                homepageContainer?.visibility = View.GONE
            }
        }
    }

    inner class KryptosWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress < 100) {
                progressBar?.visibility = View.VISIBLE
                progressBar?.progress = newProgress
            } else {
                progressBar?.visibility = View.GONE
            }
        }
    }
}
