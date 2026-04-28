package com.kel4.notaly.transaksi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Barang
import com.kel4.notaly.model.DetailPenjualan
import java.text.NumberFormat
import java.util.Locale

class AdapterProdukDetail(
    private val listDetail: List<DetailPenjualan>,
    private val mapBarang: Map<String, Barang>
) : RecyclerView.Adapter<AdapterProdukDetail.ViewHolder>() {

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaBarang: TextView = view.findViewById(R.id.tvNamaBarang)
        val tvHargaQty: TextView   = view.findViewById(R.id.tvHargaQty)
        val tvSubtotal: TextView   = view.findViewById(R.id.tvSubtotalItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produk_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val detail = listDetail[position]

        // Ambil data Barang dari Map berdasarkan ID
        val barang = mapBarang[detail.idBarang]

        // 1. Tampilkan Nama Barang (Jika barang terhapus dari DB, tampilkan ID-nya saja)
        holder.tvNamaBarang.text = barang?.namaBarang ?: detail.idBarang

        // 2. Format Qty x Harga (Gunakan Harga Nego jika ada, jika tidak pakai Harga Satuan)
        val hargaTampil = detail.hargaNego ?: detail.hargaSatuan
        holder.tvHargaQty.text = "${detail.qty} x Rp ${rupiahFormat.format(hargaTampil)}"

        // 3. Tampilkan Subtotal
        holder.tvSubtotal.text = "Rp ${rupiahFormat.format(detail.subtotal)}"
    }

    override fun getItemCount(): Int = listDetail.size
}