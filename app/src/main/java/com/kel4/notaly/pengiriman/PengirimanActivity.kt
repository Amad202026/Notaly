package com.kel4.notaly.pengiriman

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.pengiriman.TambahPengirimanActivity
import kotlinx.coroutines.launch

class PengirimanActivity : AppCompatActivity() {

    private lateinit var rvPengiriman: RecyclerView
    private lateinit var tvKosong: TextView
    private lateinit var etCari: EditText
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnFilter: LinearLayout
    private lateinit var menuTambah: View

    private lateinit var adapter: PengirimanAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    private var filterStatus: String = "Semua"
    private var isFilterVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengiriman)

        initViews()
        setupRecyclerView()
        setupSearch()
        setupFilter()
        setupFab()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun initViews() {
        rvPengiriman = findViewById(R.id.rvPengiriman)
        tvKosong    = findViewById(R.id.tvKosong)
        etCari      = findViewById(R.id.etCari)
        btnFilter   = findViewById(R.id.btnFilter)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        menuTambah  = findViewById(R.id.menuTambah)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PengirimanAdapter(
            onItemClick = { pengiriman ->
                val intent = Intent(this, DetailPengirimanActivity::class.java)
                intent.putExtra("ID_PENGIRIMAN", pengiriman.idPengiriman)
                startActivity(intent)
            },
            onEditClick = { pengiriman ->
                val intent = Intent(this, TambahPengirimanActivity::class.java)
                intent.putExtra("ID_PENGIRIMAN", pengiriman.idPengiriman)
                startActivity(intent)
            },
            onDeleteClick = { pengiriman ->
                showDeleteDialog(pengiriman.idPengiriman)
            }
        )
        rvPengiriman.layoutManager = LinearLayoutManager(this)
        rvPengiriman.adapter = adapter
    }

    private fun setupSearch() {
        etCari.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplay(s.toString(), filterStatus)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilter() {
        val statusList = listOf("Semua", "Diproses", "Dikirim", "Terkirim", "Dibatalkan")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapterSpinner

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterStatus = statusList[position]
                filterAndDisplay(etCari.text.toString(), filterStatus)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnFilter.setOnClickListener {
            isFilterVisible = !isFilterVisible
            spinnerFilter.visibility = if (isFilterVisible) View.VISIBLE else View.GONE
            if (isFilterVisible) spinnerFilter.performClick()
        }
    }

    private fun setupFab() {
        menuTambah.setOnClickListener {
            startActivity(Intent(this, TambahPengirimanActivity::class.java))
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val semua = db.pengirimanDao().getAllPengiriman()
            filterAndDisplayList(semua, etCari.text.toString(), filterStatus)
        }
    }

    private fun filterAndDisplay(query: String, status: String) {
        lifecycleScope.launch {
            val semua = db.pengirimanDao().getAllPengiriman()
            filterAndDisplayList(semua, query, status)
        }
    }

    private fun filterAndDisplayList(
        list: List<com.kel4.notaly.model.Pengiriman>,
        query: String,
        status: String
    ) {
        var hasil = list
        if (status != "Semua") {
            hasil = hasil.filter { it.statusKirim == status }
        }
        if (query.isNotBlank()) {
            hasil = hasil.filter {
                it.namaEkspedisi?.contains(query, ignoreCase = true) == true ||
                        it.noResi?.contains(query, ignoreCase = true) == true ||
                        it.idTransaksi?.contains(query, ignoreCase = true) == true ||
                        it.alamatLengkap?.contains(query, ignoreCase = true) == true
            }
        }

        adapter.submitList(hasil)

        if (hasil.isEmpty()) {
            rvPengiriman.visibility = View.GONE
            tvKosong.visibility = View.VISIBLE
            tvKosong.text = if (query.isNotBlank() || status != "Semua")
                "Tidak ada hasil yang cocok"
            else
                "Belum ada Daftar Pengiriman"
        } else {
            rvPengiriman.visibility = View.VISIBLE
            tvKosong.visibility = View.GONE
        }
    }

    private fun showDeleteDialog(idPengiriman: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hapus Pengiriman")
            .setMessage("Apakah Anda yakin ingin menghapus data pengiriman ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.pengirimanDao().deletePengirimanById(idPengiriman)
                    loadData()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}