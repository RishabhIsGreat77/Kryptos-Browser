package com.kryptos.browser

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var txtEmpty: TextView
    private lateinit var dbHelper: HistoryDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = HistoryDatabaseHelper(this)
        recyclerView = findViewById(R.id.history_recycler_view)
        txtEmpty = findViewById(R.id.txt_empty_history)
        
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btn_clear_all_history).setOnClickListener {
            dbHelper.clearHistory()
            loadHistory()
            Toast.makeText(this, "History Cleared", Toast.LENGTH_SHORT).show()
        }
    }

    // Reload history when returning to this screen
    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val historyList = dbHelper.getAllHistory()
        
        if (historyList.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            
            val adapter = HistoryAdapter(historyList) { item ->
                TabManager.addNewTab(item.url, item.title)
                finish()
            }
            recyclerView.adapter = adapter
        }
    }
}
