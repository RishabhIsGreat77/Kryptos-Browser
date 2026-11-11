package com.kryptos.browser

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BookmarksActivity : AppCompatActivity() {

    private lateinit var db: BookmarkDatabaseHelper
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        db = BookmarkDatabaseHelper(this)
        recycler = findViewById(R.id.bookmarks_recycler_view)
        recycler.layoutManager = LinearLayoutManager(this)

        // Trash Icon Listener (Needs to be added in activity_bookmarks.xml)
        val btnClear = findViewById<ImageView>(R.id.btn_clear_bookmarks)
        btnClear.setOnClickListener {
            showClearBookmarksDialog()
        }

        loadBookmarks()
    }

    private fun loadBookmarks() {
        val list = db.getAllBookmarks()
        recycler.adapter = BookmarkAdapter(list,
            onClick = { item ->
                TabManager.addNewTab(item.url, item.title)
                finish()
            },
            onLongClick = { item ->
                db.removeBookmark(item.url)
                Toast.makeText(this, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                loadBookmarks() // Refresh list
            }
        )
    }

    private fun showClearBookmarksDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Clear Bookmarks"
        message.text = "Are you sure you want to remove all bookmarks?"
        btnConfirm.text = "Clear"
        btnConfirm.setTextColor(Color.parseColor("#EF4444")) // Error Color

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            db.clearBookmarks()
            Toast.makeText(this, "All bookmarks cleared!", Toast.LENGTH_SHORT).show()
            loadBookmarks() // Refresh list
        }
        dialog.show()
    }
}
