package com.kryptos.browser

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class BookmarksActivity : AppCompatActivity() {

    private lateinit var db: BookmarkDatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var txtEmpty: TextView

    // Export & Import Launchers
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            exportBookmarksToUri(uri)
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importBookmarksFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmarks)

        db = BookmarkDatabaseHelper(this)
        recycler = findViewById(R.id.bookmarks_recycler_view)
        txtEmpty = findViewById(R.id.txt_empty_bookmarks)
        recycler.layoutManager = LinearLayoutManager(this)

        // Export Button
        findViewById<ImageView>(R.id.btn_export_bookmarks).setOnClickListener {
            if (db.getAllBookmarks().isEmpty()) {
                Toast.makeText(this, "No bookmarks to export", Toast.LENGTH_SHORT).show()
            } else {
                exportLauncher.launch("Kryptos_Bookmarks.json")
            }
        }

        // Import Button
        findViewById<ImageView>(R.id.btn_import_bookmarks).setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        // Clear Button
        val btnClear = findViewById<ImageView>(R.id.btn_clear_bookmarks)
        btnClear.setOnClickListener {
            if (db.getAllBookmarks().isNotEmpty()) {
                showClearBookmarksDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookmarks()
    }

    private fun loadBookmarks() {
        val list = db.getAllBookmarks()
        
        if (list.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }

        recycler.adapter = BookmarkAdapter(list,
            onClick = { item ->
                TabManager.addNewTab(item.url, item.title)
                finish()
            },
            onLongClick = { item ->
                db.removeBookmark(item.url)
                Toast.makeText(this, "Bookmark Removed", Toast.LENGTH_SHORT).show()
                loadBookmarks()
            }
        )
    }

    // EXPORT LOGIC
    private fun exportBookmarksToUri(uri: android.net.Uri) {
        try {
            val bookmarks = db.getAllBookmarks()
            val jsonArray = JSONArray()
            for (b in bookmarks) {
                val obj = JSONObject()
                obj.put("title", b.title)
                obj.put("url", b.url)
                jsonArray.put(obj)
            }
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonArray.toString().toByteArray())
            }
            Toast.makeText(this, "Bookmarks exported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // IMPORT LOGIC
    private fun importBookmarksFromUri(uri: android.net.Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (!jsonString.isNullOrEmpty()) {
                val jsonArray = JSONArray(jsonString)
                var count = 0
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "Untitled")
                    val url = obj.optString("url", "")
                    if (url.isNotEmpty()) {
                        db.addBookmark(title, url)
                        count++
                    }
                }
                Toast.makeText(this, "$count bookmarks imported successfully!", Toast.LENGTH_SHORT).show()
                loadBookmarks()
            } else {
                Toast.makeText(this, "File is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: Invalid file format", Toast.LENGTH_SHORT).show()
        }
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
        btnConfirm.setTextColor(Color.parseColor("#EF4444"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            db.clearBookmarks()
            Toast.makeText(this, "All bookmarks cleared!", Toast.LENGTH_SHORT).show()
            loadBookmarks()
        }
        dialog.show()
    }
}
