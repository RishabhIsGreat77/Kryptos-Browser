package com.kryptos.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TabsAdapter(
    private var tabs: MutableList<Tab>,
    private val onTabClick: (Tab) -> Unit,
    private val onTabClose: (Tab) -> Unit
) : RecyclerView.Adapter<TabsAdapter.TabViewHolder>() {

    class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tab_title)
        val url: TextView = view.findViewById(R.id.tab_url)
        val btnClose: TextView = view.findViewById(R.id.btn_close_tab)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = if (tab.title.isNullOrEmpty()) "New Tab" else tab.title
        holder.url.text = tab.url

        holder.itemView.setOnClickListener { onTabClick(tab) }
        holder.btnClose.setOnClickListener { onTabClose(tab) }
    }

    override fun getItemCount(): Int = tabs.size

    fun updateTabs(newTabs: MutableList<Tab>) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}
