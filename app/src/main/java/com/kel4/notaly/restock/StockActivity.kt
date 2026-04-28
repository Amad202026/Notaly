package com.kel4.notaly.restock

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
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
import java.text.NumberFormat
import java.util.Locale

class StockActivity : AppCompatActivity() {

    private lateinit var rvStock  : RecyclerView
    private lateinit var tvKosong : TextView
    private lateinit var etCari   : EditText
    private lateinit var adapter  : StockAdapter

    private var dataFull: List<Any> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restock)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        muatDataBarang()
    }

    private fun initViews() {
        rvStock  = findViewById(R.id.rvStock)
        tvKosong = findViewById(R.id.tvKosong)
        etCari   = findViewById(R.id.etCari)

        rvStock.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList())
        rvStock.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, RestockActivity::class.java))
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun muatDataBarang() {
        lifecycleScope.launch(Dispatchers.IO) {
            val semua = AppDatabase.getDatabase(this@StockActivity)
                .barangDao().ambilSemuaBarang()

            dataFull = buatGrouping(semua)

            withContext(Dispatchers.Main) {
                adapter = StockAdapter(dataFull)
                rvStock.adapter = adapter
                tampilkanData(dataFull)
            }
        }
    }

    /**
     * Grouping:
     * 1. Pisahkan barang normal dan varian cacat (mengandung "-C")
     * 2. Setiap barang normal dikumpulkan bersama varian cacatnya
     * 3. Dikelompokkan per kategori dengan header String
     */
    private fun buatGrouping(semua: List<Barang>): List<Any> {
        val normal = semua.filter { !it.idBarang.contains("-C") }.sortedBy { it.kategori }
        val cacat  = semua.filter {  it.idBarang.contains("-C") }

        val hasil       = mutableListOf<Any>()
        var katSekarang = ""

        for (b in normal) {
            if (b.kategori != katSekarang) {
                katSekarang = b.kategori
                hasil.add(katSekarang)
            }
            val varianCacat = cacat
                .filter { it.idBarang.startsWith("${b.idBarang}-C") }
                .sortedBy { it.idBarang }
            hasil.add(BarangGroup(b, varianCacat))
        }
        return hasil
    }

    private fun filterData(query: String) {
        if (query.isBlank()) { tampilkanData(dataFull); return }
        val lower    = query.lowercase()
        val filtered = dataFull
            .filterIsInstance<BarangGroup>()
            .filter {
                it.normal.namaBarang.lowercase().contains(lower) ||
                        it.normal.kategori.lowercase().contains(lower)
            }
        val rebuilt = mutableListOf<Any>()
        var kat = ""
        for (g in filtered) {
            if (g.normal.kategori != kat) { kat = g.normal.kategori; rebuilt.add(kat) }
            rebuilt.add(g)
        }
        tampilkanData(rebuilt)
    }

    private fun tampilkanData(data: List<Any>) {
        tvKosong.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        rvStock.visibility  = if (data.isEmpty()) View.GONE    else View.VISIBLE
        adapter.updateData(data)
    }

    // ── Data wrapper ──────────────────────────────────────────────────────────
    data class BarangGroup(
        val normal : Barang,
        val cacat  : List<Barang>
    )

    // ── Adapter ───────────────────────────────────────────────────────────────
    inner class StockAdapter(
        private var data: List<Any>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_HEADER = 0
        private val TYPE_ITEM   = 1

        // Header ViewHolder — pakai item_restock_kategori (TextView sederhana)
        inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tvHeaderKategori)
        }

        // Item ViewHolder — pakai item_restock (CardView dengan layoutCacat)
        inner class BarangVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama       : TextView     = v.findViewById(R.id.tvNamaBarang)
            val tvStokNormal : TextView     = v.findViewById(R.id.tvStokNormal)
            val tvStatus     : TextView     = v.findViewById(R.id.tvStatusKondisi)
            val layoutCacat  : LinearLayout = v.findViewById(R.id.layoutCacat)
        }

        override fun getItemViewType(pos: Int) = if (data[pos] is String) TYPE_HEADER else TYPE_ITEM

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER)
            // PERBAIKAN: header pakai item_restock_kategori
                HeaderVH(inf.inflate(R.layout.item_restock_kategori, parent, false))
            else
            // PERBAIKAN: item barang pakai item_restock
                BarangVH(inf.inflate(R.layout.item_restock, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
            val fmt = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            when (holder) {
                is HeaderVH -> holder.tv.text = (data[pos] as String).uppercase()

                is BarangVH -> {
                    val group = data[pos] as BarangGroup
                    val b     = group.normal

                    holder.tvNama      .text = b.namaBarang
                    holder.tvStokNormal.text = "Normal: ${b.stok} pcs"
                    holder.tvStatus    .text = b.statusKondisi ?: "Normal"

                    // Bersihkan dulu sebelum isi ulang (ViewHolder di-recycle)
                    holder.layoutCacat.removeAllViews()

                    if (group.cacat.isNotEmpty()) {
                        holder.layoutCacat.visibility = View.VISIBLE
                        for (c in group.cacat) {
                            val tv = TextView(holder.layoutCacat.context).apply {
                                text     = "${c.idBarang}: Cacat/Obral — ${c.stok} pcs  |  " +
                                        fmt.format(c.hargaJual).replace("Rp", "Rp ")
                                textSize = 11f
                                setTextColor(0xFFE53935.toInt())
                                setPadding(0, 4, 0, 0)
                            }
                            holder.layoutCacat.addView(tv)
                        }
                    } else {
                        holder.layoutCacat.visibility = View.GONE
                    }
                }
            }
        }

        override fun getItemCount() = data.size

        fun updateData(newData: List<Any>) { data = newData; notifyDataSetChanged() }
    }
}