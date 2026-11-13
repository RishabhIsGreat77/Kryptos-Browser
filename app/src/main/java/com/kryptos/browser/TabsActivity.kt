package com.kryptos.browser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TabsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TabsAdapter
    private lateinit var btnNormal: TextView
    private lateinit var btnIncognito: TextView
    private lateinit var layoutNormal: LinearLayout
    private lateinit var layoutIncognito: LinearLayout
    private lateinit var btnAddNormal: ImageButton
    private lateinit var btnAddIncognito: ImageButton
    private lateinit var txtEmptyTabs: TextView

    private var isIncognitoMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        recyclerView = findViewById(R.id.tabs_recycler_view)
        btnNormal = findViewById(R.id.btn_tab_normal)
        btnIncognito = findViewById(R.id.btn_tab_incognito)
        layoutNormal = findViewById(R.id.layout_tab_normal)
        layoutIncognito = findViewById(R.id.layout_tab_incognito)
        btnAddNormal = findViewById(R.id.btn_add_normal_tab)
        btnAddIncognito = findViewById(R.id.btn_add_incognito_tab)
        txtEmptyTabs = findViewById(R.id.txt_empty_tabs)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Auto-detect mode based on currently active tab
        adapter = TabsAdapter(mutableListOf(),
            onTabClick = { tab ->
                TabManager.setActiveTab(tab.id)
                finish()
            },
            onTabClose = { tab ->
                TabManager.removeTab(tab)
                // NEW: If the closed tab was Incognito, and no other Incognito tabs remain, trigger a data wipe
                if (tab.isIncognito && TabManager.tabsList.none { it.isIncognito }) {
                    val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("wipe_incognito_data", true).apply()
                }
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

        // Normal Tab Add Button
        btnAddNormal.setOnClickListener {
            val newTab = TabManager.addNewTab("kryptos://home", "New Tab", false)
            TabManager.setActiveTab(newTab.id)
            finish()
        }

        // Incognito Tab Add Button
        btnAddIncognito.setOnClickListener {
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
            layoutIncognito.setBackgroundResource(R.drawable.bg_speed_dial_box)
            btnIncognito.setTextColor(Color.parseColor("#FFFFFF"))
            btnAddIncognito.imageTintList = ContextCompat.getColorStateList(this, R.color.kryptos_text_primary)
            
            layoutNormal.setBackgroundColor(Color.TRANSPARENT)
            btnNormal.setTextColor(Color.parseColor("#8B8B93"))
            btnAddNormal.imageTintList = ContextCompat.getColorStateList(this, R.color.kryptos_text_secondary)
        } else {
            layoutNormal.setBackgroundResource(R.drawable.bg_speed_dial_box)
            btnNormal.setTextColor(Color.parseColor("#FFFFFF"))
            btnAddNormal.imageTintList = ContextCompat.getColorStateList(this, R.color.kryptos_text_primary)
            
            layoutIncognito.setBackgroundColor(Color.TRANSPARENT)
            btnIncognito.setTextColor(Color.parseColor("#8B8B93"))
            btnAddIncognito.imageTintList = ContextCompat.getColorStateList(this, R.color.kryptos_text_secondary)
        }
    }

    private fun updateTabList() {
        val filteredTabs = TabManager.tabsList.filter { it.isIncognito == isIncognitoMode }.toMutableList()
        
        if (filteredTabs.isEmpty()) {
            txtEmptyTabs.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            txtEmptyTabs.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
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
