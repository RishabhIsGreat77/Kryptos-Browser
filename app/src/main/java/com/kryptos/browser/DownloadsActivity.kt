package com.kryptos.browser

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class DownloadsActivity : AppCompatActivity() {

    private lateinit var db: DownloadDatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var txtEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)

        db = DownloadDatabaseHelper(this)
        recycler = findViewById(R.id.downloads_recycler_view)
        txtEmpty = findViewById(R.id.txt_empty_downloads)
        recycler.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btn_clear_downloads).setOnClickListener {
            if (db.getAllDownloads().isNotEmpty()) {
                showClearDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDownloads()
    }

    private fun loadDownloads() {
        val list = db.getAllDownloads()
        
        if (list.isEmpty()) {
            txtEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            txtEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }

        recycler.adapter = DownloadsAdapter(list, 
            onClick = { item ->
                openFile(item)
            }, 
            onLongClick = { item ->
                showDeleteItemDialog(item)
            }
        )
    }

    private fun openFile(item: DownloadItem) {
        val file = File(item.path)
        if (file.exists()) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString())
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open this file type", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found on device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteItemDialog(item: DownloadItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Remove Download"
        message.text = "Remove from list? (File will not be deleted)"
        btnConfirm.text = "Remove"
        btnConfirm.setTextColor(Color.parseColor("#EF4444"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            db.removeDownload(item.id)
            loadDownloads()
        }
        dialog.show()
    }

    private fun showClearDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_custom)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val message = dialog.findViewById<TextView>(R.id.dialog_message)
        val btnCancel = dialog.findViewById<TextView>(R.id.btn_dialog_cancel)
        val btnConfirm = dialog.findViewById<TextView>(R.id.btn_dialog_confirm)

        title.text = "Clear Downloads"
        message.text = "Clear download history?"
        btnConfirm.text = "Clear"
        btnConfirm.setTextColor(Color.parseColor("#EF4444"))

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            db.clearDownloads()
            loadDownloads()
            Toast.makeText(this, "Downloads cleared", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }
}
