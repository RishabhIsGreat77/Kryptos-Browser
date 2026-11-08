package com.kryptos.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val historyList: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.history_title)
        val url: TextView = view.findViewById(R.id.history_url)
        val time: TextView = view.findViewById(R.id.history_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        
        holder.title.text = if (item.title.isNullOrEmpty()) "Untitled Page" else item.title
        holder.url.text = item.url
        
        // Date formatting (e.g., 12 Nov, 10:30 AM)
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.time.text = sdf.format(Date(item.timestamp))

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = historyList.size
}
