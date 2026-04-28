package com.kel4.notaly.restock

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
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

    private lateinit var etTanggal:      TextInputEditText
    private lateinit var spinnerPemasok: AutoCompleteTextView
    private lateinit var spinnerBarang:  AutoCompleteTextView
    private lateinit var etQty:          android.widget.EditText
    private lateinit var etHargaBeli:    android.widget.EditText
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

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSimpan).setOnClickListener { validasiLaluKonfirmasi() }

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
        val namaSuppliers = daftarSupplier.map { it.namaSupplier }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, namaSuppliers)
        spinnerPemasok.setAdapter(adapter)

        spinnerPemasok.setOnItemClickListener { _, _, position, _ ->
            supplierTerpilih = daftarSupplier[position]
            spinnerBarang.setText("")
            barangTerpilih = null
            filterBarangBerdasarkanSupplier(supplierTerpilih?.kategoriSuplai)
        }
    }

    private fun filterBarangBerdasarkanSupplier(kategori: String?) {
        if (kategori == null) return

        val barangFiltered = daftarBarangAsli.filter {
            it.kategori.equals(kategori, ignoreCase = true)
        }

        if (barangFiltered.isEmpty()) {
            spinnerBarang.hint = "Tidak ada barang kategori $kategori"
            spinnerBarang.setAdapter(null)
            return
        }

        val namaBarang = barangFiltered.map { it.namaBarang }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, namaBarang)
        spinnerBarang.setAdapter(adapter)

        spinnerBarang.setOnItemClickListener { _, _, position, _ ->
            barangTerpilih = barangFiltered[position]
            etHargaBeli.setText(barangTerpilih?.hargaModal?.toString() ?: "")
            hitungTotal()
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