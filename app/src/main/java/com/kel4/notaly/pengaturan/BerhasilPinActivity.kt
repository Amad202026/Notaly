package com.kel4.notaly.pengaturan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ButtonBarLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kel4.notaly.R

class BerhasilPinActivity : AppCompatActivity() {

    lateinit var btnKembali : Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_berhasil_pin)

        btnKembali = findViewById(R.id.btnKembali)

        btnKembali.setOnClickListener {
            startActivity(Intent(this, PengaturanActivity::class.java))
            finish()
        }
    }
}