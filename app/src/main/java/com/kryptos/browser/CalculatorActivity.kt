package com.kryptos.browser

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentInput = ""
    private var operator = ""
    private var firstOperand = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator) // तुझे ये लेआउट बनाना पड़ेगा या फिर कोई सिंपल XML डाल दे

        display = findViewById(R.id.calc_display)
        
        // यहाँ बस सिंपल कैलकुलेटर का लॉजिक डालना है, 
        // फिलहाल तेरे पैनिक शेक को काम करने के लिए बस ये क्लास होना ज़रूरी है।
        // तू इसे बाद में प्रॉपर बना लेना।
    }
}
