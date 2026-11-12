package com.kryptos.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadsAdapter(
    private val downloads: List<DownloadItem>,
    private val onClick: (DownloadItem) -> Unit,
    private val onLongClick: (DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val filename: TextView = view.findViewById(R.id.dl_filename)
        val date: TextView = view.findViewById(R.id.dl_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = downloads[position]
        holder.filename.text = item.filename
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        holder.date.text = dateFormat.format(Date(item.timestamp))

        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener { 
            onLongClick(item)
            true 
        }
    }

    override fun getItemCount(): Int = downloads.size
}
