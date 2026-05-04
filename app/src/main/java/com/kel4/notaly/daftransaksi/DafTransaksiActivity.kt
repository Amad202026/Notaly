package com.kel4.notaly.daftransaksi

import android.app.DatePickerDialog
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.TransaksiPenjualan
import com.kel4.notaly.transaksi.DetailTransaksiActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DafTransaksiActivity : AppCompatActivity() {

    // ===================== DAO =====================
    private lateinit var db: AppDatabase

    // ===================== DATA =====================
    private var semuaTransaksi   : List<TransaksiPenjualan> = emptyList()
    private var transaksiFiltered: List<TransaksiPenjualan> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var filterStatus   : String  = "Semua"   // "Semua" | "Lunas" | "DP"
    private var filterTglDari  : String? = null       // format "yyyy-MM-dd"
    private var filterTglSampai: String? = null
    private var urutanAktif    : String  = "Terbaru" // label urutan

    // ===================== VIEW =====================
    private lateinit var btnBack    : ImageView
    private lateinit var etCari     : EditText
    private lateinit var btnFilter  : LinearLayout
    private lateinit var tvKosong   : LinearLayout
    private lateinit var rvTransaksi: RecyclerView

    // ===================== ADAPTER =====================
    private lateinit var adapter: DafTransaksiAdapter

    // ===================== FORMAT TANGGAL =====================
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_transaksi)

        db = AppDatabase.getDatabase(this)

        bindView()
        setupRecyclerView()
        setupListeners()
        muatData()
    }

    // ─────────────────────────────────────────────────────────
    //  BIND VIEW
    // ─────────────────────────────────────────────────────────
    private fun bindView() {
        btnBack     = findViewById(R.id.btnBack)
        etCari      = findViewById(R.id.etCari)
        btnFilter   = findViewById(R.id.btnFilter)
        tvKosong    = findViewById(R.id.tvKosong)
        rvTransaksi = findViewById(R.id.rvTransaksi)
    }

    // ─────────────────────────────────────────────────────────
    //  SETUP RECYCLERVIEW
    // ─────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = DafTransaksiAdapter(emptyList()) { transaksi ->
            val intent = Intent(this, DetailTransaksiActivity::class.java)
            intent.putExtra("ID_TRANSAKSI", transaksi.idTransaksi)
            startActivity(intent)
        }
        rvTransaksi.layoutManager = LinearLayoutManager(this)
        rvTransaksi.adapter = adapter
    }

    // ─────────────────────────────────────────────────────────
    //  SETUP LISTENERS
    // ─────────────────────────────────────────────────────────
    private fun setupListeners() {
        btnBack.setOnClickListener {
            startActivity(Intent(this, BerandaActivity::class.java))
            finish()
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { terapkanFilterDanSortir() }
        })

        btnFilter.setOnClickListener { tampilkanDialogFilterDanSortir() }
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA DARI DB
    // ─────────────────────────────────────────────────────────
    private fun muatData() {
        lifecycleScope.launch {
            semuaTransaksi = withContext(Dispatchers.IO) {
                db.transaksiPenjualanDao().ambilSemuaTransaksi()
            }
            terapkanFilterDanSortir()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR (satu dialog, dua bagian)
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        // Opsi yang ditampilkan dalam satu dialog dengan section:
        // [FILTER STATUS]  Semua | Lunas | DP
        // [FILTER TANGGAL] Pilih Rentang Tanggal | Reset Tanggal
        // [URUTKAN]        Terbaru | Terlama | Total ↑ | Total ↓

        val opsiStatus  = arrayOf("Semua", "Lunas", "DP")
        val opsiUrutkan = arrayOf("Terbaru", "Terlama", "Total Terbesar", "Total Terkecil")

        val view = layoutInflater.inflate(android.R.layout.simple_list_item_1, null) // placeholder
        // Kita pakai AlertDialog multi-choice bertingkat via menu sederhana
        val pilihan = mutableListOf<String>()
        pilihan.add("── FILTER STATUS ──")
        opsiStatus.forEach { pilihan.add("   Status: $it") }
        pilihan.add("── FILTER TANGGAL ──")
        pilihan.add("   Pilih Rentang Tanggal...")
        pilihan.add("   Reset Filter Tanggal")
        pilihan.add("── URUTKAN ──")
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Transaksi")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when (which) {
                    // Status: Semua / Lunas / DP
                    1 -> { filterStatus = "Semua";  terapkanFilterDanSortir() }
                    2 -> { filterStatus = "Lunas";  terapkanFilterDanSortir() }
                    3 -> { filterStatus = "DP";     terapkanFilterDanSortir() }
                    // Tanggal
                    5 -> pilihRentangTanggal()
                    6 -> { filterTglDari = null; filterTglSampai = null; terapkanFilterDanSortir() }
                    // Urutan
                    8  -> { urutanAktif = "Terbaru";        terapkanFilterDanSortir() }
                    9  -> { urutanAktif = "Terlama";         terapkanFilterDanSortir() }
                    10 -> { urutanAktif = "Total Terbesar";  terapkanFilterDanSortir() }
                    11 -> { urutanAktif = "Total Terkecil";  terapkanFilterDanSortir() }
                }
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────
    //  PILIH RENTANG TANGGAL (dari → sampai)
    // ─────────────────────────────────────────────────────────
    private fun pilihRentangTanggal() {
        val cal = Calendar.getInstance()

        // Pilih tanggal AWAL
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            filterTglDari = sdf.format(cal.time)

            // Lanjut pilih tanggal AKHIR
            DatePickerDialog(this, { _, y2, m2, d2 ->
                cal.set(y2, m2, d2)
                filterTglSampai = sdf.format(cal.time)
                terapkanFilterDanSortir()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Pilih Tanggal Akhir")
                show()
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Pilih Tanggal Awal")
            show()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  TERAPKAN FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun terapkanFilterDanSortir() {
        val q = etCari.text.toString().trim().lowercase()

        // 1. Filter teks pencarian
        var hasil = if (q.isEmpty()) semuaTransaksi
        else semuaTransaksi.filter { t ->
            t.idTransaksi.lowercase().contains(q) ||
                    t.statusPembayaran.lowercase().contains(q) ||
                    (t.metode?.lowercase()?.contains(q) == true) ||
                    t.tanggalTransaksi.contains(q)
        }

        // 2. Filter status pembayaran
        if (filterStatus != "Semua") {
            hasil = hasil.filter { it.statusPembayaran == filterStatus }
        }

        // 3. Filter rentang tanggal
        val dari    = filterTglDari
        val sampai  = filterTglSampai
        if (dari != null && sampai != null) {
            hasil = hasil.filter { t ->
                t.tanggalTransaksi >= dari && t.tanggalTransaksi <= sampai
            }
        }

        // 4. Sortir
        hasil = when (urutanAktif) {
            "Terbaru"       -> hasil.sortedByDescending { it.tanggalTransaksi }
            "Terlama"       -> hasil.sortedBy { it.tanggalTransaksi }
            "Total Terbesar"-> hasil.sortedByDescending { it.totalBelanja }
            "Total Terkecil"-> hasil.sortedBy { it.totalBelanja }
            else            -> hasil
        }

        transaksiFiltered = hasil
        updateUI()
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE UI
    // ─────────────────────────────────────────────────────────
    private fun updateUI() {
        if (transaksiFiltered.isEmpty()) {
            tvKosong.visibility    = View.VISIBLE
            rvTransaksi.visibility = View.GONE
        } else {
            tvKosong.visibility    = View.GONE
            rvTransaksi.visibility = View.VISIBLE
            adapter.perbarui(transaksiFiltered)
        }
    }

    override fun onResume() {
        super.onResume()
        muatData()
    }
}