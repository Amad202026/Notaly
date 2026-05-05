package com.kel4.notaly.pengiriman

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.Pengiriman
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PengirimanActivity : AppCompatActivity() {

    private lateinit var rvPengiriman : RecyclerView
    private lateinit var tvKosong     : LinearLayout
    private lateinit var etCari       : EditText
    private lateinit var btnFilter    : LinearLayout
    private lateinit var menuTambah   : View
    private lateinit var tvPesanKosong: TextView

    private lateinit var adapter: PengirimanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    // ===================== STATE DATA =====================
    private var dataFull: List<Pengiriman> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var filterStatus   : String = "Semua"    // "Semua" | "Diproses" | "Dikirim" | "Terkirim" | "Dibatalkan"
    private var filterEkspedisi: String = "Semua"    // "Semua" atau nama ekspedisi spesifik
    private var urutanAktif    : String = "Terbaru"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupFab()
        muatData()
    }

    override fun onResume() {
        super.onResume()
        muatData()
    }

    private fun initViews() {
        rvPengiriman  = findViewById(R.id.rvPengiriman)
        tvKosong      = findViewById(R.id.tvKosong)
        etCari        = findViewById(R.id.etCari)
        btnFilter     = findViewById(R.id.btnFilter)
        menuTambah    = findViewById(R.id.menuTambah)
        tvPesanKosong = findViewById(R.id.tvPesanKosong)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnFilter.setOnClickListener { tampilkanDialogFilterDanSortir() }
    }

    private fun setupRecyclerView() {
        adapter = PengirimanAdapter(
            onItemClick = { pengiriman ->
                val intent = Intent(this, DetailPengirimanActivity::class.java)
                intent.putExtra("ID_PENGIRIMAN", pengiriman.idPengiriman)
                startActivity(intent)
            },
            onEditClick = { pengiriman ->
                val intent = Intent(this, TambahPengirimanActivity::class.java)
                intent.putExtra("ID_PENGIRIMAN", pengiriman.idPengiriman)
                startActivity(intent)
            },
            onDeleteClick = { pengiriman -> showDeleteDialog(pengiriman.idPengiriman) }
        )
        rvPengiriman.layoutManager = LinearLayoutManager(this)
        rvPengiriman.adapter = adapter
    }

    private fun setupSearch() {
        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { terapkanFilterDanSortir() }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFab() {
        menuTambah.setOnClickListener {
            startActivity(Intent(this, TambahPengirimanActivity::class.java))
        }
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA
    // ─────────────────────────────────────────────────────────
    private fun muatData() {
        lifecycleScope.launch(Dispatchers.IO) {
            dataFull = db.pengirimanDao().getAllPengiriman()
            withContext(Dispatchers.Main) { terapkanFilterDanSortir() }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        val opsiStatus  = arrayOf("Semua", "Diproses", "Dikirim", "Terkirim", "Dibatalkan")
        val opsiUrutkan = arrayOf("Terbaru", "Terlama", "Ekspedisi A-Z", "Ekspedisi Z-A", "No. Resi A-Z")

        // Kumpulkan ekspedisi unik dari data
        val ekspedisiList = mutableListOf("Semua")
        dataFull.mapNotNull { it.namaEkspedisi }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .forEach { ekspedisiList.add(it) }

        val pilihan = mutableListOf<String>()

        // Section: Status Pengiriman
        pilihan.add("── FILTER STATUS PENGIRIMAN ──")
        opsiStatus.forEach { pilihan.add("   Status: $it") }

        // Section: Ekspedisi
        val headerEksp = pilihan.size
        pilihan.add("── FILTER EKSPEDISI ──")
        ekspedisiList.forEach { pilihan.add("   Ekspedisi: $it") }

        // Section: Urutkan
        val headerUrut = pilihan.size
        pilihan.add("── URUTKAN ──")
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }

        val startStatus = 1
        val endStatus   = opsiStatus.size
        val startEksp   = headerEksp + 1
        val endEksp     = headerEksp + ekspedisiList.size
        val startUrut   = headerUrut + 1

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Pengiriman")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when {
                    which in startStatus..endStatus -> {
                        filterStatus = opsiStatus[which - startStatus]
                        terapkanFilterDanSortir()
                    }
                    which in startEksp..endEksp -> {
                        filterEkspedisi = ekspedisiList[which - startEksp]
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
        val q = etCari.text.toString().trim()

        // 1. Filter teks: ekspedisi, resi, id transaksi, alamat
        var hasil = if (q.isBlank()) dataFull
        else dataFull.filter {
            it.namaEkspedisi?.contains(q, ignoreCase = true) == true ||
                    it.noResi?.contains(q, ignoreCase = true) == true ||
                    it.idTransaksi?.contains(q, ignoreCase = true) == true ||
                    it.alamatLengkap?.contains(q, ignoreCase = true) == true
        }

        // 2. Filter status
        if (filterStatus != "Semua") {
            hasil = hasil.filter { it.statusKirim == filterStatus }
        }

        // 3. Filter ekspedisi
        if (filterEkspedisi != "Semua") {
            hasil = hasil.filter { it.namaEkspedisi.equals(filterEkspedisi, ignoreCase = true) }
        }

        // 4. Sortir
        // Catatan: urutan "Terbaru"/"Terlama" mengandalkan idPengiriman sebagai proxy waktu input
        // karena model Pengiriman kemungkinan tidak memiliki field tanggal terpisah.
        // Jika ada field tanggal, ganti dengan field tersebut.
        hasil = when (urutanAktif) {
            "Terbaru"       -> hasil.sortedByDescending { it.idPengiriman }
            "Terlama"       -> hasil.sortedBy    { it.idPengiriman }
            "Ekspedisi A-Z" -> hasil.sortedBy    { it.namaEkspedisi?.lowercase() ?: "" }
            "Ekspedisi Z-A" -> hasil.sortedByDescending { it.namaEkspedisi?.lowercase() ?: "" }
            "No. Resi A-Z"  -> hasil.sortedBy    { it.noResi ?: "" }
            else            -> hasil
        }

        adapter.submitList(hasil)

        if (hasil.isEmpty()) {
            rvPengiriman.visibility = View.GONE
            tvKosong.visibility     = View.VISIBLE
            tvPesanKosong.text      =
                if (q.isNotBlank() || filterStatus != "Semua" || filterEkspedisi != "Semua")
                    "Tidak ada hasil yang cocok"
                else
                    "Belum ada Daftar Pengiriman"
        } else {
            rvPengiriman.visibility = View.VISIBLE
            tvKosong.visibility     = View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────
    //  HAPUS PENGIRIMAN
    // ─────────────────────────────────────────────────────────
    private fun showDeleteDialog(idPengiriman: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pengiriman")
            .setMessage("Apakah Anda yakin ingin menghapus data pengiriman ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.pengirimanDao().deletePengirimanById(idPengiriman)
                    muatData()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}