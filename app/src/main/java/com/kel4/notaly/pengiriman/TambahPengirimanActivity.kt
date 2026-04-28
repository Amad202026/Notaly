package com.kel4.notaly.pengiriman

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Pengiriman
import kotlinx.coroutines.launch

class TambahPengirimanActivity : AppCompatActivity() {

    private lateinit var tvJudul: TextView
    private lateinit var tvLabelAtas: TextView
    private lateinit var spinnerIdTransaksi: Spinner
    private lateinit var etNamaEkspedisi: EditText
    private lateinit var etNomorResi: EditText
    private lateinit var etAlamatTujuan: EditText
    private lateinit var etBiayaKirim: EditText
    private lateinit var spinnerStatusPengiriman: Spinner
    private lateinit var btnSimpan: Button

    private val db by lazy { AppDatabase.getDatabase(this) }
    private var editId: Int = -1

    // List ID transaksi disimpan agar bisa dicari index-nya secara manual
    private var transaksiIdList: List<String> = emptyList()

    companion object {
        const val EXTRA_ID = "ID_PENGIRIMAN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman_tambah)

        editId = intent.getIntExtra(EXTRA_ID, -1)

        initViews()
        setupStatusSpinner()

        if (editId != -1) {
            tvLabelAtas.text = "EDIT PENGIRIMAN"
            tvJudul.text = "Edit Logistik"
            btnSimpan.text = "Simpan Perubahan"
        }

        // Load daftar transaksi ke spinner, lalu isi form jika mode edit
        loadTransaksiListThenFillForm()

        btnSimpan.setOnClickListener { simpanData() }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initViews() {
        tvJudul                 = findViewById(R.id.tvJudul)
        tvLabelAtas             = findViewById(R.id.tvLabelAtas)
        spinnerIdTransaksi      = findViewById(R.id.spinnerIdTransaksi)
        etNamaEkspedisi         = findViewById(R.id.etNamaEkspedisi)
        etNomorResi             = findViewById(R.id.etNomorResi)
        etAlamatTujuan          = findViewById(R.id.etAlamatTujuan)
        etBiayaKirim            = findViewById(R.id.etBiayaKirim)
        spinnerStatusPengiriman = findViewById(R.id.spinnerStatusPengiriman)
        btnSimpan               = findViewById(R.id.btnSimpan)
    }

    private fun setupStatusSpinner() {
        val statusList = arrayListOf("Diproses", "Dikirim", "Terkirim", "Dibatalkan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusPengiriman.adapter = adapter
    }

    private fun loadTransaksiListThenFillForm() {
        lifecycleScope.launch {
            // Nama DAO sesuai AppDatabase: transaksiPenjualanDao()
            val semuaTransaksi = db.transaksiPenjualanDao().ambilSemuaTransaksi()

            // Ambil field ID_Transaksi dari model TransaksiPenjualan
            // Sesuaikan nama field di bawah jika berbeda di model kamu
            transaksiIdList = semuaTransaksi.map { transaksi -> transaksi.idTransaksi }

            val adapterSpinner = ArrayAdapter(
                this@TambahPengirimanActivity,
                android.R.layout.simple_spinner_item,
                transaksiIdList
            )
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerIdTransaksi.adapter = adapterSpinner

            // Setelah spinner terisi, isi field jika mode edit
            if (editId != -1) {
                fillFormForEdit()
            }
        }
    }

    private suspend fun fillFormForEdit() {
        val p = db.pengirimanDao().getPengirimanById(editId) ?: return

        etNamaEkspedisi.setText(p.namaEkspedisi ?: "")
        etNomorResi.setText(p.noResi ?: "")
        etAlamatTujuan.setText(p.alamatLengkap ?: "")
        etBiayaKirim.setText(p.biayaKirim?.toString() ?: "")

        // Cari index dengan loop manual untuk menghindari ambiguity indexOf
        val targetTransaksi = p.idTransaksi
        var idxTransaksi = 0
        for (i in transaksiIdList.indices) {
            if (transaksiIdList[i] == targetTransaksi) {
                idxTransaksi = i
                break
            }
        }
        spinnerIdTransaksi.setSelection(idxTransaksi)

        // Set status spinner dengan loop manual
        val statusList = listOf("Diproses", "Dikirim", "Terkirim", "Dibatalkan")
        val targetStatus = p.statusKirim
        var idxStatus = 0
        for (i in statusList.indices) {
            if (statusList[i] == targetStatus) {
                idxStatus = i
                break
            }
        }
        spinnerStatusPengiriman.setSelection(idxStatus)
    }

    private fun simpanData() {
        val idTransaksi   = spinnerIdTransaksi.selectedItem?.toString()
        val namaEkspedisi = etNamaEkspedisi.text.toString().trim()
        val noResi        = etNomorResi.text.toString().trim()
        val alamat        = etAlamatTujuan.text.toString().trim()
        val biayaStr      = etBiayaKirim.text.toString().trim()
        val statusKirim   = spinnerStatusPengiriman.selectedItem?.toString()

        if (idTransaksi.isNullOrBlank()) {
            Toast.makeText(this, "Pilih ID Transaksi", Toast.LENGTH_SHORT).show()
            return
        }
        if (namaEkspedisi.isEmpty()) {
            etNamaEkspedisi.error = "Nama ekspedisi wajib diisi"
            etNamaEkspedisi.requestFocus(); return
        }
        if (noResi.isEmpty()) {
            etNomorResi.error = "Nomor resi wajib diisi"
            etNomorResi.requestFocus(); return
        }
        if (alamat.isEmpty()) {
            etAlamatTujuan.error = "Alamat tujuan wajib diisi"
            etAlamatTujuan.requestFocus(); return
        }
        if (biayaStr.isEmpty()) {
            etBiayaKirim.error = "Biaya kirim wajib diisi"
            etBiayaKirim.requestFocus(); return
        }
        val biaya = biayaStr.toDoubleOrNull()
        if (biaya == null || biaya < 0) {
            etBiayaKirim.error = "Biaya tidak valid"
            etBiayaKirim.requestFocus(); return
        }

        lifecycleScope.launch {
            val pengiriman = Pengiriman(
                idPengiriman  = if (editId != -1) editId else 0,
                idTransaksi   = idTransaksi,
                namaEkspedisi = namaEkspedisi,
                noResi        = noResi,
                alamatLengkap = alamat,
                biayaKirim    = biaya,
                statusKirim   = statusKirim
            )

            if (editId != -1) {
                db.pengirimanDao().updateStatusPengiriman(pengiriman)
                Toast.makeText(
                    this@TambahPengirimanActivity,
                    "Data berhasil diperbarui", Toast.LENGTH_SHORT
                ).show()
            } else {
                db.pengirimanDao().buatPengiriman(pengiriman)
                Toast.makeText(
                    this@TambahPengirimanActivity,
                    "Data berhasil disimpan", Toast.LENGTH_SHORT
                ).show()
            }
            finish()
        }
    }
}