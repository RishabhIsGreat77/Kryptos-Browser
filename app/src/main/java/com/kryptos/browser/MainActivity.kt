package com.kryptos.browser

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
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
import org.json.JSONArray
import org.json.JSONObject
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

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences
    private var trackerCount = 0
    
    // NAYA: Database Helper
    private lateinit var dbHelper: HistoryDatabaseHelper

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

        // Initialize DB
        dbHelper = HistoryDatabaseHelper(this)

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

        updateTrackerUI()
        updateTabCountUI()
        setupWebView()
        setupToolbar()
        setupBackPressHandler()
        
        renderSpeedDials()
    }

    private fun updateTrackerUI() {
        txtTrackerCount?.text = String.format("%,d", trackerCount)
    }

    private fun updateTabCountUI() {
        txtTabCount?.text = TabManager.tabsList.size.toString()
    }

    // ==========================================
    // SPEED DIAL LOGIC
    // ==========================================
    private fun renderSpeedDials() {
        speedDialGrid?.removeAllViews()
        
        var dials = loadSpeedDials()
        if (dials.isEmpty()) {
            dials.add(Pair("GitHub", "https://github.com"))
            dials.add(Pair("Reddit", "https://reddit.com"))
            dials.add(Pair("YouTube", "https://youtube.com"))
            saveSpeedDials(dials)
        }

        for ((index, dial) in dials.withIndex()) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_speed_dial, speedDialGrid, false)
            
            val iconImg = itemView.findViewById<ImageView>(R.id.sd_icon)
            val letterText = itemView.findViewById<TextView>(R.id.sd_letter)
            val nameText = itemView.findViewById<TextView>(R.id.sd_name)

            nameText.text = dial.first

            when (dial.first.lowercase()) {
                "github" -> {
                    iconImg.setImageResource(R.drawable.ic_github)
                    iconImg.visibility = View.VISIBLE
                    letterText.visibility = View.GONE
                }
                "reddit" -> {
                    iconImg.setImageResource(R.drawable.ic_reddit)
                    iconImg.visibility = View.VISIBLE
                    letterText.visibility = View.GONE
                }
                "youtube" -> {
                    iconImg.setImageResource(R.drawable.ic_youtube)
                    iconImg.visibility = View.VISIBLE
                    letterText.visibility = View.GONE
                }
                else -> {
                    iconImg.visibility = View.GONE
                    letterText.visibility = View.VISIBLE
                    letterText.text = dial.first.substring(0, 1).uppercase()
                }
            }

            itemView.setOnClickListener {
                loadUrlOrSearch(dial.second)
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8)
            itemView.layoutParams = params

            speedDialGrid?.addView(itemView)
        }

        val addBtnView = LayoutInflater.from(this).inflate(R.layout.item_speed_dial, speedDialGrid, false)
        val addIcon = addBtnView.findViewById<ImageView>(R.id.sd_icon)
        val addLetter = addBtnView.findViewById<TextView>(R.id.sd_letter)
        val addName = addBtnView.findViewById<TextView>(R.id.sd_name)
        
        addIcon.visibility = View.GONE
        addLetter.visibility = View.VISIBLE
        addLetter.text = "+"
        addLetter.setTextColor(Color.parseColor("#8B8B93"))
        addName.text = "Add"
        
        addBtnView.setOnClickListener {
            showAddSpeedDialDialog()
        }

        val addParams = GridLayout.LayoutParams()
        addParams.width = 0
        addParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        addParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        addParams.setMargins(8, 8, 8, 8)
        addBtnView.layoutParams = addParams

        speedDialGrid?.addView(addBtnView)
    }

    private fun showAddSpeedDialDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_add_speed_dial)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etName = dialog.findViewById<EditText>(R.id.et_sd_name)
        val etUrl = dialog.findViewById<EditText>(R.id.et_sd_url)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_cancel_sd)
        val btnSave = dialog.findViewById<TextView>(R.id.btn_save_sd)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            var url = etUrl.text.toString().trim()

            if (name.isNotEmpty() && url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }

                val dials = loadSpeedDials()
                dials.add(Pair(name, url))
                saveSpeedDials(dials)
                renderSpeedDials()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter both Name and URL", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun loadSpeedDials(): MutableList<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val jsonStr = prefs.getString("speed_dials", "[]")
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Pair(obj.getString("name"), obj.getString("url")))
            }
        } catch (e: Exception) { }
        return list
    }

    private fun saveSpeedDials(dials: List<Pair<String, String>>) {
        val jsonArray = JSONArray()
        for (dial in dials) {
            val obj = JSONObject()
            obj.put("name", dial.first)
            obj.put("url", dial.second)
            jsonArray.put(obj)
        }
        prefs.edit().putString("speed_dials", jsonArray.toString()).apply()
    }

    // ==========================================

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
            popup.menu.add(0, 3, 3, themeText)
            popup.menu.add(0, 4, 4, "Settings")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { Toast.makeText(this, "Incognito Mode coming in V4!", Toast.LENGTH_SHORT).show(); true }
                    2 -> { 
                        // NAYA: History Screen Open karo
                        startActivity(Intent(this, HistoryActivity::class.java))
                        true 
                    }
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
            
            // NAYA: Database ki history bhi clear karo
            dbHelper.clearHistory()
            
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
    }

    override fun onResume() {
        super.onResume()
        updateTabCountUI()
        val activeTab = TabManager.getActiveTab()
        if (activeTab != null) {
            if (activeTab.url == "kryptos://home") {
                showHomepageUI()
            } else if (webView.url != activeTab.url) {
                loadUrlOrSearch(activeTab.url)
            }
        }
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
                
                // NAYA: History me save karo
                dbHelper.addHistory(title, url)
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
