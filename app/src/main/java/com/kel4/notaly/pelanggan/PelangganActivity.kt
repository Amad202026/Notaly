package com.kel4.notaly.pelanggan

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.kel4.notaly.R

class PelangganActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }
}