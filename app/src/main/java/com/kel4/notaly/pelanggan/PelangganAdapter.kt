package com.kel4.notaly.pelanggan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Pelanggan

class PelangganAdapter(
    private var listData: List<Pelanggan>,
    private val onItemClick: (Pelanggan) -> Unit
) : RecyclerView.Adapter<PelangganAdapter.ViewHolder>() {

    fun updateData(newList: List<Pelanggan>) {
        listData = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pelanggan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listData[position])
    }

    override fun getItemCount(): Int = listData.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvId:        TextView = view.findViewById(R.id.tvIdPelanggan)
        private val tvNama:      TextView = view.findViewById(R.id.tvNamaPelanggan)
        private val tvKategori:  TextView = view.findViewById(R.id.tvKategoriPelanggan)
        private val tvLokasi:    TextView = view.findViewById(R.id.tvLokasiPelanggan)
        private val tvWa:        TextView = view.findViewById(R.id.tvWaPelanggan)

        fun bind(p: Pelanggan) {
            tvId.text       = String.format("%03d", p.idPelanggan)
            tvNama.text     = p.namaPelanggan
            tvLokasi.text   = p.asalDaerah ?: "-"
            tvWa.text       = p.noWa ?: "-"

            val kategori = p.kategoriPelanggan ?: "Umum"
            tvKategori.text = kategori.uppercase()
            tvKategori.backgroundTintList = android.content.res.ColorStateList.valueOf(
                when (kategori.lowercase()) {
                    "member" -> android.graphics.Color.parseColor("#2E7D5B")
                    "grosir" -> android.graphics.Color.parseColor("#1565C0")
                    else     -> android.graphics.Color.parseColor("#757575")
                }
            )

            itemView.setOnClickListener { onItemClick(p) }
        }
    }
}