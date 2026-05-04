package com.kel4.notaly.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kel4.notaly.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cek laci SharedPreferences DULU sebelum memuat tampilan apapun
        val userPref = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val isReg = userPref.getBoolean("IS_REG", false)

        if (isReg) {
            // 2. Jika sudah registrasi, langsung "Terbangkan" ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Tutup MainActivity agar tidak bisa di-back
            return   // PENTING: Hentikan eksekusi kode di bawahnya!
        }

        // ====================================================================
        // KODE DI BAWAH INI HANYA BERJALAN JIKA USER BELUM REGISTRASI (isReg = false)
        // ====================================================================

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        // Karena user yang sudah registrasi sudah dialihkan ke Login di atas,
        // maka tombol ini sekarang FOKUS untuk mengarahkan ke halaman Register.
        btnGetStarted.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}