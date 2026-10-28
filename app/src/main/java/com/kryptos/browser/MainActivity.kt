package com.kryptos.browser

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var addressBarText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var homepageContainer: FrameLayout? = null
    private var errorContainer: FrameLayout? = null
    private var homepageSearchInput: EditText? = null
    private var txtCurrentEngine: TextView? = null
    private var btnClearInput: ImageView? = null
    private var txtSuggestion: TextView? = null

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences

    // सभी 6 सर्च इंजन वापस आ गए
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
        homepageSearchInput = findViewById(R.id.homepage_search_input)
        txtCurrentEngine = findViewById(R.id.txt_current_engine)
        btnClearInput = findViewById(R.id.btn_clear_input)
        txtSuggestion = findViewById(R.id.txt_suggestion)

        updateEngineText()
        setupWebView()
        setupToolbar()
        setupHomepage()
        setupBackPressHandler()
        startOrbitAnimation()

        checkForUpdates()
    }

    // सही मैथ के साथ ऑर्बिट एनिमेशन (डायनामिक साइज़)
    private fun startOrbitAnimation() {
        val container = findViewById<FrameLayout>(R.id.logo_container) ?: return
        val dot1 = findViewById<View>(R.id.dot1) ?: return
        val dot2 = findViewById<View>(R.id.dot2) ?: return
        val dot3 = findViewById<View>(R.id.dot3) ?: return

        // Container का साइज़ मिलने का इंतज़ार करें
        container.post {
            val width = container.width.toFloat()
            // SVG डिजाइन के हिसाब से सही रेडियस (rx=30, ry=8 out of 96)
            val rx = width * (30f / 96f)
            val ry = width * (8f / 96f)

            animateDot(dot1, rx, ry, 4000, 0f)
            animateDot(dot2, rx, ry, 5500, 60f)
            animateDot(dot3, rx, ry, 7000, 120f)
        }
    }

    private fun animateDot(dot: View, rx: Float, ry: Float, duration: Long, startAngle: Float) {
        val animator = ValueAnimator.ofFloat(0f, 360f)
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val angle = Math.toRadians((animation.animatedValue as Float + startAngle).toDouble())
            dot.translationX = (rx * cos(angle)).toFloat()
            dot.translationY = (ry * sin(angle)).toFloat()
        }
        animator.start()
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
        
        // Desktop User-Agent (ताकि YouTube पुराना वर्ज़न न खोले)
        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

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
        findViewById<ImageButton>(R.id.btn_fullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
        
        // Settings बटन (अब क्रैश नहीं होगा क्योंकि हमने इसे Manifest में जोड़ा है)
        findViewById<ImageButton>(R.id.btn_settings)?.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
    }

    private fun toggleFullscreen() {
        val attrs = window.attributes
        if (attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun setupHomepage() {
        homepageSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearInput?.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                val suggestionsEnabled = prefs.getBoolean("search_suggestions", true)
                if (suggestionsEnabled && s != null && s.length > 2) {
                    txtSuggestion?.text = "Trending search: $s ..."
                    txtSuggestion?.visibility = View.VISIBLE
                } else {
                    txtSuggestion?.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearInput?.setOnClickListener {
            homepageSearchInput?.text?.clear()
            txtSuggestion?.visibility = View.GONE
        }

        homepageSearchInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = homepageSearchInput?.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrlOrSearch(input)
                    homepageSearchInput?.text?.clear()
                    txtSuggestion?.visibility = View.GONE
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

    private fun showAddressDialog() {
        val dialogLayout = LinearLayout(this)
        dialogLayout.orientation = LinearLayout.VERTICAL
        dialogLayout.setPadding(50, 40, 50, 10)
        val input = EditText(this)
        input.setText(if (webView.url == "about:blank") "" else webView.url)
        input.setSelection(input.text.length)
        dialogLayout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("Enter URL or Search")
            .setView(dialogLayout)
            .setPositiveButton("Go") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) loadUrlOrSearch(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Shield")
            .setMessage("Do you want to clear all browsing data?")
            .setPositiveButton("Clear Data") { _, _ -> clearBrowsingData() }
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
            if (input.startsWith("http://") || input.startsWith("https://")) input else "https://$input"
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

    override fun onDestroy() {
        if (this::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    // Auto-Updater Logic
    private fun checkForUpdates() {
        Thread {
            try {
                val url = URL("https://api.github.com/repos/RishabhIsGreat77/Kryptos-Browser/releases/latest")
                val connection = url.openConnection() as HttpsURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val latestVersion = json.getString("tag_name").replace("v", "")
                if (latestVersion > "2.0.0") {
                    val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    runOnUiThread { showUpdateDialog(latestVersion, downloadUrl) }
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun showUpdateDialog(version: String, downloadUrl: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version ($version) is available. Update now?")
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

    inner class KryptosWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            addressBarText?.text = if (url != "about:blank") url else "kryptos://home"
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
