package com.kel4.notaly.supplier

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import com.kel4.notaly.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupplierActivity : AppCompatActivity() {

    private lateinit var rvSupplier: RecyclerView
    private lateinit var tvKosong: TextView
    private lateinit var adapter: SupplierAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier)

        initViews()
    }

    private fun initViews() {
        rvSupplier = findViewById(R.id.rvSupplier)
        tvKosong   = findViewById(R.id.tvKosong)

        val btnBack           = findViewById<ImageView>(R.id.btnBack)
        val menuTambahSupplier = findViewById<CardView>(R.id.menuTambahSupplier)

        rvSupplier.layoutManager = LinearLayoutManager(this)

        adapter = SupplierAdapter(
            listSupplier  = emptyList(),
            onDetailKlik  = { supplier -> bukaDetail(supplier) },
            onHapusKlik   = { supplier -> tampilkanDialogHapus(supplier) }
        )
        rvSupplier.adapter = adapter

        btnBack.setOnClickListener { finish() }
        menuTambahSupplier.setOnClickListener {
            startActivity(Intent(this, TambahSupplierActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        muatDataSupplier()
    }

    // ── Load Data ─────────────────────────────────────────────
    private fun muatDataSupplier() {
        lifecycleScope.launch(Dispatchers.IO) {
            val daftar = AppDatabase.getDatabase(this@SupplierActivity)
                .supplierDao()
                .ambilSemuaSupplier()

            withContext(Dispatchers.Main) {
                val adaData = daftar.isNotEmpty()
                tvKosong.visibility   = if (adaData) View.GONE else View.VISIBLE
                rvSupplier.visibility = if (adaData) View.VISIBLE else View.GONE
                if (adaData) adapter.updateData(daftar)
            }
        }
    }

    // ── Navigasi ke Detail ────────────────────────────────────
    private fun bukaDetail(supplier: Supplier) {
        val intent = Intent(this, DetailSupplierActivity::class.java).apply {
            putExtra(DetailSupplierActivity.EXTRA_ID_SUPPLIER, supplier.idSupplier)
        }
        startActivity(intent)
    }

    // ── Hapus Supplier ────────────────────────────────────────
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
                Toast.makeText(
                    this@SupplierActivity,
                    "${supplier.namaSupplier} berhasil dihapus",
                    Toast.LENGTH_SHORT
                ).show()
                muatDataSupplier()
            }
        }
    }
}