package com.kel4.notaly.laporan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import com.kel4.notaly.model.DetailPenjualan
import com.kel4.notaly.model.TransaksiPenjualan
import java.text.NumberFormat
import java.util.Locale

class LaporanAdapter(private var listData: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER   = 0
        private const val TYPE_KEUANGAN = 1
        private const val TYPE_RIWAYAT  = 2
        private const val TYPE_MUTASI   = 3
    }

    fun updateData(newList: List<Any>) {
        listData = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (listData[position]) {
            is HeaderTanggal      -> TYPE_HEADER
            is TransaksiPenjualan -> TYPE_KEUANGAN
            is ItemRiwayat        -> TYPE_RIWAYAT
            is MutasiBarang       -> TYPE_MUTASI
            else                  -> TYPE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_laporan_tanggal, parent, false)
            )
            TYPE_KEUANGAN -> KeuanganViewHolder(
                inflater.inflate(R.layout.item_laporan_keuangan, parent, false)
            )
            TYPE_RIWAYAT -> RiwayatViewHolder(
                // Menggunakan XML yang kamu upload (item_laporan_barang.xml)
                inflater.inflate(R.layout.item_laporan_barang, parent, false)
            )
            TYPE_MUTASI -> MutasiViewHolder(
                inflater.inflate(R.layout.item_laporan_mutasi, parent, false)
            )
            else -> throw IllegalArgumentException("Tipe view tidak valid")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = listData[position]
        val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

        when {
            // 1. BINDING HEADER TANGGAL
            holder is HeaderViewHolder && item is HeaderTanggal -> {
                holder.tvTanggal.text = item.tanggalStr
                // Set total laba jika id tvTotalLaba tersedia di XML
                holder.tvTotalLaba?.text = "Rp ${rupiah.format(item.totalLabaHarian)}"
            }

            // 2. BINDING LAPORAN KEUANGAN
            holder is KeuanganViewHolder && item is TransaksiPenjualan -> {
                holder.tvJudul.text = item.idTransaksi
                holder.tvDetail.text = "Penjualan • ${item.metode}"
                holder.tvNominal.text = "+ Rp ${rupiah.format(item.totalBelanja)}"
                // Warna teks hijau untuk uang masuk
                holder.tvNominal.setTextColor(0xFF2E7D5B.toInt())
            }

            // 3. BINDING LAPORAN RIWAYAT PEMBUKUAN (Dari item_laporan_barang.xml)
            holder is RiwayatViewHolder && item is ItemRiwayat -> {
                holder.tvNamaBarang.text = "${item.namaBarang}\n(${item.qty} pcs)"

                holder.tvHargaJual.text = "Rp\n${rupiah.format(item.hargaJual)}"
                holder.tvHargaModal.text = "Rp\n${rupiah.format(item.hargaModal)}"

                if (item.labaBersih >= 0) {
                    holder.tvLabaBersih.text = "+ Rp\n${rupiah.format(item.labaBersih)}"
                    holder.tvLabaBersih.setTextColor(0xFF2E7D5B.toInt()) // Hijau
                } else {
                    holder.tvLabaBersih.text = "- Rp\n${rupiah.format(item.labaBersih * -1)}"
                    holder.tvLabaBersih.setTextColor(0xFFE53935.toInt()) // Merah jika rugi
                }
            }

            // 4. BINDING LAPORAN MUTASI BARANG
            holder is MutasiViewHolder && item is MutasiBarang -> {
                holder.tvNama.text = item.nama
                holder.tvWaktu.text = "Sisa stok di gudang: ${item.sisa} pcs"

                if (item.keluar > 0) {
                    holder.tvQty.text = "- ${item.keluar} pcs"
                    holder.tvQty.setTextColor(0xFFE53935.toInt()) // Merah
                    holder.ivStatus.setImageResource(R.drawable.ic_arrow_down)
                    holder.ivStatus.imageTintList = android.content.res.ColorStateList.valueOf(0xFFE53935.toInt())
                } else {
                    holder.tvQty.text = "+ ${item.masuk} pcs"
                    holder.tvQty.setTextColor(0xFF2E7D5B.toInt()) // Hijau
                    holder.ivStatus.setImageResource(R.drawable.ic_arrow_up)
                    holder.ivStatus.imageTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D5B.toInt())
                }
            }
        }
    }

    override fun getItemCount() = listData.size

    // =========================================================
    // KUMPULAN VIEWHOLDER (Mewakili masing-masing Layout XML)
    // =========================================================

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTanggal: TextView = v.findViewById(R.id.tvTanggal)
        val tvTotalLaba: TextView? = v.findViewById(R.id.tvTotalLaba)
    }

    class KeuanganViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvJudul: TextView   = v.findViewById(R.id.tvJudulTrx)
        val tvDetail: TextView  = v.findViewById(R.id.tvDetailTrx)
        val tvNominal: TextView = v.findViewById(R.id.tvNominalUang)
    }

    class RiwayatViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNamaBarang: TextView = v.findViewById(R.id.tvNamaBarang)
        val tvHargaJual: TextView  = v.findViewById(R.id.tvHargaJual)
        val tvHargaModal: TextView = v.findViewById(R.id.tvHargaModal)
        val tvLabaBersih: TextView = v.findViewById(R.id.tvLabaBersih)
    }

    class MutasiViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNama: TextView    = v.findViewById(R.id.tvNamaBarangMutasi)
        val tvWaktu: TextView   = v.findViewById(R.id.tvWaktuMutasi)
        val tvQty: TextView     = v.findViewById(R.id.tvQtyMutasi)
        val ivStatus: ImageView = v.findViewById(R.id.ivStatusStok)
    }
}