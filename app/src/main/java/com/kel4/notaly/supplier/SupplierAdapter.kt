package com.kel4.notaly.supplier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Supplier

class SupplierAdapter(
    private var listSupplier: List<Supplier>,
    private val onHapusKlik: (Supplier) -> Unit // Callback untuk fungsi hapus
) : RecyclerView.Adapter<SupplierAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKategoriPill: TextView = view.findViewById(R.id.tvKategoriPill)
        val tvNamaSupplier: TextView = view.findViewById(R.id.tvNamaSupplier)
        val tvNoTelp: TextView = view.findViewById(R.id.tvNoTelp)
        val btnDetail: TextView = view.findViewById(R.id.btnDetail) // Diaktifkan untuk hapus
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_supplier, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supplier = listSupplier[position]

        // Set Nama
        holder.tvNamaSupplier.text = supplier.namaSupplier

        // Set Kategori (Uppercase & Handling Empty)
        if (supplier.kategoriSuplai.isNullOrEmpty()) {
            holder.tvKategoriPill.visibility = View.GONE
        } else {
            holder.tvKategoriPill.visibility = View.VISIBLE
            holder.tvKategoriPill.text = supplier.kategoriSuplai.uppercase()
        }

        // Set No Telp
        holder.tvNoTelp.text = if (supplier.noWa.isNullOrEmpty()) "Tidak ada nomor" else supplier.noWa

        // Logika Hapus saat tombol diklik
        holder.btnDetail.text = "Hapus" // Kita ubah teksnya jadi Hapus untuk belajar
        holder.btnDetail.setOnClickListener {
            onHapusKlik(supplier)
        }
    }

    override fun getItemCount(): Int = listSupplier.size

    fun updateData(newList: List<Supplier>) {
        listSupplier = newList
        notifyDataSetChanged()
    }
}