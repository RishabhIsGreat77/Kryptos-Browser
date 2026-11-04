package com.kryptos.browser

object TabManager {
    val tabsList = mutableListOf<Tab>()
    var activeTabId: String? = null

    init {
        // App start hote hi ek home tab banao
        addNewTab("kryptos://home", "Home")
    }

    fun addNewTab(url: String, title: String): Tab {
        val newTab = Tab(System.currentTimeMillis().toString(), title, url)
        tabsList.add(newTab)
        activeTabId = newTab.id
        return newTab
    }

    fun removeTab(tab: Tab) {
        tabsList.remove(tab)
        if (tabsList.isEmpty()) {
            // Agar sab tabs band ho gaye, toh naya home tab banao
            addNewTab("kryptos://home", "Home")
        } else if (activeTabId == tab.id) {
            // Agar active tab band hua, toh last wale ko active karo
            activeTabId = tabsList.last().id
        }
    }

    fun getActiveTab(): Tab? {
        return tabsList.find { it.id == activeTabId }
    }

    fun setActiveTab(tabId: String) {
        activeTabId = tabId
    }
    
    fun updateActiveTabTitle(title: String, url: String) {
        val active = getActiveTab()
        active?.title = if (title.isNullOrEmpty()) "New Tab" else title
        active?.url = url
    }
}
