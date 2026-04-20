package com.kel4.notaly.barang

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class BarangActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }
}