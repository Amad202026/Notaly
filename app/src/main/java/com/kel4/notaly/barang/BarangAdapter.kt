package com.kel4.notaly.barang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        // Menggunakan item_barang.xml yang baru dibuat
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
            // Memasukkan data dari Entity Barang ke UI
            tvKategoriPill.text = barang.kategori.uppercase()
            tvNamaBarang.text = barang.namaBarang
            tvStokBarang.text = "Stok: ${barang.stok} Pcs"

            // Format angka menjadi Rupiah otomatis (opsional biar rapi)
            val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            tvHargaBarang.text = formatRupiah.format(barang.hargaJual).replace("Rp", "Rp ")

            // Aksi tombol
            btnDetail.setOnClickListener { onClick(barang) }
            btnDelete.setOnClickListener { onDelete(barang) }
        }
    }
}