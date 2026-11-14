package com.kryptos.browser

object TabManager {
    val tabsList = mutableListOf<Tab>()
    var activeTabId: String? = null

    init {
        addNewTab("kryptos://home", "Home", false)
    }

    fun addNewTab(url: String, title: String, isIncognito: Boolean = false): Tab {
        val newTab = Tab(
            id = "${System.currentTimeMillis()}_${(100..999).random()}",
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
            addNewTab("kryptos://home", "Home", false)
        } else if (activeTabId == tab.id) {
            activeTabId = tabsList.last().id
        }
    }

    fun hasIncognitoTabs(): Boolean {
        return tabsList.any { it.isIncognito }
    }

    fun clearIncognitoTabs() {
        tabsList.removeAll { it.isIncognito }
        if (tabsList.isEmpty()) {
            addNewTab("kryptos://home", "Home", false)
        } else if (tabsList.none { it.id == activeTabId }) {
            activeTabId = tabsList.last().id
        }
    }

    fun resetAllTabs() {
        tabsList.clear()
        activeTabId = null
        addNewTab("kryptos://home", "Home", false)
    }

    fun getActiveTab(): Tab? {
        return tabsList.find { it.id == activeTabId }
    }

    fun setActiveTab(tabId: String) {
        activeTabId = tabId
    }

    fun updateActiveTabTitle(title: String, url: String) {
        val active = getActiveTab()
        active?.title = if (title.isEmpty()) "New Tab" else title
        active?.url = url
    }
}

data class Tab(
    val id: String,
    var title: String,
    var url: String,
    var isActive: Boolean = false,
    var isIncognito: Boolean = false
)
