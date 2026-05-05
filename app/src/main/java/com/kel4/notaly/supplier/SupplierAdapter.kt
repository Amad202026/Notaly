package com.kel4.notaly.supplier

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Supplier

class SupplierAdapter(
    private var listSupplier: List<Supplier>,
    private val onDetailKlik: (Supplier) -> Unit,
    private val onHapusKlik: (Supplier) -> Unit
) : RecyclerView.Adapter<SupplierAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInisialSupplier: TextView = view.findViewById(R.id.tvInisialSupplier) // Tambahan baru
        val tvKategoriPill: TextView  = view.findViewById(R.id.tvKategoriPill)
        val tvNamaSupplier: TextView  = view.findViewById(R.id.tvNamaSupplier)
        val tvNoTelp: TextView        = view.findViewById(R.id.tvNoTelp)
        val btnDetail: TextView       = view.findViewById(R.id.btnDetail)
        val btnDelete: ImageView      = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_supplier, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supplier = listSupplier[position]

        // Set Nama Lengkap
        holder.tvNamaSupplier.text = supplier.namaSupplier

        // Set Inisial Logo (Ambil 1 huruf pertama dan jadikan huruf besar)
        val namaLengkap = supplier.namaSupplier
        if (namaLengkap.isNotBlank()) {
            holder.tvInisialSupplier.text = namaLengkap.take(1).uppercase()
        } else {
            holder.tvInisialSupplier.text = "-"
        }

        // Set Kategori
        val kategori = supplier.kategoriSuplai
        if (kategori.isNullOrBlank()) {
            holder.tvKategoriPill.visibility = View.GONE
        } else {
            holder.tvKategoriPill.visibility = View.VISIBLE
            holder.tvKategoriPill.text = kategori.uppercase()
        }

        // Set Nomor Telepon
        holder.tvNoTelp.text = supplier.noWa
            ?.takeIf { it.isNotBlank() }
            ?: "Tidak ada nomor"

        // Set Tombol Detail
        holder.btnDetail.text = "Detail"
        holder.btnDetail.setOnClickListener { onDetailKlik(supplier) }

        // Set Tombol Hapus
        holder.btnDelete.setOnClickListener { onHapusKlik(supplier) }
    }

    override fun getItemCount(): Int = listSupplier.size

    fun updateData(newList: List<Supplier>) {
        listSupplier = newList
        notifyDataSetChanged()
    }
}