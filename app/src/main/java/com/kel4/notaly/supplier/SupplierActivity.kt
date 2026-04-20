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

        // 1. Inisialisasi UI
        rvSupplier = findViewById(R.id.rvSupplier)
        tvKosong = findViewById(R.id.tvKosong)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val menuTambahSupplier = findViewById<CardView>(R.id.menuTambahSupplier)

        // 2. Setup RecyclerView & Adapter
        rvSupplier.layoutManager = LinearLayoutManager(this)

        // Memasukkan logika hapus ke dalam adapter melalui parameter kedua
        adapter = SupplierAdapter(emptyList()) { supplier ->
            tampilkanDialogKonfirmasi(supplier)
        }
        rvSupplier.adapter = adapter

        // 3. Listener Tombol
        btnBack.setOnClickListener { finish() }

        menuTambahSupplier.setOnClickListener {
            startActivity(Intent(this, TambahSupplierActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        muatDataSupplier()
    }

    private fun muatDataSupplier() {
        val db = AppDatabase.getDatabase(this)
        val supplierDao = db.supplierDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val daftarSupplier = supplierDao.ambilSemuaSupplier()

            withContext(Dispatchers.Main) {
                if (daftarSupplier.isEmpty()) {
                    tvKosong.visibility = View.VISIBLE
                    rvSupplier.visibility = View.GONE
                } else {
                    tvKosong.visibility = View.GONE
                    rvSupplier.visibility = View.VISIBLE
                    adapter.updateData(daftarSupplier)
                }
            }
        }
    }

    // --- FITUR HAPUS ---

    private fun tampilkanDialogKonfirmasi(supplier: Supplier) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Supplier")
            .setMessage("Apakah Anda yakin ingin menghapus ${supplier.namaSupplier}?")
            .setPositiveButton("Hapus") { _, _ ->
                eksekusiHapus(supplier)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(supplier: Supplier) {
        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            db.supplierDao().hapusSupplier(supplier)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@SupplierActivity, "Supplier berhasil dihapus", Toast.LENGTH_SHORT).show()
                muatDataSupplier() // Refresh list setelah data hilang dari database
            }
        }
    }
}