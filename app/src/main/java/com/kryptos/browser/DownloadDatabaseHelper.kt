package com.kryptos.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DownloadDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "KryptosDownloads.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE downloads (id INTEGER PRIMARY KEY AUTOINCREMENT, filename TEXT, url TEXT, path TEXT, timestamp INTEGER)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS downloads")
        onCreate(db)
    }

    fun addDownload(filename: String, url: String, path: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("filename", filename)
            put("url", url)
            put("path", path)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("downloads", null, values)
        db.close()
    }

    fun getAllDownloads(): List<DownloadItem> {
        val list = mutableListOf<DownloadItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM downloads ORDER BY timestamp DESC", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val filename = cursor.getString(cursor.getColumnIndexOrThrow("filename"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val path = cursor.getString(cursor.getColumnIndexOrThrow("path"))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                list.add(DownloadItem(id, filename, url, path, timestamp))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun removeDownload(id: Long) {
        val db = this.writableDatabase
        db.delete("downloads", "id=?", arrayOf(id.toString()))
        db.close()
    }

    fun clearDownloads() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM downloads")
        db.close()
    }
}

data class DownloadItem(
    val id: Long,
    val filename: String,
    val url: String,
    val path: String,
    val timestamp: Long
)
