package com.kel4.notaly.barang

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Barang
import java.text.NumberFormat
import java.util.Locale

class BarangAdapter(
    private var listBarang: List<Barang>,
    private val onClick: (Barang) -> Unit,
    private val onDelete: (Barang) -> Unit
) : RecyclerView.Adapter<BarangAdapter.BarangViewHolder>() {

    fun updateData(newList: List<Barang>) {
        listBarang = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarangViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_barang, parent, false)
        return BarangViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarangViewHolder, position: Int) {
        holder.bind(listBarang[position])
    }

    override fun getItemCount(): Int = listBarang.size

    inner class BarangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvKategoriPill: TextView = itemView.findViewById(R.id.tvKategoriPill)
        private val tvNamaBarang: TextView   = itemView.findViewById(R.id.tvNamaBarang)
        private val tvStokBarang: TextView   = itemView.findViewById(R.id.tvStokBarang)
        private val tvHargaBarang: TextView  = itemView.findViewById(R.id.tvHargaBarang)
        private val btnDelete: ImageView     = itemView.findViewById(R.id.btnDelete)
        private val btnDetail: TextView      = itemView.findViewById(R.id.btnDetail)

        fun bind(barang: Barang) {
            tvKategoriPill.text = barang.kategori.uppercase()
            tvNamaBarang.text   = barang.namaBarang

            // Ambil stokMin dari SharedPreferences
            val sharedPref = itemView.context.getSharedPreferences("DataEkstraBarang", Context.MODE_PRIVATE)
            val stokMin    = sharedPref.getInt("STOKMIN_${barang.idBarang}", 0)

            // Tampilkan label stok, warna merah jika stok kritis
            tvStokBarang.text = "Stok: ${barang.stok} Pcs"
            if (stokMin > 0 && barang.stok <= stokMin) {
                tvStokBarang.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            } else {
                tvStokBarang.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
            }

            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvHargaBarang.text = formatRupiah.format(barang.hargaJual).replace("Rp", "Rp ")

            btnDetail.setOnClickListener { onClick(barang) }
            btnDelete.setOnClickListener { onDelete(barang) }
        }
    }
}