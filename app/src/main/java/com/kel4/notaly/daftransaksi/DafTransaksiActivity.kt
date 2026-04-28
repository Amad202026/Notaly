package com.kel4.notaly.daftransaksi

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
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.daftransaksi.DafTransaksiAdapter
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.TransaksiPenjualan
import com.kel4.notaly.transaksi.DetailTransaksiActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DafTransaksiActivity : AppCompatActivity() {

    // ===================== DAO =====================
    private lateinit var db: AppDatabase

    // ===================== DATA =====================
    private var semuaTransaksi   : List<TransaksiPenjualan> = emptyList()
    private var transaksiFiltered: List<TransaksiPenjualan> = emptyList()

    // ===================== VIEW =====================
    private lateinit var btnBack    : ImageView
    private lateinit var etCari     : EditText
    private lateinit var btnFilter  : LinearLayout
    private lateinit var tvKosong   : TextView
    private lateinit var rvTransaksi: RecyclerView

    // ===================== ADAPTER =====================
    private lateinit var adapter: DafTransaksiAdapter

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
        rvTransaksi = findViewById(R.id.rvSupplier) // ID dari XML existing
    }

    // ─────────────────────────────────────────────────────────
    //  SETUP RECYCLERVIEW
    // ─────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = DafTransaksiAdapter(emptyList()) { transaksi ->
            // Klik item → buka detail
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
        btnBack.setOnClickListener { finish() }

        // Search real-time
        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterTransaksi(s.toString())
            }
        })

        // Filter (tanggal / status) — tampilkan dialog pilihan filter
        btnFilter.setOnClickListener {
            tampilkanDialogFilter()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA DARI DB
    // ─────────────────────────────────────────────────────────
    private fun muatData() {
        lifecycleScope.launch {
            semuaTransaksi = withContext(Dispatchers.IO) {
                db.transaksiPenjualanDao().ambilSemuaTransaksi()
            }
            transaksiFiltered = semuaTransaksi
            updateUI()
        }
    }

    // ─────────────────────────────────────────────────────────
    //  UPDATE UI (kosong / ada data)
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

    // ─────────────────────────────────────────────────────────
    //  FILTER BERDASARKAN KATA KUNCI
    // ─────────────────────────────────────────────────────────
    private fun filterTransaksi(query: String) {
        val q = query.trim().lowercase()
        transaksiFiltered = if (q.isEmpty()) {
            semuaTransaksi
        } else {
            semuaTransaksi.filter { t ->
                t.idTransaksi.lowercase().contains(q) ||
                        t.statusPembayaran.lowercase().contains(q) ||
                        (t.metode?.lowercase()?.contains(q) == true) ||
                        t.tanggalTransaksi.contains(q)
            }
        }
        updateUI()
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER STATUS
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilter() {
        val opsi = arrayOf("Semua", "Lunas", "DP")
        android.app.AlertDialog.Builder(this)
            .setTitle("Filter Status")
            .setItems(opsi) { _, which ->
                val status = opsi[which]
                transaksiFiltered = when (status) {
                    "Semua" -> semuaTransaksi
                    else    -> semuaTransaksi.filter { it.statusPembayaran == status }
                }
                updateUI()
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────
    //  REFRESH SAAT KEMBALI
    // ─────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        muatData()
    }
}