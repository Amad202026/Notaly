package com.kel4.notaly.restock

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.Barang

class RestockAdapter(
    private var listData: List<Any>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM   = 1
    }

    fun updateData(newList: List<Any>) {
        listData = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (listData[position] is String) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restock_kategori, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_restock, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(listData[position] as String)
            is ItemViewHolder   -> holder.bind(listData[position] as Barang)
        }
    }

    override fun getItemCount(): Int = listData.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeaderKategori)

        fun bind(kategori: String) {
            tvHeader.text = kategori.uppercase()
        }
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNamaBarang: TextView = view.findViewById(R.id.tvNamaBarang)
        private val tvQty:        TextView = view.findViewById(R.id.tvStokNormal)

        fun bind(barang: Barang) {
            tvNamaBarang.text = barang.namaBarang
            tvQty.text        = "${barang.stok} Pcs"
        }
    }
}