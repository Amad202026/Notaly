package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kel4.notaly.model.DetailPenjualan

@Dao
interface DetailPenjualanDao {
    @Insert
    suspend fun tambahDetailBelanja(detail: List<DetailPenjualan>)

    @Query("SELECT * FROM detail_penjualan WHERE ID_Transaksi = :idTransaksi")
    suspend fun ambilStrukTransaksi(idTransaksi: String): List<DetailPenjualan>
}