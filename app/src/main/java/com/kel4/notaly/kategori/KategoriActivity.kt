package com.kel4.notaly.kategori

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.home.BerandaActivity

class KategoriActivity : AppCompatActivity() {

    private lateinit var rvKategori: RecyclerView
    private lateinit var tvKosong  : LinearLayout
    private lateinit var etCari    : EditText
    private lateinit var adapter   : KategoriAdapter

    private var dataFull: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategori)

        rvKategori = findViewById(R.id.rvKategori)
        tvKosong   = findViewById(R.id.tvKosong)
        etCari     = findViewById(R.id.etCari)

        rvKategori.layoutManager = LinearLayoutManager(this)
        adapter = KategoriAdapter(emptyList()) { namaKategori ->
            tampilkanDialogHapus(namaKategori)
        }
        rvKategori.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java))
            finish()
        }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, TambahKategoriActivity::class.java))
        }

        // Search realtime
        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        muatDaftarKategori()
    }

    private fun muatDaftarKategori() {
        val sharedPref   = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val setKategori  = sharedPref.getStringSet("DAFTAR_KATEGORI", emptySet())
        dataFull         = setKategori?.toList()?.sorted() ?: emptyList()
        // Terapkan ulang search yang mungkin sedang aktif
        filterData(etCari.text.toString())
    }

    private fun filterData(query: String) {
        val hasil = if (query.isBlank()) dataFull
        else dataFull.filter { it.contains(query, ignoreCase = true) }
        tampilkanData(hasil)
    }

    private fun tampilkanData(list: List<String>) {
        val adaData = list.isNotEmpty()
        tvKosong.visibility   = if (adaData) View.GONE    else View.VISIBLE
        rvKategori.visibility = if (adaData) View.VISIBLE else View.GONE
        adapter.updateData(list)
    }

    private fun tampilkanDialogHapus(kategori: String) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Hapus '$kategori' dari daftar?")
            .setPositiveButton("Hapus") { _, _ -> hapusKategori(kategori) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun hapusKategori(kategoriYgDihapus: String) {
        val sharedPref      = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val setKategoriLama = sharedPref.getStringSet("DAFTAR_KATEGORI", emptySet()) ?: emptySet()
        val daftarBaru      = setKategoriLama.toMutableSet().also { it.remove(kategoriYgDihapus) }
        sharedPref.edit().putStringSet("DAFTAR_KATEGORI", daftarBaru).apply()

        Toast.makeText(this, "Kategori dihapus", Toast.LENGTH_SHORT).show()
        muatDaftarKategori()
    }
}