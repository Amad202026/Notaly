package com.kel4.notaly.kategori // Sesuaikan foldermu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R

class KategoriActivity : AppCompatActivity() {

    private lateinit var rvKategori: RecyclerView
    private lateinit var tvKosong: TextView
    private lateinit var adapter: KategoriAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori) // Sesuaikan nama XML-mu

        rvKategori = findViewById(R.id.rvSupplier) // Sesuai ID di XML kamu
        tvKosong = findViewById(R.id.tvKosong)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val menuTambah = findViewById<CardView>(R.id.menuTambah)

        // Setup Adapter
        rvKategori.layoutManager = LinearLayoutManager(this)
        adapter = KategoriAdapter(emptyList()) { namaKategori ->
            tampilkanDialogHapus(namaKategori)
        }
        rvKategori.adapter = adapter

        btnBack.setOnClickListener { finish() }

        // Tombol Plus pindah ke halaman Tambah Kategori
        menuTambah.setOnClickListener {
            // Pastikan kamu punya activity untuk menambah kategori
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }
    }

    // Gunakan onResume agar list otomatis refresh setelah kita selesai menambah kategori baru
    override fun onResume() {
        super.onResume()
        muatDaftarKategori()
    }

    private fun muatDaftarKategori() {
        // Buka laci SharedPreferences
        val sharedPref = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)

        // Ambil data, jadikan List, lalu urutkan sesuai abjad (A-Z)
        val setKategori = sharedPref.getStringSet("DAFTAR_KATEGORI", emptySet())
        val daftarKategori = setKategori?.toList()?.sorted() ?: emptyList()

        if (daftarKategori.isEmpty()) {
            tvKosong.visibility = View.VISIBLE
            rvKategori.visibility = View.GONE
        } else {
            tvKosong.visibility = View.GONE
            rvKategori.visibility = View.VISIBLE
            adapter.updateData(daftarKategori)
        }
    }

    private fun tampilkanDialogHapus(kategori: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Hapus '$kategori' dari daftar?")
            .setPositiveButton("Hapus") { _, _ ->
                hapusKategori(kategori)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun hapusKategori(kategoriYgDihapus: String) {
        val sharedPref = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val setKategoriLama = sharedPref.getStringSet("DAFTAR_KATEGORI", emptySet()) ?: emptySet()

        // Buat daftar baru tanpa kategori yang dihapus
        val daftarBaru = setKategoriLama.toMutableSet()
        daftarBaru.remove(kategoriYgDihapus)

        // Simpan kembali ke laci
        sharedPref.edit().putStringSet("DAFTAR_KATEGORI", daftarBaru).apply()

        Toast.makeText(this, "Kategori dihapus", Toast.LENGTH_SHORT).show()
        muatDaftarKategori() // Refresh list di layar
    }
}