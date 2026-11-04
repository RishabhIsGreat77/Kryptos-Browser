package com.kryptos.browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var addressBar: EditText? = null
    private var progressBar: ProgressBar? = null
    private var homepageContainer: FrameLayout? = null
    private var errorContainer: FrameLayout? = null
    private var bottomDock: LinearLayout? = null
    private var txtTrackerCount: TextView? = null
    
    // Search Engine Switcher (Top Bar)
    private var btnEngineSwitch: ImageView? = null

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences

    // Real-time Tracker Counter (Starts from 0 or saved value)
    private var trackerCount = 0

    private val searchEngines = listOf(
        Pair("Google", "https://www.google.com/search?q="),
        Pair("Bing", "https://www.bing.com/search?q="),
        Pair("DuckDuckGo", "https://duckduckgo.com/?q="),
        Pair("Brave Search", "https://search.brave.com/search?q="),
        Pair("Yahoo", "https://search.yahoo.com/search?p="),
        Pair("Ecosia", "https://www.ecosia.org/search?q=")
    )
    private var selectedEngineIndex = 0

    private val blockedDomains = listOf(
        "doubleclick.net", "google-analytics.com", "googletagmanager.com",
        "facebook.com/tr", "scorecardresearch.com", "adservice.google.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)

        selectedEngineIndex = prefs.getInt("default_search_engine", 0)
        trackerCount = prefs.getInt("tracker_count", 0)

        webView = findViewById(R.id.webview)
        addressBar = findViewById(R.id.address_bar)
        progressBar = findViewById(R.id.progress_bar)
        homepageContainer = findViewById(R.id.homepage_container)
        errorContainer = findViewById(R.id.error_container)
        bottomDock = findViewById(R.id.bottom_dock)
        
        // Homepage IDs
        txtTrackerCount = findViewById(R.id.txt_tracker_count)
        btnEngineSwitch = findViewById(R.id.btn_engine_switch)

        updateTrackerUI()
        setupWebView()
        setupToolbar()
        setupBackPressHandler()

        // Load Speed Dial and Add button
        setupHomepageSpeedDial()

        checkForUpdates()
    }

    private fun updateTrackerUI() {
        txtTrackerCount?.text = String.format("%,d", trackerCount)
    }

    private fun setupHomepageSpeedDial() {
        // Dummy function for Speed Dial (Will be fully functional in next step)
        findViewById<LinearLayout>(R.id.btn_add_speed_dial)?.setOnClickListener {
            Toast.makeText(this, "Add Speed Dial coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        addressBar?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && addressBar?.text.toString() == "kryptos://home") {
                addressBar?.setText("")
            }
        }

        addressBar?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = addressBar?.text.toString().trim()
                if (input.isNotEmpty()) loadUrlOrSearch(input)
                true
            } else false
        }

        // Top Bar: Search Engine Switch (Chota sa icon)
        btnEngineSwitch?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            for (i in searchEngines.indices) {
                val check = if (i == selectedEngineIndex) " ✓" else ""
                popup.menu.add(0, i, i, searchEngines[i].first + check)
            }
            popup.setOnMenuItemClickListener { item ->
                selectedEngineIndex = item.itemId
                prefs.edit().putInt("default_search_engine", selectedEngineIndex).apply()
                Toast.makeText(this, "${searchEngines[selectedEngineIndex].first} selected", Toast.LENGTH_SHORT).show()
                true
            }
            popup.show()
        }

        // Top Bar: Reload
        findViewById<ImageButton>(R.id.btn_reload)?.setOnClickListener {
            if (webView.url != null) webView.reload()
        }

        // Bottom Dock Buttons
        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btn_forward)?.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btn_home)?.setOnClickListener {
            showHomepageUI()
        }
        
        // NAYA: Fire Button (Clear Data)
        findViewById<ImageButton>(R.id.btn_fire)?.setOnClickListener {
            showClearDataDialog()
        }

        findViewById<ImageButton>(R.id.btn_tabs)?.setOnClickListener {
            startActivity(Intent(this, TabsActivity::class.java))
        }

        // NAYA: Menu Button (Settings, Incognito, History, Dark Mode)
        findViewById<ImageButton>(R.id.btn_menu)?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            
            val isDark = prefs.getBoolean("dark_mode", true)
            val themeText = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
            
            popup.menu.add(0, 1, 1, "Incognito Mode")
            popup.menu.add(0, 2, 2, "History")
            popup.menu.add(0, 3, 3, themeText)
            popup.menu.add(0, 4, 4, "Settings")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        Toast.makeText(this, "Incognito Mode coming in V4!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    2 -> {
                        Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> {
                        val newMode = !isDark
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                        AppCompatDelegate.setDefaultNightMode(if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                        recreate()
                        true
                    }
                    4 -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (homepageContainer?.visibility == View.VISIBLE) {
                    return
                }
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    showHomepageUI()
                }
            }
        })
    }

    private fun showClearDataDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Clear Data"
        message.text = "Do you want to clear all browsing data (Cookies, Cache, and History)?"
        btnConfirm.text = "Clear Data"

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            clearBrowsingData()
        }
        dialog.show()
    }

    private fun clearBrowsingData() {
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        
        // Reset tracker count when data is cleared
        trackerCount = 0
        prefs.edit().putInt("tracker_count", 0).apply()
        updateTrackerUI()
        
        Toast.makeText(this, "Browsing data cleared successfully.", Toast.LENGTH_SHORT).show()
    }

    private fun loadUrlOrSearch(input: String) {
        val url = if (input.contains(".") && !input.contains(" ")) {
            if (input.startsWith("http://") || input.startsWith("https://")) input else "https://$input"
        } else {
            searchEngines[selectedEngineIndex].second + Uri.encode(input)
        }
        
        TabManager.updateActiveTabTitle(url, url)
        
        homepageContainer?.visibility = View.GONE
        errorContainer?.visibility = View.GONE
        bottomDock?.visibility = View.GONE // Hide dock on search
        
        addressBar?.setText(url)
        webView.loadUrl(url)
    }

    private fun showHomepageUI() {
        webView.loadUrl("about:blank")
        webView.clearHistory()
        TabManager.updateActiveTabTitle("Home", "kryptos://home")
        
        homepageContainer?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        bottomDock?.visibility = View.VISIBLE // Show dock on homepage
        
        addressBar?.setText("kryptos://home")
        progressBar?.visibility = View.GONE
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
        
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        webView.webViewClient = KryptosWebViewClient()
        webView.webChromeClient = KryptosWebChromeClient()
    }

    override fun onResume() {
        super.onResume()
        val activeTab = TabManager.getActiveTab()
        if (activeTab != null) {
            if (activeTab.url == "kryptos://home") {
                showHomepageUI()
            } else {
                if (webView.url != activeTab.url) {
                    loadUrlOrSearch(activeTab.url)
                }
            }
        }
    }

    override fun onDestroy() {
        if (this::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    // NAYA: Tracker Counter Logic
    inner class KryptosWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            for (domain in blockedDomains) {
                if (url.contains(domain)) {
                    // Tracker Blocked! Increase the count
                    trackerCount++
                    prefs.edit().putInt("tracker_count", trackerCount).apply()
                    runOnUiThread { updateTrackerUI() }
                    
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                }
            }
            return super.shouldInterceptRequest(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            addressBar?.setText(if (url != "about:blank") url else "kryptos://home")
            errorContainer?.visibility = View.GONE
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar?.visibility = View.GONE
            
            if (url != "about:blank" && url != null) {
                val title = view?.title ?: "New Tab"
                TabManager.updateActiveTabTitle(title, url)
            }
        }
        
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                lastFailedUrl = request.url.toString()
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

    // Auto-Updater Logic (Bumped to 3.0.0)
    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/RishabhIsGreat77/Kryptos-Browser/releases/latest")
                val connection = url.openConnection() as HttpsURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").replace("v", "")
                if (latestVersion > "3.0.0") {
                    val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    runOnUiThread { showUpdateDialog(latestVersion, downloadUrl) }
                }
            } catch (e: Exception) { }
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
        message.text = "A new version ($version) of Kryptos Browser is available. Update now?"
        btnConfirm.text = "Update"
        btnConfirm.setTextColor(Color.parseColor("#00E676")) // Green color for Update

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
