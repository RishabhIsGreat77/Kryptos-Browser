package com.kryptos.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var addressBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var homepageContainer: FrameLayout
    private lateinit var errorContainer: FrameLayout
    private lateinit var errorTitle: TextView
    private lateinit var errorMessage: TextView
    private lateinit var homepageSearchInput: EditText

    private var lastFailedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load theme preference before setting content view
        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_main)

        // Bind UI elements
        webView = findViewById(R.id.webview)
        addressBar = findViewById(R.id.address_bar)
        progressBar = findViewById(R.id.progress_bar)
        homepageContainer = findViewById(R.id.homepage_container)
        errorContainer = findViewById(R.id.error_container)
        errorTitle = findViewById(R.id.error_title)
        errorMessage = findViewById(R.id.error_message)
        homepageSearchInput = findViewById(R.id.homepage_search_input)

        setupWebView()
        setupToolbar()
        setupHomepage()
        setupErrorPage()
        setupBackPressHandler()
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

        webView.webViewClient = KryptosWebViewClient()
        webView.webChromeClient = KryptosWebChromeClient()
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btn_forward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btn_reload).setOnClickListener {
            if (webView.url != null) webView.reload()
        }
        findViewById<ImageButton>(R.id.btn_home).setOnClickListener {
            showHomepage()
        }
        
        // Menu button setup
        findViewById<ImageButton>(R.id.btn_menu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_browser, popup.menu)
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

        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = addressBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrlOrSearch(input)
                }
                true
            } else false
        }
    }

    private fun setupHomepage() {
        homepageSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val input = homepageSearchInput.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrlOrSearch(input)
                    homepageSearchInput.text.clear()
                }
                true
            } else false
        }
    }

    private fun setupErrorPage() {
        findViewById<Button>(R.id.error_btn_retry).setOnClickListener {
            lastFailedUrl?.let { loadUrlOrSearch(it) }
        }
        findViewById<Button>(R.id.error_btn_home).setOnClickListener {
            showHomepage()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    errorContainer.visibility == View.VISIBLE -> showHomepage()
                    homepageContainer.visibility == View.VISIBLE -> finish()
                    webView.canGoBack() -> webView.goBack()
                    else -> finish()
                }
            }
        })
    }

    private fun loadUrlOrSearch(input: String) {
        val url = if (input.contains(".") && !input.contains(" ")) {
            if (input.startsWith("http://") || input.startsWith("https://")) {
                input
            } else {
                "https://$input"
            }
        } else {
            "https://www.google.com/search?q=${Uri.encode(input)}"
        }

        homepageContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE
        addressBar.setText(url)
        webView.loadUrl(url)
    }

    private fun showHomepage() {
        webView.loadUrl("about:blank")
        homepageContainer.visibility = View.VISIBLE
        errorContainer.visibility = View.GONE
        addressBar.text.clear()
        progressBar.visibility = View.GONE
    }

    private fun toggleTheme() {
        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        val newMode = !isDark

        prefs.edit().putBoolean("dark_mode", newMode).apply()

        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage("${getString(R.string.about_description)}\n\n${getString(R.string.about_version_label)}: 1.0.0")
            .setPositiveButton(R.string.about_close, null)
            .show()
    }

    override fun onDestroy() {
        if (this::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    // Inner classes for WebView callbacks
    inner class KryptosWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != "about:blank") {
                addressBar.setText(url)
            } else {
                addressBar.text.clear()
            }
            errorContainer.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                val desc = error?.description?.toString() ?: "Unknown error"
                val msg = getString(R.string.error_message_generic) + "\n\n($desc)"
                lastFailedUrl = request.url.toString()
                
                errorTitle.text = getString(R.string.error_title)
                errorMessage.text = msg
                errorContainer.visibility = View.VISIBLE
                homepageContainer.visibility = View.GONE
            }
        }
    }

    inner class KryptosWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress < 100) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = newProgress
            } else {
                progressBar.visibility = View.GONE
            }
        }
    }
}

