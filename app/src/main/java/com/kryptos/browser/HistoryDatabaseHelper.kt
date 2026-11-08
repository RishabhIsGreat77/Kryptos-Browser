package com.kryptos.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "KryptosHistory.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Table banate hain jisme Title, URL aur Time save hoga
        val createTable = "CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT UNIQUE, timestamp INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    // Naya page khulne par history me save karna
    fun addHistory(title: String, url: String) {
        // Agar wahi URL dobara khule, to use update kar do (Duplicate na bane)
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Puri history nikalna (Latest sabse upar)
    fun getAllHistory(): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM history ORDER BY timestamp DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                historyList.add(HistoryItem(title, url, timestamp))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return historyList
    }

    // Puri history delete karna
    fun clearHistory() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM history")
        db.close()
    }
}

// Data Class (Har history item ke liye)
data class HistoryItem(val title: String, val url: String, val timestamp: Long)
