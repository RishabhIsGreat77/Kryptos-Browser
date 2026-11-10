package com.kryptos.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDatabaseHelper(
    context: Context,
    dbName: String = VaultManager.getHistoryDbName(context)
) : SQLiteOpenHelper(context, dbName, null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT UNIQUE, timestamp INTEGER, visit_count INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    fun addHistory(title: String, url: String) {
        val db = this.writableDatabase

        // Check karo pehle se kitni baar khula hai
        val cursor = db.rawQuery("SELECT visit_count FROM history WHERE url=?", arrayOf(url))
        var count = 1
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) + 1
        }
        cursor.close()

        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
            put("visit_count", count)
        }
        db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

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

    fun getMostVisitedSites(limit: Int): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM history ORDER BY visit_count DESC LIMIT $limit", null)

        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                list.add(HistoryItem(title, url, 0L))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun clearHistory() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM history")
        db.close()
    }
}

data class HistoryItem(val title: String, val url: String, val timestamp: Long)
