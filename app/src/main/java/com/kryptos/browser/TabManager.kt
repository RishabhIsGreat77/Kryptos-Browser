package com.kryptos.browser

object TabManager {
    val tabsList = mutableListOf<Tab>()
    var activeTabId: String? = null

    init {
        // Create a home tab on app start
        addNewTab("kryptos://home", "Home", false)
    }

    fun addNewTab(url: String, title: String, isIncognito: Boolean = false): Tab {
        // FIXED: Used named parameters to prevent mixing up 'isActive' and 'isIncognito'
        val newTab = Tab(
            id = System.currentTimeMillis().toString(),
            title = title,
            url = url,
            isActive = false,
            isIncognito = isIncognito
        )
        tabsList.add(newTab)
        activeTabId = newTab.id
        return newTab
    }

    fun removeTab(tab: Tab) {
        tabsList.remove(tab)
        if (tabsList.isEmpty()) {
            // If all tabs are closed, create a new home tab
            addNewTab("kryptos://home", "Home", false)
        } else if (activeTabId == tab.id) {
            // If active tab was closed, set last tab as active
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
