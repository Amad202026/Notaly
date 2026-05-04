package com.kel4.notaly.laporan

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kel4.notaly.R
import java.text.NumberFormat
import java.util.Locale

// ── Data classes ──────────────────────────────────────────────────────────────

data class HeaderTanggal(
    val tanggalStr      : String,
    val totalLabaHarian : Int,
    val infoTambahan    : String = ""   // opsional, misal "3 transaksi"
)

// ItemKeuangan: dipakai di Laporan Keuangan (pemasukan & pengeluaran)
data class ItemKeuangan(
    val tanggal  : String,
    val judul    : String,
    val detail   : String,
    val nominal  : Long,
    val jenisPos : Boolean  // true = uang masuk, false = uang keluar
)

// ── Adapter ───────────────────────────────────────────────────────────────────

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

    override fun getItemViewType(position: Int) = when (listData[position]) {
        is HeaderTanggal -> TYPE_HEADER
        is ItemKeuangan  -> TYPE_KEUANGAN
        is ItemRiwayat   -> TYPE_RIWAYAT
        is MutasiBarang  -> TYPE_MUTASI
        else             -> TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER   -> HeaderViewHolder(inf.inflate(R.layout.item_laporan_tanggal,  parent, false))
            TYPE_KEUANGAN -> KeuanganViewHolder(inf.inflate(R.layout.item_laporan_keuangan, parent, false))
            TYPE_RIWAYAT  -> RiwayatViewHolder(inf.inflate(R.layout.item_laporan_barang,   parent, false))
            TYPE_MUTASI   -> MutasiViewHolder(inf.inflate(R.layout.item_laporan_mutasi,   parent, false))
            else          -> throw IllegalArgumentException("Tipe tidak dikenal")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item   = listData[position]
        val rupiah = NumberFormat.getNumberInstance(Locale("id", "ID"))

        when {
            // ── HEADER TANGGAL ────────────────────────────────────────────
            holder is HeaderViewHolder && item is HeaderTanggal -> {
                holder.tvTanggal.text = item.tanggalStr

                // Tampilkan label tambahan di samping tanggal jika ada
                val labelExtra = when {
                    item.infoTambahan.isNotEmpty() -> item.infoTambahan
                    else -> ""
                }
                // tvTotalLaba selalu tampil sebagai laba/saldo hari itu
                holder.tvTotalLaba?.text = if (item.totalLabaHarian >= 0)
                    "Rp ${rupiah.format(item.totalLabaHarian)}"
                else
                    "- Rp ${rupiah.format(-item.totalLabaHarian.toLong())}"

                holder.tvTotalLaba?.setTextColor(
                    if (item.totalLabaHarian >= 0) 0xFF0D5C3A.toInt() else 0xFFE53935.toInt()
                )
            }

            // ── LAPORAN KEUANGAN (item_laporan_keuangan.xml) ───────────────
            holder is KeuanganViewHolder && item is ItemKeuangan -> {
                holder.tvJudul.text  = item.judul
                holder.tvDetail.text = item.detail

                if (item.jenisPos) {
                    holder.tvNominal.text = "+ Rp ${rupiah.format(item.nominal)}"
                    holder.tvNominal.setTextColor(0xFF2E7D5B.toInt())
                    holder.ivIcon?.setImageResource(R.drawable.ic_money)
                    holder.ivIcon?.imageTintList = ColorStateList.valueOf(0xFF0D5C3A.toInt())
                } else {
                    holder.tvNominal.text = "- Rp ${rupiah.format(item.nominal)}"
                    holder.tvNominal.setTextColor(0xFFE53935.toInt())
                    holder.ivIcon?.setImageResource(R.drawable.ic_arrow_down)
                    holder.ivIcon?.imageTintList = ColorStateList.valueOf(0xFFE53935.toInt())
                }
            }

            // ── LAPORAN RIWAYAT (item_laporan_barang.xml) ─────────────────
            holder is RiwayatViewHolder && item is ItemRiwayat -> {
                val statusTag = if (item.statusBayar == "DP") " [DP]" else ""
                holder.tvNamaBarang.text = "${item.namaBarang}${statusTag}\n${item.qty} pcs · ${item.idTransaksi}"
                holder.tvHargaJual.text  = "Rp\n${rupiah.format(item.hargaJual)}"
                holder.tvHargaModal.text = "Rp\n${rupiah.format(item.hargaModal)}"

                if (item.labaBersih >= 0) {
                    holder.tvLabaBersih.text = "+ Rp\n${rupiah.format(item.labaBersih)}"
                    holder.tvLabaBersih.setTextColor(0xFF2E7D5B.toInt())
                } else {
                    holder.tvLabaBersih.text = "- Rp\n${rupiah.format(-item.labaBersih)}"
                    holder.tvLabaBersih.setTextColor(0xFFE53935.toInt())
                }
            }

            // ── LAPORAN MUTASI BARANG (item_laporan_mutasi.xml) ───────────
            holder is MutasiViewHolder && item is MutasiBarang -> {
                holder.tvNama.text   = item.nama
                holder.tvWaktu.text  = item.keterangan

                if (item.masuk > 0) {
                    holder.tvQty.text = "+ ${item.masuk} pcs"
                    holder.tvQty.setTextColor(0xFF2E7D5B.toInt())
                    holder.ivStatus.setImageResource(R.drawable.ic_arrow_up)
                    holder.ivStatus.imageTintList = ColorStateList.valueOf(0xFF2E7D5B.toInt())
                } else {
                    holder.tvQty.text = "- ${item.keluar} pcs"
                    holder.tvQty.setTextColor(0xFFE53935.toInt())
                    holder.ivStatus.setImageResource(R.drawable.ic_arrow_down)
                    holder.ivStatus.imageTintList = ColorStateList.valueOf(0xFFE53935.toInt())
                }
            }
        }
    }

    override fun getItemCount() = listData.size

    // ── ViewHolders ───────────────────────────────────────────────────────────

    class HeaderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvTanggal  : TextView  = v.findViewById(R.id.tvTanggal)
        val tvTotalLaba: TextView? = v.findViewById(R.id.tvTotalLaba)
    }

    class KeuanganViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvJudul  : TextView  = v.findViewById(R.id.tvJudulTrx)
        val tvDetail : TextView  = v.findViewById(R.id.tvDetailTrx)
        val tvNominal: TextView  = v.findViewById(R.id.tvNominalUang)
        val ivIcon   : ImageView? = v.findViewById(R.id.ivIcon)
    }

    class RiwayatViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNamaBarang: TextView = v.findViewById(R.id.tvNamaBarang)
        val tvHargaJual : TextView = v.findViewById(R.id.tvHargaJual)
        val tvHargaModal: TextView = v.findViewById(R.id.tvHargaModal)
        val tvLabaBersih: TextView = v.findViewById(R.id.tvLabaBersih)
    }

    class MutasiViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNama  : TextView  = v.findViewById(R.id.tvNamaBarangMutasi)
        val tvWaktu : TextView  = v.findViewById(R.id.tvWaktuMutasi)
        val tvQty   : TextView  = v.findViewById(R.id.tvQtyMutasi)
        val ivStatus: ImageView = v.findViewById(R.id.ivStatusStok)
    }
}