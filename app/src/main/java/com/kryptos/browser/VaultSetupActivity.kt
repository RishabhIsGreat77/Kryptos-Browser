package com.kryptos.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VaultSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_setup)

        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val etCommand = findViewById<EditText>(R.id.et_vault_command)
        val btnSave = findViewById<Button>(R.id.btn_save_vault)

        btnSave.setOnClickListener {
            val command = etCommand.text.toString().trim()
            if (command.isNotEmpty()) {
                prefs.edit().putString("vault_command", command).apply()
                prefs.edit().putBoolean("is_vault_setup_done", true).apply()
                
                Toast.makeText(this, "Vault Command Saved!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter a secret command", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
