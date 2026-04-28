package com.kel4.notaly.daftransaksi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.TransaksiPenjualan
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DafTransaksiAdapter(
    private var listTransaksi: List<TransaksiPenjualan>,
    private val onItemKlik: (TransaksiPenjualan) -> Unit
) : RecyclerView.Adapter<DafTransaksiAdapter.ViewHolder>() {

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId        : TextView  = itemView.findViewById(R.id.tvIdTransaksi)
        val tvTanggal   : TextView  = itemView.findViewById(R.id.tvTanggalTransaksi)
        val tvMetode    : TextView  = itemView.findViewById(R.id.tvMetode)
        val tvTotal     : TextView  = itemView.findViewById(R.id.tvTotalTransaksi)
        val tvStatus    : TextView  = itemView.findViewById(R.id.tvStatusTransaksi)
        val card        : CardView  = itemView.findViewById(R.id.cardTransaksi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = listTransaksi[position]

        holder.tvId.text      = t.idTransaksi
        holder.tvTanggal.text = formatTanggal(t.tanggalTransaksi)
        holder.tvMetode.text  = t.metode ?: "-"
        holder.tvTotal.text   = "Rp ${rupiahFormat.format(t.totalBelanja)}"
        holder.tvStatus.text  = t.statusPembayaran

        // Warna status chip
        val ctx = holder.itemView.context
        val colorBg = if (t.statusPembayaran == "Lunas") {
            ctx.getColor(R.color.green_primary)       // #2E7D5B
        } else {
            ctx.getColor(R.color.orange_dp)            // #FF8C00 — tambahkan di colors.xml
        }
        holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(colorBg)

        holder.card.setOnClickListener { onItemKlik(t) }
    }

    override fun getItemCount() = listTransaksi.size

    fun perbarui(data: List<TransaksiPenjualan>) {
        listTransaksi = data
        notifyDataSetChanged()
    }

    private fun formatTanggal(raw: String): String {
        return try {
            val inFmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            outFmt.format(inFmt.parse(raw) ?: Date())
        } catch (e: Exception) { raw }
    }
}