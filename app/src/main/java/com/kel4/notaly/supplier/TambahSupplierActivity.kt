package com.kel4.notaly.supplier

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahSupplierActivity : AppCompatActivity() {

    private lateinit var etNamaSupplier: EditText
    private lateinit var etNoTelp: EditText
    private lateinit var etAlamat: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var btnSimpan: TextView
    private lateinit var btnBatal: TextView
    private lateinit var btnBack: ImageView
    private lateinit var tvIdSupplier: TextView
    private lateinit var tvJudul: TextView

    // Data lama saat mode Edit
    private var supplierLama: Supplier? = null
    private val isEditMode get() = supplierLama != null

    companion object {
        const val EXTRA_ID_SUPPLIER_EDIT = "extra_id_supplier_edit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_tambah)

        initViews()
        setupListeners()
        loadKategoriData()

        // Cek apakah mode Edit (ada ID yang dikirim)
        val idEdit = intent.getIntExtra(EXTRA_ID_SUPPLIER_EDIT, -1)
        if (idEdit != -1) {
            muatDataUntukEdit(idEdit)
        } else {
            // Mode Tambah: ID otomatis dari Room
            tvIdSupplier.text = "Otomatis"
            tvJudul.text = "Tambah Supplier"
        }
    }

    private fun initViews() {
        etNamaSupplier  = findViewById(R.id.etNamaSupplier)
        etNoTelp        = findViewById(R.id.etNoTelp)
        etAlamat        = findViewById(R.id.etAlamat)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        btnSimpan       = findViewById(R.id.btnSimpan)
        btnBatal        = findViewById(R.id.btnBatal)
        btnBack         = findViewById(R.id.btnBack)
        tvIdSupplier    = findViewById(R.id.tvIdSupplier)
        tvJudul         = findViewById(R.id.tvJudul)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnBatal.setOnClickListener { finish() }
        btnSimpan.setOnClickListener { simpanData() }
    }

    // ── Load data lama ke form (mode Edit) ───────────────────
    private fun muatDataUntukEdit(id: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val supplier = AppDatabase.getDatabase(this@TambahSupplierActivity)
                .supplierDao()
                .ambilSupplierById(id)

            withContext(Dispatchers.Main) {
                if (supplier == null) {
                    Toast.makeText(
                        this@TambahSupplierActivity,
                        "Data tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@withContext
                }

                supplierLama = supplier
                tvJudul.text      = "Edit Supplier"
                tvIdSupplier.text = supplier.idSupplier.toString()

                etNamaSupplier.setText(supplier.namaSupplier)
                etNoTelp.setText(supplier.noWa ?: "")
                etAlamat.setText(supplier.asalDaerah ?: "")

                // Set posisi spinner sesuai kategori lama
                val kategoriAdapter = spinnerKategori.adapter
                for (i in 0 until kategoriAdapter.count) {
                    if (kategoriAdapter.getItem(i).toString() == supplier.kategoriSuplai) {
                        spinnerKategori.setSelection(i)
                        break
                    }
                }
            }
        }
    }

    // ── Isi pilihan Spinner dari SharedPreferences ────────────
    private fun loadKategoriData() {
        val sharedPref      = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val daftarKategori  = sharedPref.getStringSet("DAFTAR_KATEGORI", emptySet()) ?: emptySet()

        val pilihan = mutableListOf("Pilih Kategori")
        pilihan += if (daftarKategori.isEmpty()) {
            listOf("Bahan Baku", "Pakaian", "Aksesoris")
        } else {
            daftarKategori.toList().sorted()
        }

        spinnerKategori.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            pilihan
        )
    }

    // ── Simpan / Update data ──────────────────────────────────
    private fun simpanData() {
        val nama    = etNamaSupplier.text.toString().trim()
        val noWa    = etNoTelp.text.toString().trim()
        val alamat  = etAlamat.text.toString().trim()
        val kategori = spinnerKategori.selectedItem.toString()
            .takeIf { it != "Pilih Kategori" } ?: ""

        if (nama.isEmpty()) {
            etNamaSupplier.error = "Nama supplier wajib diisi"
            etNamaSupplier.requestFocus()
            return
        }

        val db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (isEditMode) {
                    // ── Mode Edit: update data lama ──
                    val supplierDiperbarui = supplierLama!!.copy(
                        namaSupplier   = nama,
                        noWa           = noWa,
                        asalDaerah     = alamat,
                        kategoriSuplai = kategori
                    )
                    db.supplierDao().updateSupplier(supplierDiperbarui)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TambahSupplierActivity,
                            "Supplier berhasil diperbarui",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    // ── Mode Tambah: insert baru ──
                    db.supplierDao().tambahSupplier(
                        Supplier(
                            namaSupplier   = nama,
                            noWa           = noWa,
                            asalDaerah     = alamat,
                            kategoriSuplai = kategori
                        )
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@TambahSupplierActivity,
                            "Supplier berhasil disimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@TambahSupplierActivity,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}