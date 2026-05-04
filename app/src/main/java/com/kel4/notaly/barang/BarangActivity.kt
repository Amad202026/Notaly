package com.kel4.notaly.barang

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BarangActivity : AppCompatActivity() {

    private lateinit var rvBarang     : RecyclerView
    private lateinit var tvKosong     : LinearLayout
    private lateinit var tvPesanKosong: TextView
    private lateinit var etCari       : EditText
    private lateinit var btnFilter    : LinearLayout
    private lateinit var adapter      : BarangAdapter

    // ===================== STATE DATA =====================
    private var daftarBarangAsli: List<Barang> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var kategoriTerpilih: String = "Semua Kategori"
    private var filterStok      : String = "Semua"   // "Semua" | "Stok Ada" | "Stok Habis" | "Stok Menipis"
    private var urutanAktif     : String = "Nama A-Z"
    private var kataKunciCari   : String = ""

    // Batas "stok menipis" — sesuaikan dengan kebutuhan bisnis
    private val BATAS_MENIPIS = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang)

        initViews()
        setupCari()
    }

    override fun onResume() {
        super.onResume()
        muatDataBarang()
    }

    private fun initViews() {
        rvBarang      = findViewById(R.id.rvBarang)
        tvKosong      = findViewById(R.id.tvKosong)
        etCari        = findViewById(R.id.etCari)
        btnFilter     = findViewById(R.id.btnFilter)
        tvPesanKosong = findViewById(R.id.tvPesanKosong)

        val btnBack          = findViewById<ImageView>(R.id.btnBack)
        val menuTambahBarang = findViewById<CardView>(R.id.menuTambahBarang)

        btnBack.setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java))
            finish()
        }
        menuTambahBarang.setOnClickListener {
            startActivity(Intent(this, TambahBarangActivity::class.java))
        }

        rvBarang.layoutManager = LinearLayoutManager(this)
        adapter = BarangAdapter(
            listBarang = emptyList(),
            onClick    = { barang -> bukaDetail(barang) },
            onDelete   = { barang -> tampilkanDialogHapus(barang) }
        )
        rvBarang.adapter = adapter

        btnFilter.setOnClickListener { tampilkanDialogFilterDanSortir() }
    }

    private fun setupCari() {
        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                kataKunciCari = s.toString().trim()
                terapkanFilterDanSortir()
            }
        })
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        // Ambil kategori dari SharedPreferences (dinamis)
        val sharedPref  = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val setKategori = sharedPref.getStringSet("DAFTAR_KATEGORI", mutableSetOf()) ?: mutableSetOf()
        val kategoriList = mutableListOf("Semua Kategori")
        kategoriList.addAll(setKategori.sorted())

        val opsiStok    = arrayOf("Semua", "Stok Ada", "Stok Habis", "Stok Menipis (≤$BATAS_MENIPIS)")
        val opsiUrutkan = arrayOf("Nama A-Z", "Nama Z-A", "Stok Terbanyak", "Stok Tersedikit", "Harga Jual Tertinggi", "Harga Jual Terendah", "Harga Modal Tertinggi", "Harga Modal Terendah")

        val pilihan = mutableListOf<String>()

        // Section: Kategori
        pilihan.add("── FILTER KATEGORI ──")                            // 0
        kategoriList.forEach { pilihan.add("   Kategori: $it") }        // 1 .. kategoriList.size

        val headerStok = pilihan.size
        pilihan.add("── FILTER STOK ──")                               // headerStok
        opsiStok.forEach { pilihan.add("   Stok: $it") }               // headerStok+1 .. headerStok+opsiStok.size

        val headerUrut = pilihan.size
        pilihan.add("── URUTKAN ──")                                   // headerUrut
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }            // headerUrut+1 ..

        // Kalkulasi batas index
        val startKat  = 1
        val endKat    = kategoriList.size        // index terakhir item kategori (inklusif)
        val startStok = headerStok + 1
        val endStok   = headerStok + opsiStok.size
        val startUrut = headerUrut + 1

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Barang")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when {
                    which in startKat..endKat -> {
                        kategoriTerpilih = kategoriList[which - startKat]
                        terapkanFilterDanSortir()
                    }
                    which in startStok..endStok -> {
                        filterStok = opsiStok[which - startStok]
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
        var hasil = daftarBarangAsli

        // 1. Filter kategori
        if (kategoriTerpilih != "Semua Kategori") {
            hasil = hasil.filter { it.kategori.equals(kategoriTerpilih, ignoreCase = true) }
        }

        // 2. Filter stok
        hasil = when (filterStok) {
            "Stok Ada"                          -> hasil.filter { it.stok > BATAS_MENIPIS }
            "Stok Habis"                        -> hasil.filter { it.stok == 0 }
            "Stok Menipis (≤$BATAS_MENIPIS)"   -> hasil.filter { it.stok in 1..BATAS_MENIPIS }
            else                                -> hasil
        }

        // 3. Filter pencarian nama
        if (kataKunciCari.isNotEmpty()) {
            hasil = hasil.filter { it.namaBarang.contains(kataKunciCari, ignoreCase = true) }
        }

        // 4. Sortir
        hasil = when (urutanAktif) {
            "Nama A-Z"              -> hasil.sortedBy    { it.namaBarang.lowercase() }
            "Nama Z-A"              -> hasil.sortedByDescending { it.namaBarang.lowercase() }
            "Stok Terbanyak"        -> hasil.sortedByDescending { it.stok }
            "Stok Tersedikit"       -> hasil.sortedBy    { it.stok }
            "Harga Jual Tertinggi"  -> hasil.sortedByDescending { it.hargaJual }
            "Harga Jual Terendah"   -> hasil.sortedBy    { it.hargaJual }
            "Harga Modal Tertinggi" -> hasil.sortedByDescending { it.hargaModal }
            "Harga Modal Terendah"  -> hasil.sortedBy    { it.hargaModal }
            else                    -> hasil
        }

        // 5. Update UI
        val adaData = hasil.isNotEmpty()
        tvKosong.visibility = if (adaData) View.GONE    else View.VISIBLE
        rvBarang.visibility = if (adaData) View.VISIBLE else View.GONE

        if (!adaData) {
            tvPesanKosong.text = if (daftarBarangAsli.isNotEmpty()) "Barang tidak ditemukan"
            else "Belum ada barang"
        }

        adapter.updateData(hasil)
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA
    // ─────────────────────────────────────────────────────────
    private fun muatDataBarang() {
        lifecycleScope.launch(Dispatchers.IO) {
            val daftarBarang = AppDatabase.getDatabase(this@BarangActivity)
                .barangDao()
                .ambilSemuaBarang()
            withContext(Dispatchers.Main) {
                daftarBarangAsli = daftarBarang
                terapkanFilterDanSortir()
            }
        }
    }

    private fun bukaDetail(barang: Barang) {
        val intent = Intent(this, DetailBarangActivity::class.java)
        intent.putExtra(DetailBarangActivity.EXTRA_ID_BARANG, barang.idBarang)
        startActivity(intent)
    }

    private fun tampilkanDialogHapus(barang: Barang) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Barang")
            .setMessage("Yakin ingin menghapus \"${barang.namaBarang}\"?")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapus(barang) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(barang: Barang) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@BarangActivity)
                .barangDao()
                .hapusBarang(barang)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BarangActivity, "${barang.namaBarang} berhasil dihapus", Toast.LENGTH_SHORT).show()
                muatDataBarang()
            }
        }
    }
}