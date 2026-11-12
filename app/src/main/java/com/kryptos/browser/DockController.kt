package com.kryptos.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.webkit.WebView

class DockController(
    private val context: Context,
    private val bottomDock: View,
    private val pillContainer: View,
    private val pillView: View,
    private val webView: WebView
) {
    private val handler = Handler(Looper.getMainLooper())
    private var autoHideRunnable: Runnable? = null
    private var isDockHidden = false
    private var isHomePage = true

    init {
        setupGestures()
        
        // Reset timer when user touches the dock
        bottomDock.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                resetAutoHideTimer()
            }
            false 
        }
    }

    fun setHomePage(isHome: Boolean) {
        isHomePage = isHome
        if (isHome) {
            stopTimer()
            showDockImmediately()
        }
    }

    private fun showDockImmediately() {
        isDockHidden = false
        bottomDock.visibility = View.VISIBLE
        bottomDock.animate().cancel()
        bottomDock.translationY = 0f
        bottomDock.alpha = 1f

        pillContainer.visibility = View.GONE
        pillContainer.translationY = 150f
    }

    fun hideDockAndShowPill() {
        if (isHomePage || isDockHidden) return
        isDockHidden = true
        stopTimer()

        bottomDock.animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(280)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                bottomDock.visibility = View.INVISIBLE
                
                pillContainer.visibility = View.VISIBLE
                pillContainer.bringToFront()
                pillContainer.animate()
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }.start()
    }

    fun revealDockAndHidePill() {
        if (!isDockHidden) return
        isDockHidden = false

        pillContainer.animate()
            .translationY(150f)
            .setDuration(200)
            .withEndAction {
                pillContainer.visibility = View.GONE
                
                bottomDock.visibility = View.VISIBLE
                bottomDock.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(280)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        // Timer starts only after the dock fully animates up
                        resetAutoHideTimer()
                    }.start()
            }.start()
    }

    fun resetAutoHideTimer() {
        if (isHomePage || isDockHidden) return
        stopTimer()
        autoHideRunnable = Runnable { hideDockAndShowPill() }
        handler.postDelayed(autoHideRunnable!!, 3000) // 3 seconds
    }

    fun stopTimer() {
        autoHideRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun setupGestures() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e1.y - e2.y > 50) {
                    revealDockAndHidePill()
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                revealDockAndHidePill()
                return true
            }
        })

        pillContainer.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    fun updatePillColor() {
        webView.evaluateJavascript(
            "(function() { " +
            "var c = window.getComputedStyle(document.body).backgroundColor; " +
            "if (c === 'rgba(0, 0, 0, 0)' || c === 'transparent') { " +
            "c = window.getComputedStyle(document.documentElement).backgroundColor; " +
            "} " +
            "return c; " +
            "})();"
        ) { colorStr ->
            try {
                val cleanColor = colorStr.replace("\"", "")
                val rgb = cleanColor.replace("rgba(", "").replace("rgb(", "").replace(")", "").split(",")
                
                if (rgb.size >= 3) {
                    val r = rgb[0].trim().toFloatOrNull() ?: 0f
                    val g = rgb[1].trim().toFloatOrNull() ?: 0f
                    val b = rgb[2].trim().toFloatOrNull() ?: 0f
                    
                    val brightness = (r * 299 + g * 587 + b * 114) / 1000

                    if (brightness > 128) {
                        pillView.setBackgroundResource(R.drawable.bg_pill_dark)
                    } else {
                        pillView.setBackgroundResource(R.drawable.bg_pill_light)
                    }
                }
            } catch (e: Exception) {
                pillView.setBackgroundResource(R.drawable.bg_pill_light)
            }
        }
    }
}
