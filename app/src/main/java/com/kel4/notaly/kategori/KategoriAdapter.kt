package com.kel4.notaly.kategori // Sesuaikan foldermu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R

class KategoriAdapter(
    private var listKategori: List<String>,
    private val onHapusKlik: (String) -> Unit
) : RecyclerView.Adapter<KategoriAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaKategori: TextView = view.findViewById(R.id.tvNamaKategori)
        val btnHapus: ImageView = view.findViewById(R.id.btnHapusKategori)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_kategori, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val namaKategori = listKategori[position]
        holder.tvNamaKategori.text = namaKategori

        holder.btnHapus.setOnClickListener {
            onHapusKlik(namaKategori)
        }
    }

    override fun getItemCount(): Int = listKategori.size

    fun updateData(newList: List<String>) {
        listKategori = newList
        notifyDataSetChanged()
    }
}