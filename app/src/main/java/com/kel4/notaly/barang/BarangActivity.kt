package com.kel4.notaly.barang

import android.content.Context
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BarangActivity : AppCompatActivity() {

    private lateinit var rvBarang: RecyclerView
    private lateinit var tvKosong: TextView
    private lateinit var etCari: EditText
    private lateinit var btnFilter: LinearLayout
    private lateinit var adapter: BarangAdapter

    // Variabel untuk menyimpan state data
    private var daftarBarangAsli: List<Barang> = emptyList() // Menyimpan semua data dari database
    private var kategoriTerpilih: String = "Semua Kategori" // Default filter
    private var kataKunciCari: String = "" // Default pencarian

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang)

        initViews()
        setupFungsiCariDanFilter()
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang data setiap kali kembali ke halaman ini
        muatDataBarang()
    }

    private fun initViews() {
        rvBarang = findViewById(R.id.rvBarang)
        tvKosong = findViewById(R.id.tvKosong)
        etCari = findViewById(R.id.etCari)
        btnFilter = findViewById(R.id.btnFilter)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val menuTambahBarang = findViewById<CardView>(R.id.menuTambahBarang)

        btnBack.setOnClickListener { finish() }

        menuTambahBarang.setOnClickListener {
            startActivity(Intent(this, TambahBarangActivity::class.java))
        }

        rvBarang.layoutManager = LinearLayoutManager(this)

        adapter = BarangAdapter(
            listBarang = emptyList(),
            onClick = { barang -> bukaDetail(barang) },
            onDelete = { barang -> tampilkanDialogHapus(barang) }
        )
        rvBarang.adapter = adapter
    }

    private fun setupFungsiCariDanFilter() {
        // 1. Setup Fungsi Pencarian (Search)
        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                kataKunciCari = s.toString().trim()
                terapkanFilterPencarian()
            }
        })

        // 2. Setup Tombol Filter Kategori
        btnFilter.setOnClickListener {
            tampilkanDialogFilter()
        }
    }

    private fun tampilkanDialogFilter() {
        // Ambil daftar kategori dari SharedPreferences
        val sharedPref = getSharedPreferences("KategoriPrefs", Context.MODE_PRIVATE)
        val setKategori = sharedPref.getStringSet("DAFTAR_KATEGORI", mutableSetOf()) ?: mutableSetOf()

        // Buat list pilihan untuk dialog, tambahkan opsi "Semua Kategori" di paling atas
        val listPilihan = mutableListOf("Semua Kategori")
        listPilihan.addAll(setKategori)
        val arrayPilihan = listPilihan.toTypedArray()

        // Cari index dari kategori yang sedang terpilih agar ter-highlight (dicentang) di dialog
        val checkedItemIndex = listPilihan.indexOf(kategoriTerpilih).takeIf { it >= 0 } ?: 0

        // Tampilkan Dialog
        AlertDialog.Builder(this)
            .setTitle("Filter Kategori")
            .setSingleChoiceItems(arrayPilihan, checkedItemIndex) { dialog, which ->
                // Saat user memilih salah satu, simpan pilihan dan jalankan fungsi filter
                kategoriTerpilih = arrayPilihan[which]
                terapkanFilterPencarian()
                dialog.dismiss() // Langsung tutup dialog setelah memilih agar UX-nya cepat
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Fungsi inti untuk menggabungkan hasil Cari dan Filter
    private fun terapkanFilterPencarian() {
        var hasilFilter = daftarBarangAsli

        // A. Terapkan Filter Kategori (Jika bukan "Semua Kategori")
        if (kategoriTerpilih != "Semua Kategori") {
            hasilFilter = hasilFilter.filter {
                it.kategori.equals(kategoriTerpilih, ignoreCase = true)
            }
        }

        // B. Terapkan Filter Pencarian Nama Barang
        if (kataKunciCari.isNotEmpty()) {
            hasilFilter = hasilFilter.filter {
                it.namaBarang.contains(kataKunciCari, ignoreCase = true)
            }
        }

        // C. Perbarui Tampilan RecyclerView
        val adaData = hasilFilter.isNotEmpty()
        tvKosong.visibility = if (adaData) View.GONE else View.VISIBLE
        rvBarang.visibility = if (adaData) View.VISIBLE else View.GONE

        // Update teks kosong jika datanya tidak ditemukan karena filter
        if (!adaData && daftarBarangAsli.isNotEmpty()) {
            tvKosong.text = "Barang tidak ditemukan"
        } else if (!adaData && daftarBarangAsli.isEmpty()) {
            tvKosong.text = "Belum ada Daftar Barang"
        }

        adapter.updateData(hasilFilter)
    }

    private fun muatDataBarang() {
        lifecycleScope.launch(Dispatchers.IO) {
            val daftarBarang = AppDatabase.getDatabase(this@BarangActivity)
                .barangDao()
                .ambilSemuaBarang()

            withContext(Dispatchers.Main) {
                // Simpan data asli ke variabel global
                daftarBarangAsli = daftarBarang

                // Langsung jalankan filter (berguna jika user habis menghapus barang
                // tapi sedang berada di dalam filter/pencarian tertentu)
                terapkanFilterPencarian()
            }
        }
    }

    private fun bukaDetail(barang: Barang) {
        val intent = Intent(this, DetailBarangActivity::class.java)
        intent.putExtra(DetailBarangActivity.EXTRA_ID_BARANG, barang.idBarang)
        startActivity(intent)
    }

    private fun tampilkanDialogHapus(barang: Barang) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Barang")
            .setMessage("Yakin ingin menghapus \"${barang.namaBarang}\"?")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapus(barang) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapus(barang: Barang) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@BarangActivity)
                .barangDao()
                .hapusBarang(barang)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@BarangActivity,
                    "${barang.namaBarang} berhasil dihapus",
                    Toast.LENGTH_SHORT
                ).show()
                muatDataBarang()
            }
        }
    }
}