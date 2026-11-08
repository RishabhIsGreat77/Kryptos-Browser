package com.kryptos.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(private val list: List<HistoryItem>, private val onClick: (HistoryItem) -> Unit, private val onLongClick: (HistoryItem) -> Unit) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.bm_title)
        val url: TextView = v.findViewById(R.id.bm_url)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item.title
        holder.url.text = item.url
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { onLongClick(item); true }
    }
    override fun getItemCount() = list.size
}
