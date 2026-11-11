package com.kryptos.browser

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TabsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TabsAdapter
    private lateinit var btnNormal: TextView
    private lateinit var btnIncognito: TextView

    private var isIncognitoMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        recyclerView = findViewById(R.id.tabs_recycler_view)
        btnNormal = findViewById(R.id.btn_tab_normal)
        btnIncognito = findViewById(R.id.btn_tab_incognito)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = TabsAdapter(mutableListOf(),
            onTabClick = { tab ->
                TabManager.setActiveTab(tab.id)
                finish()
            },
            onTabClose = { tab ->
                TabManager.removeTab(tab)
                updateTabList()
            }
        )
        recyclerView.adapter = adapter

        // Tab mode switch listeners
        btnNormal.setOnClickListener {
            isIncognitoMode = false
            updateTabSelectorUI()
            updateTabList()
        }

        btnIncognito.setOnClickListener {
            isIncognitoMode = true
            updateTabSelectorUI()
            updateTabList()
        }

        // New Tab (+) Button logic (Creates tab based on current mode)
        findViewById<ImageButton>(R.id.btn_new_tab).setOnClickListener {
            val title = if (isIncognitoMode) "Incognito Tab" else "New Tab"
            val newTab = TabManager.addNewTab("kryptos://home", title, isIncognitoMode)
            TabManager.setActiveTab(newTab.id)
            finish()
        }

        // Dedicated Incognito Tab Button (Forces incognito mode)
        findViewById<ImageButton>(R.id.btn_new_incognito_tab).setOnClickListener {
            isIncognitoMode = true
            updateTabSelectorUI()
            val newTab = TabManager.addNewTab("kryptos://home", "Incognito Tab", true)
            TabManager.setActiveTab(newTab.id)
            finish()
        }

        // Initial setup
        updateTabSelectorUI()
        updateTabList()
    }

    private fun updateTabSelectorUI() {
        if (isIncognitoMode) {
            btnIncognito.setBackgroundResource(R.drawable.bg_speed_dial_box)
            btnIncognito.setTextColor(Color.parseColor("#FFFFFF"))
            btnNormal.setBackgroundColor(Color.TRANSPARENT)
            btnNormal.setTextColor(Color.parseColor("#8B8B93"))
        } else {
            btnNormal.setBackgroundResource(R.drawable.bg_speed_dial_box)
            btnNormal.setTextColor(Color.parseColor("#FFFFFF"))
            btnIncognito.setBackgroundColor(Color.TRANSPARENT)
            btnIncognito.setTextColor(Color.parseColor("#8B8B93"))
        }
    }

    private fun updateTabList() {
        // Filter tabs based on current mode
        val filteredTabs = TabManager.tabsList.filter { it.isIncognito == isIncognitoMode }.toMutableList()
        
        // If no tabs in this mode, add a default one
        if (filteredTabs.isEmpty()) {
            val title = if (isIncognitoMode) "Incognito Tab" else "New Tab"
            TabManager.addNewTab("kryptos://home", title, isIncognitoMode)
            filteredTabs.add(TabManager.tabsList.last())
        }
        
        adapter.updateTabs(filteredTabs)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}
