package com.kryptos.browser

import android.content.Context
import android.content.SharedPreferences

object VaultManager {

    private const val PREFS_NAME = "kryptos_prefs"
    private const val KEY_VAULT_SETUP = "is_vault_setup_done"
    private const val KEY_VAULT_COMMAND = "vault_command"
    private const val KEY_VAULT_UNLOCKED = "is_vault_unlocked"
    private const val KEY_GHOST_VAULT = "ghost_vault"

    // Dual-Database names
    const val DB_HISTORY_REAL = "KryptosHistory.db"
    const val DB_HISTORY_DECOY = "KryptosHistory_decoy.db"
    const val DB_BOOKMARKS_REAL = "KryptosBookmarks.db"
    const val DB_BOOKMARKS_DECOY = "KryptosBookmarks_decoy.db"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /* ---------- Ghost Vault master toggle ---------- */

    fun isGhostVaultEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GHOST_VAULT, true)

    fun setGhostVaultEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GHOST_VAULT, enabled).apply()
    }

    /* ---------- Vault Setup ---------- */

    fun isVaultSetupDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VAULT_SETUP, false)

    fun saveVaultCommand(context: Context, command: String) {
        prefs(context).edit()
            .putBoolean(KEY_VAULT_SETUP, true)
            .putString(KEY_VAULT_COMMAND, command.trim())
            .putBoolean(KEY_VAULT_UNLOCKED, false) // starts locked
            .apply()
    }

    fun getVaultCommand(context: Context): String =
        prefs(context).getString(KEY_VAULT_COMMAND, "") ?: ""

    fun isCorrectCommand(context: Context, input: String): Boolean {
        val saved = getVaultCommand(context)
        return saved.isNotEmpty() && input.trim().equals(saved, ignoreCase = true)
    }

    /* ---------- Lock / Unlock State ---------- */

    fun isVaultUnlocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VAULT_UNLOCKED, false)

    fun unlockVault(context: Context) {
        prefs(context).edit().putBoolean(KEY_VAULT_UNLOCKED, true).apply()
    }

    fun lockVault(context: Context) {
        prefs(context).edit().putBoolean(KEY_VAULT_UNLOCKED, false).apply()
    }

    /**
     * Flip between locked/unlocked.
     * @return naya state — true = unlocked (Real DB), false = locked (Decoy DB)
     */
    fun toggleVault(context: Context): Boolean {
        val next = !isVaultUnlocked(context)
        prefs(context).edit().putBoolean(KEY_VAULT_UNLOCKED, next).apply()
        return next
    }

    /* ---------- Current DB Names (final resolver) ---------- */

    fun getHistoryDbName(context: Context): String {
        // Ghost Vault OFF → normal browser, always REAL
        if (!isGhostVaultEnabled(context)) return DB_HISTORY_REAL
        // Ghost Vault ON → locked = decoy, unlocked = real
        return if (isVaultUnlocked(context)) DB_HISTORY_REAL else DB_HISTORY_DECOY
    }

    fun getBookmarkDbName(context: Context): String {
        if (!isGhostVaultEnabled(context)) return DB_BOOKMARKS_REAL
        return if (isVaultUnlocked(context)) DB_BOOKMARKS_REAL else DB_BOOKMARKS_DECOY
    }
}
