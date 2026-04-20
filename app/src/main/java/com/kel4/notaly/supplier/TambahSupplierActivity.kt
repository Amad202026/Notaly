package com.kel4.notaly.supplier // Sesuaikan dengan nama package kamu

import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Supplier
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R // Pastikan import R ini benar
// import com.kel4.notaly.database.AppDatabase // Import lokasi database kamu
// import com.kel4.notaly.database.Supplier // Import Entity Supplier kamu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TambahSupplierActivity : AppCompatActivity() {

    // Deklarasi variabel UI
    private lateinit var etNamaSupplier: EditText
    private lateinit var etNoTelp: EditText
    private lateinit var etAlamat: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var btnSimpan: TextView
    private lateinit var btnBatal: TextView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_supplier_tambah) // Ganti sesuai nama file XML kamu

        // 1. Hubungkan variabel dengan ID di XML
        etNamaSupplier = findViewById(R.id.etNamaSupplier)
        etNoTelp = findViewById(R.id.etNoTelp)
        etAlamat = findViewById(R.id.etAlamat)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        btnSimpan = findViewById(R.id.btnSimpan)
        btnBatal = findViewById(R.id.btnBatal)
        btnBack = findViewById(R.id.btnBack)

        // 2. Siapkan data untuk Spinner (Dropdown Kategori)
        val pilihanKategori = arrayOf("Pilih Kategori", "Bahan Baku", "Pakaian Jadi", "Aksesoris", "Kemasan", "Lainnya")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pilihanKategori)
        spinnerKategori.adapter = adapter

        // 3. Tombol Kembali & Batal (Tutup Halaman)
        btnBack.setOnClickListener { finish() }
        btnBatal.setOnClickListener { finish() }

        // 4. Tombol Simpan
        btnSimpan.setOnClickListener {
            simpanKeDatabase()
        }
    }

    private fun simpanKeDatabase() {
        // Ambil teks yang diketik user
        val nama = etNamaSupplier.text.toString().trim()
        val noWa = etNoTelp.text.toString().trim()
        val alamat = etAlamat.text.toString().trim()
        val kategori = spinnerKategori.selectedItem.toString()

        // Validasi: Nama tidak boleh kosong
        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama Supplier harus diisi!", Toast.LENGTH_SHORT).show()
            etNamaSupplier.requestFocus()
            return
        }

        // Jika kategori belum dipilih, ubah jadi teks kosong atau null
        val kategoriFinal = if (kategori == "Pilih Kategori") "" else kategori

        // Bungkus data menjadi objek Supplier (Sesuai Entity yang kita buat sebelumnya)
        // ID tidak dimasukkan karena autoGenerate = true
        val supplierBaru = Supplier(
            namaSupplier = nama,
            noWa = noWa,
            asalDaerah = alamat,
            kategoriSuplai = kategoriFinal
        )

        // Panggil Database
        val db = AppDatabase.getDatabase(this)
        val supplierDao = db.supplierDao()

        // Masukkan ke Database menggunakan Jalur Belakang (Coroutine)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                supplierDao.tambahSupplier(supplierBaru)

                // Kembali ke Jalur Utama untuk menampilkan Toast dan menutup halaman
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahSupplierActivity, "Supplier berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish() // Tutup halaman
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TambahSupplierActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}