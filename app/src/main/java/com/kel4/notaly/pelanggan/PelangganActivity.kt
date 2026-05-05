package com.kel4.notaly.pelanggan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.Pelanggan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PelangganActivity : AppCompatActivity() {

    private lateinit var rvPelanggan: RecyclerView
    private lateinit var tvKosong   : LinearLayout
    private lateinit var etCari     : EditText
    private lateinit var adapter    : PelangganAdapter

    // ===================== STATE DATA =====================
    private var dataFull: List<Pelanggan> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var filterKategori: String = "Semua"   // "Semua" | "Umum" | "Grosir" | "Member"
    private var filterDaerah  : String = "Semua"   // "Semua" atau nama daerah spesifik
    private var urutanAktif   : String = "Nama A-Z"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        muatData()
    }

    private fun initViews() {
        rvPelanggan = findViewById(R.id.rvPelanggan)
        tvKosong    = findViewById(R.id.tvKosong)
        etCari      = findViewById(R.id.etCari)

        rvPelanggan.layoutManager = LinearLayoutManager(this)
        adapter = PelangganAdapter(emptyList()) { pelanggan ->
            val intent = Intent(this, DetailPelangganActivity::class.java)
            intent.putExtra("ID_PELANGGAN", pelanggan.idPelanggan)
            startActivity(intent)
        }
        rvPelanggan.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, TambahPelangganActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.btnFilter).setOnClickListener {
            tampilkanDialogFilterDanSortir()
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { terapkanFilterDanSortir() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun muatData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PelangganActivity)
            dataFull = db.pelangganDao().ambilSemuaPelanggan()
            withContext(Dispatchers.Main) { terapkanFilterDanSortir() }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        val opsiKategori = arrayOf("Semua", "Umum", "Grosir", "Member")
        val opsiUrutkan  = arrayOf("Nama A-Z", "Nama Z-A", "Daerah A-Z", "Kategori")

        // Kumpulkan daerah unik dari data
        val daerahList = mutableListOf("Semua")
        dataFull.mapNotNull { it.asalDaerah }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .forEach { daerahList.add(it) }

        val pilihan = mutableListOf<String>()

        // Section: Kategori Pelanggan
        pilihan.add("── FILTER KATEGORI PELANGGAN ──")
        opsiKategori.forEach { pilihan.add("   Kategori: $it") }

        // Section: Daerah Asal
        val headerDaerah = pilihan.size
        pilihan.add("── FILTER DAERAH ASAL ──")
        daerahList.forEach { pilihan.add("   Daerah: $it") }

        // Section: Urutkan
        val headerUrut = pilihan.size
        pilihan.add("── URUTKAN ──")
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }

        val startKat   = 1
        val endKat     = opsiKategori.size
        val startDaer  = headerDaerah + 1
        val endDaer    = headerDaerah + daerahList.size
        val startUrut  = headerUrut + 1

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Pelanggan")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when {
                    which in startKat..endKat -> {
                        filterKategori = opsiKategori[which - startKat]
                        terapkanFilterDanSortir()
                    }
                    which in startDaer..endDaer -> {
                        filterDaerah = daerahList[which - startDaer]
                        terapkanFilterDanSortir()
                    }
                    which >= startUrut -> {
                        urutanAktif = opsiUrutkan[which - startUrut]
                        terapkanFilterDanSortir()
                    }
                }
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────
    //  TERAPKAN FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun terapkanFilterDanSortir() {
        val q = etCari.text.toString().trim().lowercase()

        // 1. Filter teks: nama, daerah, no WA
        var hasil = if (q.isBlank()) dataFull
        else dataFull.filter {
            it.namaPelanggan.lowercase().contains(q) ||
                    it.asalDaerah?.lowercase()?.contains(q) == true ||
                    it.noWa?.contains(q) == true
        }

        // 2. Filter kategori pelanggan
        if (filterKategori != "Semua") {
            hasil = hasil.filter { it.kategoriPelanggan.equals(filterKategori, ignoreCase = true) }
        }

        // 3. Filter daerah
        if (filterDaerah != "Semua") {
            hasil = hasil.filter { it.asalDaerah.equals(filterDaerah, ignoreCase = true) }
        }

        // 4. Sortir
        hasil = when (urutanAktif) {
            "Nama A-Z"  -> hasil.sortedBy    { it.namaPelanggan.lowercase() }
            "Nama Z-A"  -> hasil.sortedByDescending { it.namaPelanggan.lowercase() }
            "Daerah A-Z"-> hasil.sortedBy    { it.asalDaerah?.lowercase() ?: "" }
            "Kategori"  -> hasil.sortedBy    { it.kategoriPelanggan?.lowercase() }
            else        -> hasil
        }

        tampilkanData(hasil)
    }

    private fun tampilkanData(list: List<Pelanggan>) {
        val adaData = list.isNotEmpty()
        tvKosong.visibility    = if (adaData) View.GONE    else View.VISIBLE
        rvPelanggan.visibility = if (adaData) View.VISIBLE else View.GONE
        adapter.updateData(list)
    }
}