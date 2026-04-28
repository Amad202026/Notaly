package com.kel4.notaly.pengiriman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Pengiriman
import java.text.NumberFormat
import java.util.Locale

class PengirimanAdapter(
    private val onItemClick: (Pengiriman) -> Unit,
    private val onEditClick: (Pengiriman) -> Unit,
    private val onDeleteClick: (Pengiriman) -> Unit
) : ListAdapter<Pengiriman, PengirimanAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Pengiriman>() {
            override fun areItemsTheSame(oldItem: Pengiriman, newItem: Pengiriman) =
                oldItem.idPengiriman == newItem.idPengiriman
            override fun areContentsTheSame(oldItem: Pengiriman, newItem: Pengiriman) =
                oldItem == newItem
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIdPengiriman: TextView = itemView.findViewById(R.id.tvIdPengiriman)
        val tvNamaEkspedisi: TextView = itemView.findViewById(R.id.tvNamaEkspedisi)
        val tvNoResi: TextView = itemView.findViewById(R.id.tvNoResi)
        val tvAlamat: TextView = itemView.findViewById(R.id.tvAlamat)
        val tvBiaya: TextView = itemView.findViewById(R.id.tvBiaya)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pengiriman, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvIdPengiriman.text = "#${item.idPengiriman}"
        holder.tvNamaEkspedisi.text = item.namaEkspedisi ?: "-"
        holder.tvNoResi.text = item.noResi ?: "-"
        holder.tvAlamat.text = item.alamatLengkap ?: "-"

        val biaya = item.biayaKirim?.let {
            "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(it)}"
        } ?: "Rp 0"
        holder.tvBiaya.text = biaya

        val status = item.statusKirim ?: "Diproses"
        holder.tvStatus.text = status

        val statusColor = when (status) {
            "Terkirim"   -> 0xFF2E7D5B.toInt()
            "Dikirim"    -> 0xFF1976D2.toInt()
            "Diproses"   -> 0xFFFF8F00.toInt()
            "Dibatalkan" -> 0xFFD32F2F.toInt()
            else         -> 0xFF888888.toInt()
        }
        holder.tvStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivEdit.setOnClickListener { onEditClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }
}