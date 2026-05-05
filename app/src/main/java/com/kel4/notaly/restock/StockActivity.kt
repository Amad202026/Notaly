package com.kel4.notaly.restock

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.database.AppDatabase
import com.kel4.notaly.home.BerandaActivity
import com.kel4.notaly.model.Barang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class StockActivity : AppCompatActivity() {

    private lateinit var rvStock  : RecyclerView
    private lateinit var tvKosong : LinearLayout
    private lateinit var etCari   : EditText
    private lateinit var btnFilter: LinearLayout
    private lateinit var adapter  : StockAdapter

    // ===================== STATE DATA =====================
    // dataFull menyimpan semua BarangGroup (tanpa header String)
    // agar filter/sortir bisa dijalankan ulang kapan saja
    private var groupFull: List<BarangGroup> = emptyList()

    // ===================== STATE FILTER & SORTIR =====================
    private var filterKategori : String = "Semua"   // "Semua" atau nama kategori
    private var filterStatusStok: String = "Semua"  // "Semua" | "Normal" | "Menipis" | "Habis" | "Ada Cacat"
    private var urutanAktif    : String = "Kategori" // label urutan

    // Batas stok menipis — sesuaikan kebutuhan bisnis
    private val BATAS_MENIPIS = 5

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
        rvStock   = findViewById(R.id.rvStock)
        tvKosong  = findViewById(R.id.tvKosong)
        etCari    = findViewById(R.id.etCari)
        btnFilter = findViewById(R.id.btnFilter)

        rvStock.layoutManager = LinearLayoutManager(this)
        adapter = StockAdapter(emptyList())
        rvStock.adapter = adapter

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
        findViewById<CardView>(R.id.menuTambah).setOnClickListener {
            startActivity(Intent(this, RestockActivity::class.java))
        }

        etCari.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { terapkanFilterDanSortir() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilter.setOnClickListener { tampilkanDialogFilterDanSortir() }
    }

    // ─────────────────────────────────────────────────────────
    //  MUAT DATA
    // ─────────────────────────────────────────────────────────
    private fun muatDataBarang() {
        lifecycleScope.launch(Dispatchers.IO) {
            val semua = AppDatabase.getDatabase(this@StockActivity)
                .barangDao().ambilSemuaBarang()

            // Simpan sebagai flat list BarangGroup, header dibuat saat render
            groupFull = buatGroupList(semua)

            withContext(Dispatchers.Main) {
                terapkanFilterDanSortir()
            }
        }
    }

    /**
     * Ubah List<Barang> → List<BarangGroup> (tanpa header String).
     * Pemisahan normal vs cacat tetap sama seperti sebelumnya.
     */
    private fun buatGroupList(semua: List<Barang>): List<BarangGroup> {
        val normal = semua.filter { !it.idBarang.contains("-C") }
        val cacat  = semua.filter {  it.idBarang.contains("-C") }

        return normal.map { b ->
            val varianCacat = cacat
                .filter { it.idBarang.startsWith("${b.idBarang}-C") }
                .sortedBy { it.idBarang }
            BarangGroup(b, varianCacat)
        }
    }

    // ─────────────────────────────────────────────────────────
    //  DIALOG FILTER + SORTIR
    // ─────────────────────────────────────────────────────────
    private fun tampilkanDialogFilterDanSortir() {
        // Kategori dinamis dari data
        val kategoriList = mutableListOf("Semua")
        groupFull.map { it.normal.kategori }
            .distinct().sorted()
            .forEach { kategoriList.add(it) }

        val opsiStatus  = arrayOf(
            "Semua",
            "Normal (Stok > $BATAS_MENIPIS)",
            "Menipis (1–$BATAS_MENIPIS)",
            "Habis (Stok = 0)",
            "Ada Varian Cacat/Obral"
        )
        val opsiUrutkan = arrayOf(
            "Kategori (A-Z)",
            "Nama Barang A-Z",
            "Nama Barang Z-A",
            "Stok Terbanyak",
            "Stok Tersedikit"
        )

        val pilihan = mutableListOf<String>()

        // Section: Kategori
        pilihan.add("── FILTER KATEGORI ──")
        kategoriList.forEach { pilihan.add("   Kategori: $it") }

        val headerStatus = pilihan.size
        pilihan.add("── FILTER STATUS STOK ──")
        opsiStatus.forEach { pilihan.add("   Status: $it") }

        val headerUrut = pilihan.size
        pilihan.add("── URUTKAN ──")
        opsiUrutkan.forEach { pilihan.add("   Urut: $it") }

        val startKat    = 1
        val endKat      = kategoriList.size
        val startStatus = headerStatus + 1
        val endStatus   = headerStatus + opsiStatus.size
        val startUrut   = headerUrut + 1

        AlertDialog.Builder(this)
            .setTitle("Filter & Urutkan Stok")
            .setItems(pilihan.toTypedArray()) { _, which ->
                when {
                    which in startKat..endKat -> {
                        filterKategori = kategoriList[which - startKat]
                        terapkanFilterDanSortir()
                    }
                    which in startStatus..endStatus -> {
                        filterStatusStok = opsiStatus[which - startStatus]
                        terapkanFilterDanSortir()
                    }
                    which >= startUrut -> {
                        urutanAktif = opsiUrutkan[which - startUrut]
                        terapkanFilterDanSortir()
                    }
                }
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────
    //  TERAPKAN FILTER + SORTIR → rebuild list dengan header
    // ─────────────────────────────────────────────────────────
    private fun terapkanFilterDanSortir() {
        val q = etCari.text.toString().trim().lowercase()

        // 1. Filter teks: nama barang atau kategori
        var hasil = if (q.isBlank()) groupFull
        else groupFull.filter {
            it.normal.namaBarang.lowercase().contains(q) ||
                    it.normal.kategori.lowercase().contains(q)
        }

        // 2. Filter kategori
        if (filterKategori != "Semua") {
            hasil = hasil.filter { it.normal.kategori.equals(filterKategori, ignoreCase = true) }
        }

        // 3. Filter status stok
        hasil = when {
            filterStatusStok.startsWith("Normal") ->
                hasil.filter { it.normal.stok > BATAS_MENIPIS }
            filterStatusStok.startsWith("Menipis") ->
                hasil.filter { it.normal.stok in 1..BATAS_MENIPIS }
            filterStatusStok.startsWith("Habis") ->
                hasil.filter { it.normal.stok == 0 }
            filterStatusStok.startsWith("Ada Varian") ->
                hasil.filter { it.cacat.isNotEmpty() }
            else -> hasil
        }

        // 4. Sortir
        hasil = when (urutanAktif) {
            "Kategori (A-Z)"   -> hasil.sortedWith(compareBy({ it.normal.kategori }, { it.normal.namaBarang }))
            "Nama Barang A-Z"  -> hasil.sortedBy    { it.normal.namaBarang.lowercase() }
            "Nama Barang Z-A"  -> hasil.sortedByDescending { it.normal.namaBarang.lowercase() }
            "Stok Terbanyak"   -> hasil.sortedByDescending { it.normal.stok }
            "Stok Tersedikit"  -> hasil.sortedBy    { it.normal.stok }
            else               -> hasil
        }

        // 5. Rebuild list dengan header kategori (hanya jika urutan per kategori)
        val rendered = rebuildDenganHeader(hasil)
        tampilkanData(rendered)
    }

    /**
     * Sisipkan header String di antara BarangGroup jika kategori berubah.
     * Header hanya muncul saat urutan "Kategori (A-Z)" atau filter spesifik satu kategori.
     */
    private fun rebuildDenganHeader(groups: List<BarangGroup>): List<Any> {
        val pakai = urutanAktif == "Kategori (A-Z)" || filterKategori == "Semua"
        val result = mutableListOf<Any>()
        var katSekarang = ""
        for (g in groups) {
            if (pakai && g.normal.kategori != katSekarang) {
                katSekarang = g.normal.kategori
                result.add(katSekarang)
            }
            result.add(g)
        }
        return result
    }

    private fun tampilkanData(data: List<Any>) {
        tvKosong.visibility = if (data.filterIsInstance<BarangGroup>().isEmpty()) View.VISIBLE else View.GONE
        rvStock.visibility  = if (data.filterIsInstance<BarangGroup>().isEmpty()) View.GONE    else View.VISIBLE
        adapter.updateData(data)
    }

    // ── Data wrapper ──────────────────────────────────────────────────────────
    data class BarangGroup(
        val normal: Barang,
        val cacat : List<Barang>
    )

    // ── Adapter ───────────────────────────────────────────────────────────────
    inner class StockAdapter(
        private var data: List<Any>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_HEADER = 0
        private val TYPE_ITEM   = 1

        inner class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
            val tv: TextView = v.findViewById(R.id.tvHeaderKategori)
        }

        inner class BarangVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNama      : TextView     = v.findViewById(R.id.tvNamaBarang)
            val tvStokNormal: TextView     = v.findViewById(R.id.tvStokNormal)
            val tvStatus    : TextView     = v.findViewById(R.id.tvStatusKondisi)
            val layoutCacat : LinearLayout = v.findViewById(R.id.layoutCacat)
        }

        override fun getItemViewType(pos: Int) = if (data[pos] is String) TYPE_HEADER else TYPE_ITEM

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER)
                HeaderVH(inf.inflate(R.layout.item_restock_kategori, parent, false))
            else
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

                    // Warnai status stok agar langsung terlihat kondisinya
                    val statusText: String
                    val statusColor: Int
                    when {
                        b.stok == 0 -> {
                            statusText  = "HABIS"
                            statusColor = 0xFFE53935.toInt() // merah
                        }
                        b.stok <= BATAS_MENIPIS -> {
                            statusText  = "MENIPIS"
                            statusColor = 0xFFFB8C00.toInt() // oranye
                        }
                        else -> {
                            statusText  = b.statusKondisi ?: "Normal"
                            statusColor = 0xFF43A047.toInt() // hijau
                        }
                    }
                    holder.tvStatus.text     = statusText
                    holder.tvStatus.setTextColor(statusColor)

                    // Bersihkan sebelum isi ulang (ViewHolder di-recycle)
                    holder.layoutCacat.removeAllViews()

                    if (group.cacat.isNotEmpty()) {
                        holder.layoutCacat.visibility = View.VISIBLE
                        for (c in group.cacat) {
                            val tv = TextView(holder.layoutCacat.context).apply {
                                text = "${c.idBarang}: Cacat/Obral — ${c.stok} pcs  |  " +
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