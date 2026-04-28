package com.kel4.notaly.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.kel4.notaly.model.BarangMasuk
import com.kel4.notaly.model.DetailRestokLengkap

@Dao
interface BarangMasukDao {

    @Insert
    suspend fun catatRestok(barangMasuk: BarangMasuk)

    @Query("SELECT * FROM barang_masuk ORDER BY Tanggal_Masuk DESC")
    suspend fun riwayatBarangMasuk(): List<BarangMasuk>

    @Query("SELECT * FROM barang_masuk WHERE ID_Barang = :idBarang")
    suspend fun riwayatRestokSatuBarang(idBarang: String): List<BarangMasuk>

    @Query("DELETE FROM barang_masuk")  // ← was: DELETE FROM barang
    suspend fun hapusSemua()

    @Query("""
        SELECT
            bm.ID_Restok                   AS idRestok,
            b.Nama_Barang                  AS namaBarang,
            b.Kategori                     AS kategori,
            s.Nama_Supplier                AS namaSupplier,
            COALESCE(s.Asal_Daerah, '-')   AS asalSupplier,
            bm.Qty_Masuk                   AS totalQty,
            bm.Tanggal_Masuk               AS tanggalMasuk,
            bm.Harga_Beli                  AS hargaBeli
        FROM barang_masuk bm
        INNER JOIN barang b   ON bm.ID_Barang   = b.ID_Barang
        INNER JOIN supplier s ON bm.ID_Supplier = s.ID_Supplier
        ORDER BY b.Kategori ASC, bm.Tanggal_Masuk DESC
    """)
    suspend fun ambilRiwayatRestokLengkap(): List<DetailRestokLengkap>
}
