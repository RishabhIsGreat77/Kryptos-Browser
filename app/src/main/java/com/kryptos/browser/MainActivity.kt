package com.kryptos.browser

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.app.DownloadManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

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

    private var suggestionsList: ListView? = null
    private var suggestionsAdapter: ArrayAdapter<String>? = null
    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var suggestionRunnable: Runnable? = null
    private var isKeyboardVisible = false
    private var currentSuggestions = mutableListOf<String>()
    private var isUpdatingUrl = false

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences
    private var trackerCount = 0

    private lateinit var dbHelper: HistoryDatabaseHelper
    private lateinit var bookmarkDb: BookmarkDatabaseHelper
    private lateinit var downloadDb: DownloadDatabaseHelper
    private lateinit var dockController: DockController

    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceSearchDialog: Dialog? = null
    private val normalCookiesBackup = mutableMapOf<String, String>()

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var isImmersiveMode = false

    // Gemini's Fix: Atomic Boolean for Auto-Kill
    private val isWipingAndKilling = AtomicBoolean(false)

    private val searchEngines = listOf(
        Pair("Google", "https://www.google.com/search?q="),
        Pair("Bing", "https://www.bing.com/search?q="),
        Pair("DuckDuckGo", "https://duckduckgo.com/?q="),
        Pair("Brave Search", "https://search.brave.com/search?q="),
        Pair("Yahoo", "https://search.yahoo.com/search?p="),
        Pair("Ecosia", "https://www.ecosia.org/search?q=")
    )
    private var selectedEngineIndex = 0
    
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                if (shouldKillIncognito()) {
                    executeIncognitoKillNow()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)

        dbHelper = HistoryDatabaseHelper(this)
        bookmarkDb = BookmarkDatabaseHelper(this)
        downloadDb = DownloadDatabaseHelper(this)

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
        suggestionsList = findViewById(R.id.suggestions_list)

        // Reset Exclude from Recents on normal launch
        setTaskExcludedFromRecents(false)

        setupSuggestions()
        updateTrackerUI()
        updateTabCountUI()
        setupWebView()
        setupToolbar()
        setupErrorPage()
        setupBackPressHandler()

        renderSpeedDials()
        checkAndClearCache()

        dockController = DockController(
            this, 
            bottomDock!!, 
            findViewById(R.id.pill_container), 
            findViewById(R.id.pill_view), 
            webView
        )

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }
    }

    private fun shouldKillIncognito(): Boolean {
        return prefs.getBoolean("incognito_kill", false) && TabManager.hasIncognitoTabs()
    }

    // Gemini's Fix: Failsafe & Secure Flag logic
    fun updateSecureFlag() {
        val hasIncognito = TabManager.hasIncognitoTabs()
        if (hasIncognito) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(false)
            }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(true)
            }
        }
    }

    private fun setTaskExcludedFromRecents(exclude: Boolean) {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.appTasks.forEach { it.setExcludeFromRecents(exclude) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTrackerUI() {
        txtTrackerCount?.text = String.format("%,d", trackerCount)
    }
    
    private fun updateTabCountUI() {
        val countTxt = findViewById<TextView>(R.id.txt_tab_count)
        val incognitoIcon = findViewById<ImageView>(R.id.img_incognito_icon)
        
        val activeTab = TabManager.getActiveTab()

        if (activeTab?.isIncognito == true) {
            countTxt.visibility = View.GONE
            incognitoIcon.visibility = View.VISIBLE
        } else {
            countTxt.visibility = View.VISIBLE
            incognitoIcon.visibility = View.GONE
            countTxt.text = TabManager.tabsList.size.toString()
        }
        updateSecureFlag()
    }

    private fun setupSuggestions() {
        suggestionsAdapter = object : ArrayAdapter<String>(this, R.layout.item_suggestion, R.id.txt_suggestion, mutableListOf()) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val txtSuggestion = view.findViewById<TextView>(R.id.txt_suggestion)
                val btnArrow = view.findViewById<ImageView>(R.id.btn_arrow_paste)
                
                val item = getItem(position)
                txtSuggestion.text = item
                
                view.setOnClickListener {
                    isUpdatingUrl = true
                    addressBar?.setText(item)
                    isUpdatingUrl = false
                    hideSuggestions()
                    loadUrlOrSearch(item!!)
                }

                btnArrow.setOnClickListener {
                    isUpdatingUrl = true
                    addressBar?.setText(item)
                    isUpdatingUrl = false
                    addressBar?.setSelection(addressBar?.text?.length ?: 0)
                }
                return view
            }
        }
        suggestionsList?.adapter = suggestionsAdapter

        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - r.bottom
            val newKeyboardState = keypadHeight > screenHeight * 0.15
            
            if (isKeyboardVisible != newKeyboardState) {
                isKeyboardVisible = newKeyboardState
                if (addressBar?.hasFocus() == true && suggestionsList?.visibility == View.VISIBLE) {
                    val limit = if (isKeyboardVisible) 4 else 8
                    updateSuggestionsView(currentSuggestions, limit)
                }
            }
        }

        addressBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingUrl) return
                val query = s.toString().trim()
                if (query.isEmpty() || query == "kryptos://home") {
                    hideSuggestions()
                    return
                }
                if (prefs.getBoolean("search_suggestions", true)) {
                    fetchSuggestions(query)
                }
            }
        })
    }

    private fun updateSuggestionsView(suggestions: List<String>, limit: Int) {
        suggestionsAdapter?.clear()
        suggestionsAdapter?.addAll(suggestions.take(limit))
        suggestionsAdapter?.notifyDataSetChanged()
        if (suggestions.isNotEmpty()) {
            suggestionsList?.visibility = View.VISIBLE
        }
    }

    private fun fetchSuggestions(query: String) {
        suggestionRunnable?.let { suggestionHandler.removeCallbacks(it) }
        suggestionRunnable = Runnable {
            Thread {
                try {
                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                    val url = java.net.URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$encodedQuery")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val jsonArray = org.json.JSONArray(response)
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    currentSuggestions.clear()
                    for (i in 0 until suggestionsArray.length()) {
                        currentSuggestions.add(suggestionsArray.getString(i))
                    }
                    
                    runOnUiThread {
                        val limit = if (isKeyboardVisible) 4 else 8
                        updateSuggestionsView(currentSuggestions, limit)
                    }
                } catch (e: Exception) {
                    runOnUiThread { hideSuggestions() }
                }
            }.start()
        }
        suggestionHandler.postDelayed(suggestionRunnable!!, 300)
    }

    private fun hideSuggestions() {
        suggestionsList?.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                isUpdatingUrl = true
                addressBar?.setText(spokenText)
                isUpdatingUrl = false
                loadUrlOrSearch(spokenText)
            }
        }
    }

    private fun renderSpeedDials() {
        speedDialGrid?.removeAllViews()

        val finalDials = mutableListOf<HistoryItem>()

        finalDials.add(HistoryItem("GitHub", "https://github.com", 0L))
        finalDials.add(HistoryItem("Reddit", "https://reddit.com", 0L))
        finalDials.add(HistoryItem("YouTube", "https://youtube.com", 0L))

        val searchDomains = listOf("google.com", "bing.com", "duckduckgo.com", "search.brave.com", "yahoo.com", "ecosia.org")
        
        val dynamicDials = dbHelper.getMostVisitedSites(50).filter {
            val lowerUrl = it.url.lowercase()
            val isHardcoded = lowerUrl.contains("github.com") || lowerUrl.contains("reddit.com") || lowerUrl.contains("youtube.com")
            val isSearch = searchDomains.any { domain -> 
                lowerUrl.contains(domain) && (lowerUrl.contains("?q=") || lowerUrl.contains("&q=") || lowerUrl.contains("/search") || lowerUrl.contains("?p=")) 
            }
            !isHardcoded && !isSearch
        }

        finalDials.addAll(dynamicDials.take(5))

        for (dial in finalDials) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_speed_dial, speedDialGrid, false)

            val iconImg = itemView.findViewById<ImageView>(R.id.sd_icon)
            val letterText = itemView.findViewById<TextView>(R.id.sd_letter)
            val nameText = itemView.findViewById<TextView>(R.id.sd_name)

            nameText.text = dial.title

            if (dial.url.contains("github.com")) {
                iconImg.setImageResource(R.drawable.ic_github)
                iconImg.visibility = View.VISIBLE
                letterText.visibility = View.GONE
            } else if (dial.url.contains("reddit.com")) {
                iconImg.setImageResource(R.drawable.ic_reddit)
                iconImg.visibility = View.VISIBLE
                letterText.visibility = View.GONE
            } else if (dial.url.contains("youtube.com")) {
                iconImg.setImageResource(R.drawable.ic_youtube)
                iconImg.visibility = View.VISIBLE
                letterText.visibility = View.GONE
            } else {
                iconImg.visibility = View.GONE
                letterText.visibility = View.VISIBLE
                letterText.text = if (dial.title.isNotEmpty()) dial.title.substring(0, 1).uppercase() else "W"
                loadGrayscaleFavicon(dial.url, iconImg, letterText)
            }

            itemView.setOnClickListener {
                addressBar?.clearFocus()
                hideSuggestions()
                loadUrlOrSearch(dial.url)
            }

            itemView.setOnLongClickListener {
                val isHardcoded = dial.url.contains("github.com") || dial.url.contains("reddit.com") || dial.url.contains("youtube.com")
                if (!isHardcoded) {
                    showRemoveSpeedDialDialog(dial)
                } else {
                    Toast.makeText(this, "Cannot remove default sites", Toast.LENGTH_SHORT).show()
                }
                true
            }

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8)
            itemView.layoutParams = params

            speedDialGrid?.addView(itemView)
        }
    }

    private fun showRemoveSpeedDialDialog(dial: HistoryItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Remove Site"
        message.text = "Do you want to remove ${dial.title} from Speed Dial?"
        btnConfirm.text = "Remove"
        btnConfirm.setTextColor(Color.parseColor("#EF4444"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            dbHelper.removeHistoryForUrl(dial.url) 
            renderSpeedDials()
            Toast.makeText(this, "Removed from Speed Dial", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun loadGrayscaleFavicon(url: String, imageView: ImageView, textView: TextView) {
        Thread {
            try {
                val domain = Uri.parse(url).host ?: return@Thread
                val faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=64"
                val connection = java.net.URL(faviconUrl).openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                if (bitmap != null) {
                    runOnUiThread {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        textView.visibility = View.GONE

                        val colorMatrix = android.graphics.ColorMatrix()
                        colorMatrix.setSaturation(0f)
                        val filter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                        imageView.colorFilter = filter
                    }
                }
            } catch (e: Exception) { }
        }.start()
    }
    
    private fun defangUrl(url: String): String {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return url
        try {
            val uri = Uri.parse(url)
            val builder = uri.buildUpon()
            builder.clearQuery()

            val trackingParams = setOf(
                "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
                "fbclid", "gclid", "mc_eid", "yclid", "igshid", "mkt_tok"
            )

            for (param in uri.queryParameterNames) {
                if (!trackingParams.contains(param.lowercase())) {
                    val values = uri.getQueryParameters(param)
                    for (value in values) {
                        builder.appendQueryParameter(param, value)
                    }
                }
            }
            return builder.build().toString()
        } catch (e: Exception) {
            return url
        }
    }

    private fun setupToolbar() {
        val clearAddressBar = View.OnClickListener {
            if (addressBar?.text.toString() == "kryptos://home") {
                isUpdatingUrl = true
                addressBar?.setText("")
                isUpdatingUrl = false
            }
        }

        addressBar?.setOnClickListener(clearAddressBar)

        addressBar?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && addressBar?.text.toString() == "kryptos://home") {
                isUpdatingUrl = true
                addressBar?.setText("")
                isUpdatingUrl = false
            }
        }

        addressBar?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                hideSuggestions()
                val input = addressBar?.text.toString().trim()
                if (input.isNotEmpty()) loadUrlOrSearch(input)
                true
            } else false
        }

        findViewById<ImageView>(R.id.btn_voice_search)?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            } else {
                startCustomVoiceSearch()
            }
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
            if (TabManager.getActiveTab()?.isIncognito == true) {
                Toast.makeText(this, "Cannot bookmark in Incognito Mode", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
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
            popup.menu.add(0, 6, 6, "Desktop Site")
            popup.menu.add(0, 8, 8, "Downloads")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { 
                        if (TabManager.tabsList.none { it.isIncognito }) {
                            backupNormalCookies()
                            clearAllCookies()
                        }
                        val newTab = TabManager.addNewTab("kryptos://home", "Incognito Tab", true)
                        TabManager.setActiveTab(newTab.id)
                        showHomepageUI()
                        updateTabCountUI()
                        Toast.makeText(this, "Incognito Tab Opened", Toast.LENGTH_SHORT).show()
                        true 
                    }
                    2 -> { startActivity(Intent(this, HistoryActivity::class.java)); true }
                    5 -> { startActivity(Intent(this, BookmarksActivity::class.java)); true }
                    3 -> {
                        val newMode = !isDark
                        prefs.edit().putBoolean("dark_mode", newMode).apply()
                        AppCompatDelegate.setDefaultNightMode(if (newMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                        recreate(); true
                    }
                    4 -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                    6 -> {
                        toggleDesktopMode()
                        true
                    }
                    8 -> { startActivity(Intent(this, DownloadsActivity::class.java)); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupErrorPage() {
        findViewById<Button>(R.id.error_btn_retry)?.setOnClickListener {
            lastFailedUrl?.let { loadUrlOrSearch(it) }
        }
        findViewById<Button>(R.id.error_btn_home)?.setOnClickListener {
            showHomepageUI()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    return
                }
                if (isImmersiveMode) {
                    toggleImmersiveMode()
                    return
                }
                if (suggestionsList?.visibility == View.VISIBLE) {
                    hideSuggestions()
                    return
                }
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

            dbHelper.clearHistory()
            bookmarkDb.clearBookmarks()

            TabManager.tabsList.clear()
            TabManager.addNewTab("kryptos://home", "Home", false)
            updateTabCountUI()
            showHomepageUI()
            renderSpeedDials()

            trackerCount = 0
            prefs.edit().putInt("tracker_count", 0).apply()
            updateTrackerUI()
            Toast.makeText(this, "Browsing data cleared!", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun loadUrlOrSearch(input: String) {
        hideSuggestions()
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

        dockController.setHomePage(false)
        dockController.hideDockAndShowPill()

        val defangedUrl = defangUrl(url)
        isUpdatingUrl = true
        addressBar?.setText(defangedUrl)
        isUpdatingUrl = false
        webView.loadUrl(defangedUrl)
    }

    private fun showHomepageUI() {
        hideSuggestions()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        TabManager.updateActiveTabTitle("Home", "kryptos://home")
        homepageContainer?.visibility = View.VISIBLE
        errorContainer?.visibility = View.GONE
        
        dockController.setHomePage(true)

        bottomDock?.setBackgroundResource(R.drawable.bg_bottom_dock)
        bottomDock?.elevation = 12f

        isUpdatingUrl = true
        addressBar?.setText("kryptos://home")
        isUpdatingUrl = false
        progressBar?.visibility = View.GONE
        btnBookmark?.setImageResource(R.drawable.ic_star_empty)
        renderSpeedDials()
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
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
            request.setTitle(fileName)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            
            val fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName
            downloadDb.addDownload(fileName, url, fullPath)
            
            Toast.makeText(this, "Download Started...", Toast.LENGTH_SHORT).show()
        }

        val isDarkForced = prefs.getBoolean("force_dark_mode", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.isAlgorithmicDarkeningAllowed = isDarkForced
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.forceDark = if (isDarkForced) android.webkit.WebSettings.FORCE_DARK_ON else android.webkit.WebSettings.FORCE_DARK_OFF
        }
    }

    private fun checkAndClearCache() {
        Thread {
            try {
                val cacheSize = getDirSize(cacheDir)
                val sizeInMB = cacheSize / (1024 * 1024)

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

    private fun toggleDesktopMode() {
        val isDesktop = webView.settings.userAgentString.contains("Windows NT")
        if (!isDesktop) {
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
            Toast.makeText(this, "Desktop Site Enabled", Toast.LENGTH_SHORT).show()
        } else {
            webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
            Toast.makeText(this, "Mobile Site Enabled", Toast.LENGTH_SHORT).show()
        }
        webView.reload()
    }

    private fun toggleImmersiveMode() {
        isImmersiveMode = !isImmersiveMode
        if (isImmersiveMode) {
            hideUIForFullscreen()
        } else {
            showUIBackFromFullscreen()
        }
    }

    private fun hideUIForFullscreen() {
        isImmersiveMode = true
        findViewById<View>(R.id.top_bar).visibility = View.GONE
        findViewById<View>(R.id.progress_bar).visibility = View.GONE
        bottomDock?.visibility = View.GONE
        
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY 
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE 
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION 
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        
        dockController.setHomePage(false)
        dockController.hideDockAndShowPill()
    }

    private fun showUIBackFromFullscreen() {
        isImmersiveMode = false
        findViewById<View>(R.id.top_bar).visibility = View.VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        
        if (homepageContainer?.visibility == View.VISIBLE) {
            dockController.setHomePage(true)
            bottomDock?.visibility = View.VISIBLE
        } else {
            dockController.setHomePage(false)
            dockController.hideDockAndShowPill()
        }
    }

    private fun startCustomVoiceSearch() {
        addressBar?.clearFocus()
        hideSuggestions()

        voiceSearchDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        voiceSearchDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        voiceSearchDialog?.setContentView(R.layout.dialog_voice_search)
        voiceSearchDialog?.setCancelable(false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            voiceSearchDialog?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        val txtStatus = voiceSearchDialog?.findViewById<TextView>(R.id.txt_listening_status)
        val btnCancel = voiceSearchDialog?.findViewById<FrameLayout>(R.id.btn_cancel_voice)
        val outerCircle = voiceSearchDialog?.findViewById<ImageView>(R.id.img_circle_outer)
        val innerCircle = voiceSearchDialog?.findViewById<ImageView>(R.id.img_circle_inner)

        if (outerCircle != null && innerCircle != null) {
            val rotateOuter = android.animation.ObjectAnimator.ofFloat(outerCircle, "rotation", 0f, 360f).apply {
                duration = 4000; repeatCount = android.animation.ObjectAnimator.INFINITE; interpolator = android.view.animation.LinearInterpolator()
            }
            val rotateInner = android.animation.ObjectAnimator.ofFloat(innerCircle, "rotation", 360f, 0f).apply {
                duration = 3000; repeatCount = android.animation.ObjectAnimator.INFINITE; interpolator = android.view.animation.LinearInterpolator()
            }
            rotateOuter.start(); rotateInner.start()
        }

        btnCancel?.setOnClickListener {
            speechRecognizer?.stopListening()
            voiceSearchDialog?.dismiss()
        }

        voiceSearchDialog?.show()

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { txtStatus?.text = "Listening..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { txtStatus?.text = "Processing..." }
            override fun onError(error: Int) {
                voiceSearchDialog?.dismiss()
                Toast.makeText(this@MainActivity, "Didn't catch that.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                voiceSearchDialog?.dismiss()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    isUpdatingUrl = true
                    addressBar?.setText(matches[0])
                    isUpdatingUrl = false
                    loadUrlOrSearch(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    txtStatus?.text = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCustomVoiceSearch()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun backupNormalCookies() {
        normalCookiesBackup.clear()
        val cookieManager = CookieManager.getInstance()
        
        for (tab in TabManager.tabsList.filter { !it.isIncognito }) {
            val cookie = cookieManager.getCookie(tab.url)
            if (cookie != null) normalCookiesBackup[tab.url] = cookie
        }
        
        val commonSites = listOf("https://www.youtube.com", "https://github.com", "https://www.reddit.com", "https://www.google.com")
        for (site in commonSites) {
            val cookie = cookieManager.getCookie(site)
            if (cookie != null) normalCookiesBackup[site] = cookie
        }
    }

    private fun clearAllCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
        }
        WebStorage.getInstance().deleteAllData()
    }

    private fun restoreNormalCookies() {
        val cookieManager = CookieManager.getInstance()
        for ((url, cookie) in normalCookiesBackup) {
            cookieManager.setCookie(url, cookie)
        }
        cookieManager.flush()
    }

    // ==========================================
    // INCOGNITO AUTO-KILL (Gemini 3-Stage Pipeline)
    // ==========================================
    private fun executeIncognitoKillNow() {
        if (!isWipingAndKilling.compareAndSet(false, true)) return

        prefs.edit().putBoolean("wipe_on_next_launch", true).commit()

        try {
            if (this::webView.isInitialized) {
                webView.visibility = View.GONE
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.clearFormData()
                webView.clearCache(true)
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies { cookieManager.flush() }
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) { e.printStackTrace() }

        try {
            dbHelper.clearHistory()
            TabManager.clearIncognitoTabs()
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.appTasks.forEach { task ->
                task.setExcludeFromRecents(true)
                task.finishAndRemoveTask()
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        finishAndRemoveTask()

        Handler(Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }, 400)
    }

    override fun onStart() {
        super.onStart()
        if (isWipingAndKilling.get()) {
            finishAndRemoveTask()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        
        if (isWipingAndKilling.get()) {
            finishAndRemoveTask()
            return
        }

        if (prefs.getBoolean("backup_normal_data", false)) {
            backupNormalCookies()
            clearAllCookies()
            prefs.edit().putBoolean("backup_normal_data", false).apply()
        }

        if (prefs.getBoolean("wipe_incognito_data", false)) {
            clearAllCookies()
            restoreNormalCookies()
            prefs.edit().putBoolean("wipe_incognito_data", false).apply()
            
            val currentTab = TabManager.getActiveTab()
            if (currentTab?.url == "kryptos://home") {
                showHomepageUI()
            } else if (currentTab != null) {
                loadUrlOrSearch(currentTab.url)
            }
        }

        if (this::webView.isInitialized && webView.visibility != View.VISIBLE) {
            webView.visibility = View.VISIBLE
        }
        updateSecureFlag()

        val isFullscreenEnabled = prefs.getBoolean("fullscreen_mode", false)
        if (isFullscreenEnabled && !isImmersiveMode) {
            hideUIForFullscreen()
        } else if (!isFullscreenEnabled && isImmersiveMode) {
            showUIBackFromFullscreen()
        }
        
        updateTabCountUI()
        renderSpeedDials()
        
        val activeTab = TabManager.getActiveTab()
        if (activeTab != null) {
            if (activeTab.url == "kryptos://home") {
                showHomepageUI()
            } else if (webView.url != activeTab.url) {
                loadUrlOrSearch(activeTab.url)
            }
        }
    }

    override fun onPause() {
        if (TabManager.hasIncognitoTabs()) {
            window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
            if (this::webView.isInitialized) {
                webView.visibility = View.INVISIBLE
            }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive && shouldKillIncognito()) {
            executeIncognitoKillNow()
        }

        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if ((!powerManager.isInteractive || keyguardManager.isKeyguardLocked) && shouldKillIncognito()) {
            executeIncognitoKillNow()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (TabManager.hasIncognitoTabs() || isWipingAndKilling.get()) {
            outState.clear()
            return
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) { }

        TabManager.tabsList.removeAll { it.isIncognito }
        speechRecognizer?.destroy()

        if (this::webView.isInitialized) {
            try {
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
            } catch (e: Exception) {}
        }
        
        super.onDestroy()
    }

    inner class KryptosWebViewClient : WebViewClient() {
        
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            for (domain in TrackerList.blockedDomains) {
                if (url.contains(domain)) {
                    trackerCount++
                    prefs.edit().putInt("tracker_count", trackerCount).apply()
                    runOnUiThread { updateTrackerUI() }
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url.toString()
            
            val downloadExtensions = listOf(
                ".apk", ".aab", ".xapk", ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv", ".rtf", ".odt",
                ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp", ".tiff", ".heic",
                ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".3gp",
                ".mp3", ".wav", ".aac", ".flac", ".ogg", ".m4a", ".opus",
                ".exe", ".msi", ".dmg", ".iso", ".bin",
                ".epub", ".mobi", ".azw3", ".torrent", ".ttf", ".otf", ".woff", ".woff2"
            )
            
            val isDownload = downloadExtensions.any { url.lowercase().endsWith(it) } || url.contains("release-assets.githubusercontent.com")
            
            if (isDownload) {
                try {
                    val fileName = URLUtil.guessFileName(url, null, null)
                    val dmRequest = DownloadManager.Request(Uri.parse(url))
                    dmRequest.setMimeType("application/vnd.android.package-archive")
                    dmRequest.addRequestHeader("User-Agent", request?.requestHeaders?.get("User-Agent") ?: webView.settings.userAgentString)
                    dmRequest.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                    dmRequest.setTitle(fileName)
                    dmRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    dmRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(dmRequest)
                    
                    val fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName
                    downloadDb.addDownload(fileName, url, fullPath)
                    
                    Toast.makeText(this@MainActivity, "Download Started...", Toast.LENGTH_SHORT).show()
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val defangedUrl = defangUrl(url)
            if (url != defangedUrl) {
                view?.loadUrl(defangedUrl)
                return true
            }
            return false
        }
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            isUpdatingUrl = true
            addressBar?.setText(if (url != "about:blank") url else "kryptos://home")
            isUpdatingUrl = false
            errorContainer?.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar?.visibility = View.GONE

            if (url != "about:blank" && url != null) {
                val title = view?.title ?: "New Tab"
                TabManager.updateActiveTabTitle(title, url)

                val activeTab = TabManager.getActiveTab()
                if (activeTab?.isIncognito != true) {
                    val lowerUrl = url.lowercase()
                    val isSearch = lowerUrl.contains("?q=") || lowerUrl.contains("&q=") || lowerUrl.contains("/search?") || lowerUrl.contains("?p=")
                    if (!isSearch) {
                        dbHelper.addHistory(title, url)
                    }
                }

                if (bookmarkDb.isBookmarked(url)) {
                    btnBookmark?.setImageResource(R.drawable.ic_star_filled)
                } else {
                    btnBookmark?.setImageResource(R.drawable.ic_star_empty)
                }

                dockController.updatePillColor()
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

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (customView != null) {
                onHideCustomView()
                return
            }
            customView = view
            customViewCallback = callback
            
            val fullscreenContainer = findViewById<FrameLayout>(R.id.fullscreen_container)
            fullscreenContainer.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            fullscreenContainer.visibility = View.VISIBLE
            
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            hideUIForFullscreen()
        }

        override fun onHideCustomView() {
            if (customView == null) return
            val fullscreenContainer = findViewById<FrameLayout>(R.id.fullscreen_container)
            fullscreenContainer.removeView(customView)
            fullscreenContainer.visibility = View.GONE
            customView = null
            customViewCallback?.onCustomViewHidden()
            customViewCallback = null
            
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            showUIBackFromFullscreen()
        }
    }
}
