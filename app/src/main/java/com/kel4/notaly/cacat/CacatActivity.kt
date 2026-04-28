package com.kel4.notaly.cacat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class CacatActivity : AppCompatActivity() {

    // Satu baris data = satu entri cacat, representasi sederhana tanpa data class
    // index 0=nama, 1=kategori, 2=jumlah, 3=harga, 4=keterangan, 5=tanggal
    private var dataFull : List<Array<String>> = emptyList()

    private lateinit var rvCacat  : RecyclerView
    private lateinit var tvKosong : TextView
    private lateinit var adapter  : InlineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cacat)

        rvCacat  = findViewById(R.id.rvCacat)
        tvKosong = findViewById(R.id.tvKosong)

        rvCacat.layoutManager = LinearLayoutManager(this)
        adapter = InlineAdapter(emptyList())
        rvCacat.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, TambahCacatActivity::class.java))
        }

        val etCari = findViewById<EditText>(R.id.etCari)
        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        muatData()
    }

    private fun muatData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db      = AppDatabase.getDatabase(this@CacatActivity)
            val semua   = db.barangDao().ambilSemuaBarang()
            val prefs   = getSharedPreferences("NotalyPrefs", Context.MODE_PRIVATE)
            val hasil   = mutableListOf<Array<String>>()

            for (barang in semua) {
                val id      = barang.idBarang
                val jumlahList = prefs.getString("CACAT_JUMLAH_$id", "")!!.split("|").filter { it.isNotEmpty() }
                val hargaList  = prefs.getString("CACAT_HARGA_$id",  "")!!.split("|").filter { it.isNotEmpty() }
                val ketList    = prefs.getString("CACAT_KET_$id",    "")!!.split("|").filter { it.isNotEmpty() }
                val tglList    = prefs.getString("CACAT_TGL_$id",    "")!!.split("|").filter { it.isNotEmpty() }

                for (i in jumlahList.indices) {
                    hasil.add(arrayOf(
                        barang.namaBarang,
                        barang.kategori,
                        jumlahList.getOrElse(i) { "0" },
                        hargaList.getOrElse(i)  { "0" },
                        ketList.getOrElse(i)    { "-" },
                        tglList.getOrElse(i)    { "-" }
                    ))
                }
            }

            dataFull = hasil.reversed() // terbaru di atas

            withContext(Dispatchers.Main) { tampilkan(dataFull) }
        }
    }

    private fun filterData(query: String) {
        if (query.isBlank()) { tampilkan(dataFull); return }
        val lower = query.lowercase()
        tampilkan(dataFull.filter {
            it[0].lowercase().contains(lower) || it[1].lowercase().contains(lower) || it[4].lowercase().contains(lower)
        })
    }

    private fun tampilkan(data: List<Array<String>>) {
        tvKosong.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        rvCacat.visibility  = if (data.isEmpty()) View.GONE    else View.VISIBLE
        adapter.update(data)
    }

    // ── Adapter inline, tidak perlu file terpisah ─────────────────────────────
    inner class InlineAdapter(
        private var data: List<Array<String>>
    ) : RecyclerView.Adapter<InlineAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama : TextView = v.findViewById(R.id.tvNamaBarang)
            val tvKat  : TextView = v.findViewById(R.id.tvKategori)
            val tvJml  : TextView = v.findViewById(R.id.tvJumlahCacat)
            val tvHrg  : TextView = v.findViewById(R.id.tvHargaObral)
            val tvKet  : TextView = v.findViewById(R.id.tvKeterangan)
            val tvTgl  : TextView = v.findViewById(R.id.tvTanggal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_cacat, parent, false))

        override fun getItemCount() = data.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val it  = data[pos]
            val fmt = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            h.tvNama.text = it[0]
            h.tvKat .text = it[1]
            h.tvJml .text = "${it[2]} Pcs"
            h.tvHrg .text = fmt.format(it[3].toIntOrNull() ?: 0).replace("Rp", "Rp ")
            h.tvKet .text = it[4]
            h.tvTgl .text = it[5]
        }

        fun update(newData: List<Array<String>>) { data = newData; notifyDataSetChanged() }
    }
}