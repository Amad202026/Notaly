package com.kel4.notaly.pelanggan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.model.Pelanggan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PelangganActivity : AppCompatActivity() {

    private lateinit var rvPelanggan:  RecyclerView
    private lateinit var tvKosong:     TextView
    private lateinit var etCari:       EditText
    private lateinit var adapter:      PelangganAdapter

    private var dataFull: List<Pelanggan> = emptyList()
    private var filterAktif: String = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pelanggan)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        muatData()
    }

    private fun initViews() {
        rvPelanggan = findViewById(R.id.rvSupplier)
        tvKosong    = findViewById(R.id.tvKosong)
        etCari      = findViewById(R.id.etCari)

        rvPelanggan.layoutManager = LinearLayoutManager(this)
        adapter = PelangganAdapter(emptyList()) { pelanggan ->
            val intent = Intent(this, DetailPelangganActivity::class.java)
            intent.putExtra("ID_PELANGGAN", pelanggan.idPelanggan)
            startActivity(intent)
        }
        rvPelanggan.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, TambahPelangganActivity::class.java))
        }

        // Filter chip
        findViewById<LinearLayout>(R.id.btnFilter).setOnClickListener {
            tampilkanFilterDialog()
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun muatData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PelangganActivity)
            dataFull = db.pelangganDao().ambilSemuaPelanggan()
            withContext(Dispatchers.Main) {
                filterData(etCari.text.toString())
            }
        }
    }

    private fun filterData(query: String) {
        val lower = query.lowercase()
        var hasil = if (lower.isBlank()) dataFull
        else dataFull.filter {
            it.namaPelanggan.lowercase().contains(lower) ||
                    it.asalDaerah?.lowercase()?.contains(lower) == true ||
                    it.noWa?.contains(lower) == true
        }
        if (filterAktif != "Semua") {
            hasil = hasil.filter {
                it.kategoriPelanggan.equals(filterAktif, ignoreCase = true)
            }
        }
        tampilkanData(hasil)
    }

    private fun tampilkanFilterDialog() {
        val pilihan = arrayOf("Semua", "Umum", "Grosir", "Member")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Kategori")
            .setSingleChoiceItems(pilihan, pilihan.indexOf(filterAktif)) { dialog, which ->
                filterAktif = pilihan[which]
                dialog.dismiss()
                filterData(etCari.text.toString())
            }
            .show()
    }

    private fun tampilkanData(list: List<Pelanggan>) {
        val adaData = list.isNotEmpty()
        tvKosong.visibility    = if (adaData) View.GONE    else View.VISIBLE
        rvPelanggan.visibility = if (adaData) View.VISIBLE else View.GONE
        adapter.updateData(list)
    }
}