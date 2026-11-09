package com.kryptos.browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import java.io.File
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.URLUtil
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
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var addressBar: EditText? = null
    private var progressBar: ProgressBar? = null
    private var homepageContainer: FrameLayout? = null
    private var errorContainer: FrameLayout? = null
    private var bottomDock: LinearLayout? = null
    private var txtTrackerCount: TextView? = null
    private var txtTabCount: TextView? = null
    private var btnEngineSwitch: ImageView? = null
    private var speedDialGrid: GridLayout? = null
    private var btnBookmark: ImageView? = null

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences
    private var trackerCount = 0
    
    private lateinit var dbHelper: HistoryDatabaseHelper
    private lateinit var bookmarkDb: BookmarkDatabaseHelper

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

        dbHelper = HistoryDatabaseHelper(this)
        bookmarkDb = BookmarkDatabaseHelper(this)

        selectedEngineIndex = prefs.getInt("default_search_engine", 0)
        trackerCount = prefs.getInt("tracker_count", 0)

        webView = findViewById(R.id.webview)
        addressBar = findViewById(R.id.address_bar)
        progressBar = findViewById(R.id.progress_bar)
        homepageContainer = findViewById(R.id.homepage_container)
        errorContainer = findViewById(R.id.error_container)
        bottomDock = findViewById(R.id.bottom_dock)
        txtTrackerCount = findViewById(R.id.txt_tracker_count)
        txtTabCount = findViewById(R.id.txt_tab_count)
        btnEngineSwitch = findViewById(R.id.btn_engine_switch)
        speedDialGrid = findViewById(R.id.speed_dial_grid)
        btnBookmark = findViewById(R.id.btn_bookmark)

        updateTrackerUI()
        updateTabCountUI()
        setupWebView()
        setupToolbar()
        setupBackPressHandler()
        
        renderSpeedDials()
        checkAndClearCache()
    }

    private fun updateTrackerUI() {
        txtTrackerCount?.text = String.format("%,d", trackerCount)
    }

    private fun updateTabCountUI() {
        txtTabCount?.text = TabManager.tabsList.size.toString()
    }

    // ==========================================
    // AUTO SPEED DIAL LOGIC (NEW)
    // ==========================================
    private fun renderSpeedDials() {
        speedDialGrid?.removeAllViews()
        
        // Database se top 4 sites uthao
        var dials = dbHelper.getMostVisitedSites(4)
        
        // Agar user naya hai (history khali hai), toh Default sites dikhao
        if (dials.isEmpty()) {
            dials = listOf(
                HistoryItem("GitHub", "https://github.com", 0L),
                HistoryItem("Reddit", "https://reddit.com", 0L),
                HistoryItem("YouTube", "https://youtube.com", 0L)
            )
        }

        for ((index, dial) in dials.withIndex()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_speed_dial, speedDialGrid, false)
            
            val iconImg = itemView.findViewById<ImageView>(R.id.sd_icon)
            val letterText = itemView.findViewById<TextView>(R.id.sd_letter)
            val nameText = itemView.findViewById<TextView>(R.id.sd_name)

            nameText.text = dial.title

            // Icon Logic (Default sites ka logo, nayi sites ka First Letter)
            when (dial.title.lowercase()) {
                "github" -> { iconImg.setImageResource(R.drawable.ic_github); iconImg.visibility = View.VISIBLE; letterText.visibility = View.GONE }
                "reddit" -> { iconImg.setImageResource(R.drawable.ic_reddit); iconImg.visibility = View.VISIBLE; letterText.visibility = View.GONE }
                "youtube" -> { iconImg.setImageResource(R.drawable.ic_youtube); iconImg.visibility = View.VISIBLE; letterText.visibility = View.GONE }
                else -> {
                    iconImg.visibility = View.GONE
                    letterText.visibility = View.VISIBLE
                    letterText.text = if(dial.title.isNotEmpty()) dial.title.substring(0, 1).uppercase() else "W"
                }
            }

            itemView.setOnClickListener {
                loadUrlOrSearch(dial.url)
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8)
            itemView.layoutParams = params

            speedDialGrid?.addView(itemView)
        }
        // Note: "+ Add" button ka code yahan se hata diya gaya hai
    }

    // ==========================================

    private fun setupToolbar() {
        val clearAddressBar = View.OnClickListener {
            if (addressBar?.text.toString() == "kryptos://home") {
                addressBar?.setText("")
            }
        }
        
        addressBar?.setOnClickListener(clearAddressBar)
        
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

        btnBookmark?.setOnClickListener {
            val currentUrl = webView.url
            val currentTitle = webView.title ?: "Untitled"
            
            if (currentUrl != null && currentUrl != "about:blank" && !currentUrl.startsWith("kryptos://")) {
                if (bookmarkDb.isBookmarked(currentUrl)) {
                    bookmarkDb.removeBookmark(currentUrl)
                    btnBookmark?.setImageResource(R.drawable.ic_star_empty)
                    Toast.makeText(this, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                } else {
                    bookmarkDb.addBookmark(currentTitle, currentUrl)
                    btnBookmark?.setImageResource(R.drawable.ic_star_filled)
                    Toast.makeText(this, "Bookmark Added", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Cannot bookmark this page", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.btn_reload)?.setOnClickListener { webView.reload() }
        findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        findViewById<ImageButton>(R.id.btn_home)?.setOnClickListener { showHomepageUI() }
        findViewById<ImageButton>(R.id.btn_fire)?.setOnClickListener { showClearDataDialog() }

        findViewById<View>(R.id.btn_tabs)?.setOnClickListener {
            startActivity(Intent(this, TabsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_menu)?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            val isDark = prefs.getBoolean("dark_mode", true)
            val themeText = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
            
            popup.menu.add(0, 1, 1, "Incognito Mode")
            popup.menu.add(0, 2, 2, "History")
            popup.menu.add(0, 5, 5, "Bookmarks")
            popup.menu.add(0, 3, 3, themeText)
            popup.menu.add(0, 4, 4, "Settings")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { Toast.makeText(this, "Incognito Mode coming in V4!", Toast.LENGTH_SHORT).show(); true }
                    2 -> { startActivity(Intent(this, HistoryActivity::class.java)); true }
                    5 -> { startActivity(Intent(this, BookmarksActivity::class.java)); true }
                    3 -> {
                        val newMode = !isDark
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                        AppCompatDelegate.setDefaultNightMode(if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                        recreate(); true
                    }
                    4 -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (homepageContainer?.visibility == View.VISIBLE) return
                if (webView.canGoBack()) webView.goBack() else showHomepageUI()
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
        message.text = "Do you want to clear all browsing data?"
        btnConfirm.text = "Clear Data"

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            webView.clearCache(true)
            webView.clearHistory()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)
            
            dbHelper.clearHistory() // Auto speed dial bhi clear ho jayega
            
            TabManager.tabsList.clear()
            TabManager.addNewTab("kryptos://home", "Home")
            updateTabCountUI()
            showHomepageUI()
            
            trackerCount = 0
            prefs.edit().putInt("tracker_count", 0).apply()
            updateTrackerUI()
            Toast.makeText(this, "Browsing data cleared!", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
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
        
        bottomDock?.setBackgroundColor(Color.TRANSPARENT)
        bottomDock?.elevation = 0f
        
        addressBar?.setText(url)
        webView.loadUrl(url)
    }

    private fun showHomepageUI() {
        webView.loadUrl("about:blank")
        webView.clearHistory()
        TabManager.updateActiveTabTitle("Home", "kryptos://home")
        homepageContainer?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        
        bottomDock?.setBackgroundResource(R.drawable.bg_bottom_dock)
        bottomDock?.elevation = 12f
        
        addressBar?.setText("kryptos://home")
        progressBar?.visibility = View.GONE
        btnBookmark?.setImageResource(R.drawable.ic_star_empty)
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
        
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        
        webView.webViewClient = KryptosWebViewClient()
        webView.webChromeClient = KryptosWebChromeClient()

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "Download Started...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTabCountUI()
        renderSpeedDials() // Resume par speed dial update hoga
        
        val activeTab = TabManager.getActiveTab()
        if (activeTab != null) {
            if (activeTab.url == "kryptos://home") {
                showHomepageUI()
            } else if (webView.url != activeTab.url) {
                loadUrlOrSearch(activeTab.url)
            }
        }
    }

        // NAYA: Auto Cache Cleaner Logic
    private fun checkAndClearCache() {
        Thread {
            try {
                val cacheSize = getDirSize(cacheDir)
                val sizeInMB = cacheSize / (1024 * 1024)
                
                // Agar cache 100MB se zyada hai toh clear karo
                if (sizeInMB > 100) {
                    runOnUiThread {
                        webView.clearCache(true)
                        WebStorage.getInstance().deleteAllData()
                        Toast.makeText(this, "Cache auto-cleared to save space!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        try {
            for (file in dir.listFiles()!!) {
                size += if (file.isFile) file.length() else getDirSize(file)
            }
        } catch (e: Exception) { }
        return size
    }

    override fun onDestroy() {
        if (this::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    inner class KryptosWebViewClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            for (domain in blockedDomains) {
                if (url.contains(domain)) {
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
                dbHelper.addHistory(title, url) // History aur Visit Count update hoga
                
                if (bookmarkDb.isBookmarked(url)) {
                    btnBookmark?.setImageResource(R.drawable.ic_star_filled)
                } else {
                    btnBookmark?.setImageResource(R.drawable.ic_star_empty)
                }
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
            if (newProgress < 100) { progressBar?.visibility = View.VISIBLE; progressBar?.progress = newProgress }
            else { progressBar?.visibility = View.GONE }
        }
    }
}
