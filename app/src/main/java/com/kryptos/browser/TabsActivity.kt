package com.kryptos.browser

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TabsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        recyclerView = findViewById(R.id.tabs_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = TabsAdapter(TabManager.tabsList,
            onTabClick = { tab ->
                TabManager.setActiveTab(tab.id)
                finish() 
            },
            onTabClose = { tab ->
                TabManager.removeTab(tab)
                adapter.updateTabs(TabManager.tabsList)
            }
        )
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btn_new_tab).setOnClickListener {
            TabManager.addNewTab("kryptos://home", "New Tab")
            adapter.updateTabs(TabManager.tabsList)
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }
}
