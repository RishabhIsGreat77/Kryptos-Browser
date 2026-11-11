package com.kryptos.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Tab Data Class (Added isIncognito flag)
data class Tab(
    val id: String,
    var title: String,
    var url: String,
    var isActive: Boolean = false,
    var isIncognito: Boolean = false
)

// 2. TabsAdapter (Jo list ko grid me dikhayega)
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

        // Tab par click karne se switch hoga
        holder.itemView.setOnClickListener { onTabClick(tab) }
        
        // X par click karne se tab close hoga
        holder.btnClose.setOnClickListener { onTabClose(tab) }
    }

    override fun getItemCount(): Int = tabs.size

    // Jab bhi tab list update ho, ise call karenge
    fun updateTabs(newTabs: MutableList<Tab>) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}
