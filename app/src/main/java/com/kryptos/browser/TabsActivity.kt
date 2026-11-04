package com.kryptos.browser

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TabsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TabsAdapter
    private val tabsList = mutableListOf<Tab>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)

        recyclerView = findViewById(R.id.tabs_recycler_view)
        
        // Grid Layout (2 Columns)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Dummy Tabs (Abhi ke liye, baad me MainActivity se data aayega)
        tabsList.add(Tab("1", "Google", "https://google.com", true))
        tabsList.add(Tab("2", "YouTube", "https://youtube.com", false))

        adapter = TabsAdapter(tabsList, 
            onTabClick = { tab ->
                // Jab tab par click ho, toh MainActivity ko wapas bhejna
                finish() 
            },
            onTabClose = { tab ->
                // Tab ko list se hatana
                tabsList.remove(tab)
                adapter.updateTabs(tabsList)
            }
        )
        recyclerView.adapter = adapter

        // "+" (Naya Tab) Button
        findViewById<ImageButton>(R.id.btn_new_tab).setOnClickListener {
            // Naya tab add karne ka logic (baad me add karenge)
            tabsList.add(Tab(System.currentTimeMillis().toString(), "New Tab", "kryptos://home"))
            adapter.updateTabs(tabsList)
        }
    }
}
