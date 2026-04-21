package com.kel4.notaly.kategori

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.kel4.notaly.R
import com.kel4.notaly.pengaturan.PengaturanActivity

lateinit var etNamaKategori: EditText
lateinit var btnSimpan: TextView

class TambahKategoriActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kategori_tambah)

        var etNamaKategori = findViewById<EditText>(R.id.etNamaKategori)
        var btnSimpan = findViewById<TextView>(R.id.btnSimpan)

        // Di dalam onCreate KategoriActivity.kt
        btnSimpan.setOnClickListener {
            val inputKategori = etNamaKategori.text.toString().trim()

            if (inputKategori.isEmpty()) {
                Toast.makeText(this, "Kategori tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            } else {
                // 1. Buka laci SharedPreferences
                val sharedPref = getSharedPreferences("KategoriPrefs", MODE_PRIVATE)

                // 2. Ambil daftar kategori yang sudah ada sebelumnya (kalau belum ada, buat laci kosong)
                val daftarLama = sharedPref.getStringSet("DAFTAR_KATEGORI", mutableSetOf()) ?: mutableSetOf()

                // 3. Tambahkan kategori baru ke dalam daftar
                val daftarBaru = mutableSetOf<String>()
                daftarBaru.addAll(daftarLama)
                daftarBaru.add(inputKategori) // Masukkan ketikan user

                // 4. Simpan kembali laci tersebut
                sharedPref.edit().putStringSet("DAFTAR_KATEGORI", daftarBaru).apply()

                Toast.makeText(this, "Kategori $inputKategori berhasil disimpan!", Toast.LENGTH_SHORT).show()
                etNamaKategori.text.clear()

                startActivity(Intent(this, KategoriActivity::class.java))
                finish()
            }
        }
    }
}