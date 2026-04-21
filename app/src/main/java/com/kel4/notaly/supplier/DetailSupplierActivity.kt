package com.kel4.notaly.supplier

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailSupplierActivity : AppCompatActivity() {

    private lateinit var tvAvatarInisial: TextView
    private lateinit var tvNamaSupplier: TextView
    private lateinit var tvBadgeKategori: TextView
    private lateinit var tvIdSupplier: TextView
    private lateinit var tvNoWa: TextView
    private lateinit var btnHubungiWa: TextView

    private lateinit var tvDetailNama: TextView
    private lateinit var tvDetailId: TextView
    private lateinit var tvDetailWa: TextView
    private lateinit var tvDetailKategori: TextView
    private lateinit var tvDetailAsalDaerah: TextView

    private lateinit var btnBack: ImageView
    private lateinit var btnEdit: ImageView
    private lateinit var btnHapus: ImageView
    private lateinit var btnWhatsapp: Button

    private var supplier: Supplier? = null

    companion object {
        const val EXTRA_ID_SUPPLIER = "extra_id_supplier"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_detail)

        initViews()
        setupListeners()
        muatDataSupplier()
    }

    private fun initViews() {
        tvAvatarInisial    = findViewById(R.id.tvAvatarInisial)
        tvNamaSupplier     = findViewById(R.id.tvNamaSupplier)
        tvBadgeKategori    = findViewById(R.id.tvBadgeKategori)
        tvIdSupplier       = findViewById(R.id.tvIdSupplier)
        tvNoWa             = findViewById(R.id.tvNoWa)
        btnHubungiWa       = findViewById(R.id.btnHubungiWa)

        tvDetailNama       = findViewById(R.id.tvDetailNama)
        tvDetailId         = findViewById(R.id.tvDetailId)
        tvDetailWa         = findViewById(R.id.tvDetailWa)
        tvDetailKategori   = findViewById(R.id.tvDetailKategori)
        tvDetailAsalDaerah = findViewById(R.id.tvDetailAsalDaerah)

        btnBack    = findViewById(R.id.btnBack)
        btnEdit    = findViewById(R.id.btnEdit)
        btnHapus   = findViewById(R.id.btnHapus)
        btnWhatsapp = findViewById(R.id.btnWhatsapp)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Buka TambahSupplierActivity dalam mode Edit
        btnEdit.setOnClickListener {
            supplier?.let { s ->
                val intent = Intent(this, TambahSupplierActivity::class.java).apply {
                    putExtra(TambahSupplierActivity.EXTRA_ID_SUPPLIER_EDIT, s.idSupplier)
                }
                startActivity(intent)
            }
        }

        btnHapus.setOnClickListener {
            supplier?.let { tampilkanDialogHapus(it) }
        }

        btnWhatsapp.setOnClickListener { bukaWhatsApp() }
        btnHubungiWa.setOnClickListener { bukaWhatsApp() }
    }

    // ── Load Data dari Database ───────────────────────────────
    private fun muatDataSupplier() {
        val id = intent.getIntExtra(EXTRA_ID_SUPPLIER, -1)

        if (id == -1) {
            Toast.makeText(this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val hasil = AppDatabase.getDatabase(this@DetailSupplierActivity)
                .supplierDao()
                .ambilSupplierById(id)

            withContext(Dispatchers.Main) {
                if (hasil == null) {
                    Toast.makeText(this@DetailSupplierActivity, "Supplier tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    supplier = hasil
                    tampilkanData(hasil)
                }
            }
        }
    }

    // ── Isi UI dengan data supplier ───────────────────────────
    private fun tampilkanData(s: Supplier) {
        val inisial  = s.namaSupplier.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
        val nomorWa  = s.noWa?.takeIf { it.isNotBlank() } ?: "-"
        val kategori = s.kategoriSuplai?.takeIf { it.isNotBlank() } ?: "SUPPLIER"

        tvAvatarInisial.text    = inisial
        tvNamaSupplier.text     = s.namaSupplier
        tvIdSupplier.text       = "ID: ${s.idSupplier}"
        tvBadgeKategori.text    = kategori.uppercase()
        tvNoWa.text             = nomorWa

        tvDetailNama.text       = s.namaSupplier
        tvDetailId.text         = s.idSupplier.toString()
        tvDetailWa.text         = nomorWa
        tvDetailKategori.text   = kategori
        tvDetailAsalDaerah.text = s.asalDaerah?.takeIf { it.isNotBlank() } ?: "-"
    }

    // ── Buka WhatsApp ─────────────────────────────────────────
    private fun bukaWhatsApp() {
        val noWa = supplier?.noWa?.takeIf { it.isNotBlank() }

        if (noWa == null) {
            Toast.makeText(this, "Nomor WhatsApp tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val nomorBersih = noWa
            .replace(Regex("[\\s\\-]"), "")
            .let { if (it.startsWith("0")) "62${it.substring(1)}" else it }
            .removePrefix("+")

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$nomorBersih")))
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Hapus Supplier ────────────────────────────────────────
    private fun tampilkanDialogHapus(s: Supplier) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Supplier")
            .setMessage("Yakin ingin menghapus \"${s.namaSupplier}\"?\nData tidak dapat dikembalikan.")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapus(s) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(s: Supplier) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@DetailSupplierActivity)
                .supplierDao()
                .hapusSupplier(s)

            withContext(Dispatchers.Main) {
                Toast.makeText(this@DetailSupplierActivity, "${s.namaSupplier} berhasil dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // ── Refresh setelah kembali dari Edit ─────────────────────
    override fun onResume() {
        super.onResume()
        if (supplier != null) muatDataSupplier()
    }
}