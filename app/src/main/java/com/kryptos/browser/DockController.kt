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
import androidx.core.content.ContextCompat

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
            false // Let the click pass to the actual buttons
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
        bottomDock.translationY = 0f
        bottomDock.alpha = 1f

        pillContainer.visibility = View.GONE
        pillContainer.translationY = 150f
    }

    fun hideDockAndShowPill() {
        if (isHomePage || isDockHidden) return
        isDockHidden = true

        // Slide dock down and fade out
        bottomDock.animate()
            .translationY(bottomDock.height.toFloat() + 150f)
            .alpha(0f)
            .setDuration(280)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bottomDock.visibility = View.INVISIBLE
                    
                    // Show pill with jump animation
                    pillContainer.visibility = View.VISIBLE
                    pillContainer.animate()
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            }).start()
    }

    fun revealDockAndHidePill() {
        if (!isDockHidden) return
        isDockHidden = false

        // Hide pill
        pillContainer.animate()
            .translationY(150f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pillContainer.visibility = View.GONE
                    
                    // Reveal dock
                    bottomDock.visibility = View.VISIBLE
                    bottomDock.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(280)
                        .setInterpolator(DecelerateInterpolator())
                        .setListener(null)
                        .start()
                    
                    // Start timer
                    resetAutoHideTimer()
                }
            }).start()
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

    // Extract background color of the page to change pill color
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
                // colorStr example: "rgb(255, 255, 255)"
                val rgb = colorStr.replace("rgba(", "").replace("rgb(", "").replace(")", "").split(",")
                if (rgb.size >= 3) {
                    val r = rgb[0].trim().toFloat()
                    val g = rgb[1].trim().toFloat()
                    val b = rgb[2].trim().toFloat()
                    
                    // Formula to calculate brightness
                    val brightness = (r * 299 + g * 587 + b * 114) / 1000

                    if (brightness > 128) {
                        // Light background -> Use dark pill
                        pillView.setBackgroundResource(R.drawable.bg_pill_dark)
                    } else {
                        // Dark background -> Use light pill
                        pillView.setBackgroundResource(R.drawable.bg_pill_light)
                    }
                }
            } catch (e: Exception) {
                // Default fallback
                pillView.setBackgroundResource(R.drawable.bg_pill_light)
            }
        }
    }
}
