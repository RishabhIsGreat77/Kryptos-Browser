package com.kryptos.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookmarkDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "KryptosBookmarks.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, url TEXT UNIQUE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS bookmarks")
        onCreate(db)
    }

    fun addBookmark(title: String, url: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("url", url)
        }
        db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun removeBookmark(url: String) {
        val db = this.writableDatabase
        db.delete("bookmarks", "url=?", arrayOf(url))
        db.close()
    }

    fun isBookmarked(url: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bookmarks WHERE url=?", arrayOf(url))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun getAllBookmarks(): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bookmarks ORDER BY id DESC", null)
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

    fun clearBookmarks() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM bookmarks")
        db.close()
    }
}
