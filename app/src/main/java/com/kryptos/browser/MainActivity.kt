package com.kryptos.browser

import android.animation.ValueAnimator
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
import android.util.TypedValue
import android.view.View
import android.view.Window
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var addressBar: EditText? = null
    private var progressBar: ProgressBar? = null
    private var homepageContainer: FrameLayout? = null
    private var errorContainer: FrameLayout? = null
    private var homepageSearchInput: EditText? = null
    private var txtCurrentEngine: TextView? = null
    private var btnClearInput: ImageView? = null
    private var txtSuggestion: TextView? = null

    private var lastFailedUrl: String? = null
    private lateinit var prefs: android.content.SharedPreferences

    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var suggestionRunnable: Runnable? = null

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
        addressBar = findViewById(R.id.address_bar)
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

    // बुलेटप्रूफ ऑर्बिट एनिमेशन (लाइट/डार्क दोनों में काम करेगा)
    private fun startOrbitAnimation() {
        val dot1 = findViewById<View>(R.id.dot1) ?: return
        val dot2 = findViewById<View>(R.id.dot2) ?: return
        val dot3 = findViewById<View>(R.id.dot3) ?: return

        // हार्डकोडेड dp साइज़ (160dp कंटेनर के हिसाब से)
        val rx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics)
        val ry = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, resources.displayMetrics)

        animateDot(dot1, rx, ry, 4000, 0.0)
        animateDot(dot2, rx, ry, 5500, 60.0)
        animateDot(dot3, rx, ry, 7000, 120.0)
    }

    private fun animateDot(dot: View, rx: Float, ry: Float, duration: Long, tiltAngle: Double) {
        val animator = ValueAnimator.ofFloat(0f, 360f)
        animator.duration = duration
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val angle = Math.toRadians((animation.animatedValue as Float).toDouble())
            val x = rx * cos(angle)
            val y = ry * sin(angle)

            val tiltRad = Math.toRadians(tiltAngle)
            val rotatedX = x * cos(tiltRad) - y * sin(tiltRad)
            val rotatedY = x * sin(tiltRad) + y * cos(tiltRad)

            dot.translationX = rotatedX.toFloat()
            dot.translationY = rotatedY.toFloat()
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
        
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 KryptosBrowser/2.0"

        webView.webViewClient = KryptosWebViewClient()
        webView.webChromeClient = KryptosWebChromeClient()
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
        findViewById<ImageButton>(R.id.btn_settings)?.setOnClickListener {
            try {
                startActivity(Intent(this, SettingsActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupHomepage() {
        homepageSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClearInput?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                
                val suggestionsEnabled = prefs.getBoolean("search_suggestions", true)
                if (suggestionsEnabled && query.length > 1) {
                    fetchLiveSuggestions(query)
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

    private fun fetchLiveSuggestions(query: String) {
        suggestionRunnable?.let { suggestionHandler.removeCallbacks(it) }
        
        suggestionRunnable = Runnable {
            Thread {
                try {
                    val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=${Uri.encode(query)}")
                    val connection = url.openConnection() as HttpsURLConnection
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val jsonArray = JSONArray(response)
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    
                    if (suggestionsArray.length() > 0) {
                        val topSuggestion = suggestionsArray.getString(0)
                        runOnUiThread {
                            if (homepageSearchInput?.text.toString().trim() == query) {
                                txtSuggestion?.text = topSuggestion
                                txtSuggestion?.visibility = View.VISIBLE
                            }
                        }
                    }
                } catch (e: Exception) { }
            }.start()
        }
        suggestionHandler.postDelayed(suggestionRunnable!!, 300L)
    }

    // प्रीमियम Sci-Fi डायलॉग
    private fun showClearDataDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Privacy Shield"
        message.text = "Do you want to clear all browsing data (Cookies, Cache, and History)?"
        btnConfirm.text = "Clear Data"

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            clearBrowsingData()
        }
        dialog.show()
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
        btnConfirm.setTextColor(Color.parseColor("#60A5FA"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            downloadAndInstallApk(downloadUrl)
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
        addressBar?.setText("kryptos://home")
        progressBar?.visibility = View.GONE
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
                val latestVersion = json.getString("tag_name").replace("v", "")
                if (latestVersion > "2.0.0") {
                    val downloadUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    runOnUiThread { showUpdateDialog(latestVersion, downloadUrl) }
                }
            } catch (e: Exception) { }
        }.start()
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
            addressBar?.setText(if (url != "about:blank") url else "kryptos://home")
            errorContainer?.visibility = View.GONE
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar?.visibility = View.GONE
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
}
