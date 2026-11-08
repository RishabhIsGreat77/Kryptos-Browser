package com.kryptos.browser

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BookmarksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)
        
        val db = BookmarkDatabaseHelper(this)
        val recycler = findViewById<RecyclerView>(R.id.bookmarks_recycler_view)
        recycler.layoutManager = LinearLayoutManager(this)

        fun loadBookmarks() {
            val list = db.getAllBookmarks()
            recycler.adapter = BookmarkAdapter(list, 
                onClick = { item ->
                    TabManager.addNewTab(item.url, item.title)
                    finish()
                }, 
                onLongClick = { item ->
                    db.removeBookmark(item.url)
                    Toast.makeText(this, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                    recycler.adapter = BookmarkAdapter(db.getAllBookmarks(), { i -> TabManager.addNewTab(i.url, i.title); finish() }, { i -> db.removeBookmark(i.url); finish(); startActivity(intent) })
                }
            )
        }
        loadBookmarks()
    }
}
