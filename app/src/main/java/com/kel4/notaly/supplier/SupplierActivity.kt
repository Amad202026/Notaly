package com.kel4.notaly.supplier

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupplierActivity : AppCompatActivity() {

    private lateinit var rvSupplier: RecyclerView
    private lateinit var tvKosong  : LinearLayout
    private lateinit var etCari    : EditText
    private lateinit var btnFilter : LinearLayout
    private lateinit var adapter   : SupplierAdapter

    // ===================== STATE DATA =====================
    private var dataFull: List<Supplier> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var filterKategori: String = "Semua"   // "Semua" atau kategori suplai spesifik
    private var filterDaerah  : String = "Semua"   // "Semua" atau daerah asal spesifik
    private var urutanAktif   : String = "Nama A-Z"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        muatDataSupplier()
    }

    private fun initViews() {
        rvSupplier = findViewById(R.id.rvSupplier)
        tvKosong   = findViewById(R.id.tvKosong)
        etCari     = findViewById(R.id.etCari)
        btnFilter  = findViewById(R.id.btnFilter)

        rvSupplier.layoutManager = LinearLayoutManager(this)
        adapter = SupplierAdapter(
            listSupplier = emptyList(),
            onDetailKlik = { supplier -> bukaDetail(supplier) },
            onHapusKlik  = { supplier -> tampilkanDialogHapus(supplier) }
        )
        rvSupplier.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java))
            finish()
        }
        findViewById<CardView>(R.id.menuTambahSupplier).setOnClickListener {
            startActivity(Intent(this, TambahSupplierActivity::class.java))
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { terapkanFilterDanSortir() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilter.setOnClickListener { tampilkanDialogFilterDanSortir() }
    }

    private fun muatDataSupplier() {
        lifecycleScope.launch(Dispatchers.IO) {
            dataFull = AppDatabase.getDatabase(this@SupplierActivity)
                .supplierDao()
                .ambilSemuaSupplier()
            withContext(Dispatchers.Main) { terapkanFilterDanSortir() }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        // Kumpulkan kategori suplai unik dari data (dinamis)
        val kategoriList = mutableListOf("Semua")
        dataFull.mapNotNull { it.kategoriSuplai }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .forEach { kategoriList.add(it) }

        // Kumpulkan daerah asal unik dari data (dinamis)
        val daerahList = mutableListOf("Semua")
        dataFull.mapNotNull { it.asalDaerah }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .forEach { daerahList.add(it) }

        val opsiUrutkan = arrayOf("Nama A-Z", "Nama Z-A", "Kategori Suplai A-Z", "Daerah A-Z")

        val pilihan = mutableListOf<String>()

        // Section: Kategori Suplai
        pilihan.add("── FILTER KATEGORI SUPLAI ──")
        kategoriList.forEach { pilihan.add("   Kategori: $it") }

        // Section: Daerah Asal
        val headerDaerah = pilihan.size
        pilihan.add("── FILTER DAERAH ASAL ──")
        daerahList.forEach { pilihan.add("   Daerah: $it") }

        // Section: Urutkan
        val headerUrut = pilihan.size
        pilihan.add("── URUTKAN ──")
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }

        val startKat  = 1
        val endKat    = kategoriList.size
        val startDaer = headerDaerah + 1
        val endDaer   = headerDaerah + daerahList.size
        val startUrut = headerUrut + 1

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Supplier")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when {
                    which in startKat..endKat -> {
                        filterKategori = kategoriList[which - startKat]
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
            it.namaSupplier.lowercase().contains(q) ||
                    it.asalDaerah?.lowercase()?.contains(q) == true ||
                    it.noWa?.contains(q) == true
        }

        // 2. Filter kategori suplai
        if (filterKategori != "Semua") {
            hasil = hasil.filter { it.kategoriSuplai.equals(filterKategori, ignoreCase = true) }
        }

        // 3. Filter daerah asal
        if (filterDaerah != "Semua") {
            hasil = hasil.filter { it.asalDaerah.equals(filterDaerah, ignoreCase = true) }
        }

        // 4. Sortir
        hasil = when (urutanAktif) {
            "Nama A-Z"            -> hasil.sortedBy    { it.namaSupplier.lowercase() }
            "Nama Z-A"            -> hasil.sortedByDescending { it.namaSupplier.lowercase() }
            "Kategori Suplai A-Z" -> hasil.sortedBy    { it.kategoriSuplai?.lowercase() ?: "" }
            "Daerah A-Z"          -> hasil.sortedBy    { it.asalDaerah?.lowercase() ?: "" }
            else                  -> hasil
        }

        tampilkanData(hasil)
    }

    private fun tampilkanData(list: List<Supplier>) {
        val adaData = list.isNotEmpty()
        tvKosong.visibility   = if (adaData) View.GONE    else View.VISIBLE
        rvSupplier.visibility = if (adaData) View.VISIBLE else View.GONE
        adapter.updateData(list)
    }

    private fun bukaDetail(supplier: Supplier) {
        startActivity(
            Intent(this, DetailSupplierActivity::class.java).apply {
                putExtra(DetailSupplierActivity.EXTRA_ID_SUPPLIER, supplier.idSupplier)
            }
        )
    }

    private fun tampilkanDialogHapus(supplier: Supplier) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Supplier")
            .setMessage("Yakin ingin menghapus \"${supplier.namaSupplier}\"?")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapus(supplier) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(supplier: Supplier) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@SupplierActivity)
                .supplierDao()
                .hapusSupplier(supplier)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SupplierActivity,
                    "${supplier.namaSupplier} berhasil dihapus", Toast.LENGTH_SHORT).show()
                muatDataSupplier()
            }
        }
    }
}