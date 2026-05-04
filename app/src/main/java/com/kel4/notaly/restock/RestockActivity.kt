package com.kel4.notaly.restock

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import com.kel4.notaly.model.BarangMasuk
import com.kel4.notaly.model.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RestockActivity : AppCompatActivity() {

    private lateinit var etTanggal:      EditText
    private lateinit var spinnerPemasok: Spinner
    private lateinit var spinnerBarang:  Spinner
    private lateinit var etQty:          EditText
    private lateinit var etHargaBeli:    EditText
    private lateinit var tvTotalHarga:   TextView

    private var daftarSupplier:   List<Supplier> = emptyList()
    private var daftarBarangAsli: List<Barang>   = emptyList()
    private var supplierTerpilih: Supplier?       = null
    private var barangTerpilih:   Barang?         = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restock_tambah)

        initViews()
        setupDatePicker()
        muatDataAwal()
        setupKalkulasiOtomatis()
    }

    private fun initViews() {
        etTanggal      = findViewById(R.id.etTanggal)
        spinnerPemasok = findViewById(R.id.spinnerPemasok)
        spinnerBarang  = findViewById(R.id.spinnerBarang)
        etQty          = findViewById(R.id.etQty)
        etHargaBeli    = findViewById(R.id.etHargaBeli)
        tvTotalHarga   = findViewById(R.id.tvTotalHarga)

        // Set adapter kosong sementara untuk Spinner Barang
        spinnerBarang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Pilih Barang"))

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSimpan).setOnClickListener { validasiLaluKonfirmasi() }

        updateLabelTanggal()
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            updateLabelTanggal()
        }

        etTanggal.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateLabelTanggal() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        etTanggal.setText(sdf.format(calendar.time))
    }

    private fun muatDataAwal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@RestockActivity)
            daftarSupplier   = db.supplierDao().ambilSemuaSupplier()
            daftarBarangAsli = db.barangDao().ambilSemuaBarang()

            withContext(Dispatchers.Main) {
                setupSupplierDropdown()
            }
        }
    }

    private fun setupSupplierDropdown() {
        // Tambahkan "Pilih Pemasok" di index ke-0
        val namaSuppliers = mutableListOf("Pilih Pemasok")
        namaSuppliers.addAll(daftarSupplier.map { it.namaSupplier })

        // Gunakan .adapter (bukan .setAdapter)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, namaSuppliers)
        spinnerPemasok.adapter = adapter

        // Gunakan onItemSelectedListener untuk Spinner
        spinnerPemasok.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    // Jika memilih "Pilih Pemasok", kosongkan pilihan
                    supplierTerpilih = null
                    barangTerpilih = null
                    filterBarangBerdasarkanSupplier(null)
                } else {
                    // Karena index 0 adalah "Pilih Pemasok", index data asli dikurangi 1
                    supplierTerpilih = daftarSupplier[position - 1]
                    barangTerpilih = null
                    filterBarangBerdasarkanSupplier(supplierTerpilih?.kategoriSuplai)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterBarangBerdasarkanSupplier(kategori: String?) {
        if (kategori == null || kategori.isEmpty()) {
            spinnerBarang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Pilih Barang"))
            return
        }

        // Filter barang sesuai kategori supplier yang dipilih
        val barangFiltered = daftarBarangAsli.filter {
            it.kategori.equals(kategori, ignoreCase = true)
        }

        if (barangFiltered.isEmpty()) {
            spinnerBarang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Tidak ada barang di kategori ini"))
            return
        }

        // Tambahkan "Pilih Barang" di index ke-0
        val namaBarang = mutableListOf("Pilih Barang")
        namaBarang.addAll(barangFiltered.map { it.namaBarang })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, namaBarang)
        spinnerBarang.adapter = adapter

        spinnerBarang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    barangTerpilih = null
                    etHargaBeli.setText("") // Kosongkan harga jika belum memilih
                } else {
                    barangTerpilih = barangFiltered[position - 1]
                    // Set harga otomatis saat barang dipilih
                    etHargaBeli.setText(barangTerpilih?.hargaModal?.toString() ?: "")
                    hitungTotal()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupKalkulasiOtomatis() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { hitungTotal() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etQty.addTextChangedListener(watcher)
        etHargaBeli.addTextChangedListener(watcher)
    }

    private fun hitungTotal() {
        val qty   = etQty.text.toString().toIntOrNull() ?: 0
        val harga = etHargaBeli.text.toString().toDoubleOrNull() ?: 0.0
        val total = qty * harga

        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        tvTotalHarga.text = formatRupiah.format(total).replace("Rp", "Rp ")
    }

    private fun validasiLaluKonfirmasi() {
        val qty       = etQty.text.toString().toIntOrNull() ?: 0
        val hargaBeli = etHargaBeli.text.toString().toDoubleOrNull() ?: 0.0

        if (supplierTerpilih == null) {
            Toast.makeText(this, "Pilih pemasok terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (barangTerpilih == null) {
            Toast.makeText(this, "Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (qty <= 0) {
            etQty.error = "Qty harus lebih dari 0"
            etQty.requestFocus()
            return
        }
        if (hargaBeli <= 0.0) {
            etHargaBeli.error = "Harga beli harus lebih dari 0"
            etHargaBeli.requestFocus()
            return
        }

        val stockSekarang = barangTerpilih!!.stok
        val namaBarang    = barangTerpilih!!.namaBarang

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Restok")
            .setMessage(
                "Tambah stock \"$namaBarang\" sebanyak $qty pcs?\n\n" +
                        "Stock saat ini: $stockSekarang pcs\n" +
                        "Stock setelah restok: ${stockSekarang + qty} pcs"
            )
            .setPositiveButton("Ya, Tambahkan") { _, _ -> simpanRestok(qty, hargaBeli) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun simpanRestok(qty: Int, hargaBeli: Double) {
        val tanggal = etTanggal.text.toString().trim()
        val db      = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val restokBaru = BarangMasuk(
                    idSupplier   = supplierTerpilih!!.idSupplier,
                    idBarang     = barangTerpilih!!.idBarang,
                    tanggalMasuk = tanggal,
                    qtyMasuk     = qty,
                    hargaBeli    = hargaBeli
                )
                db.barangMasukDao().catatRestok(restokBaru)
                db.barangDao().tambahStok(barangTerpilih!!.idBarang, qty)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RestockActivity, "Restok berhasil dicatat!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RestockActivity,
                        "Terjadi kesalahan: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}