package com.kel4.notaly.database

import com.kel4.notaly.dao.SupplierDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kel4.notaly.dao.DetailPenjualanDao
import com.kel4.notaly.dao.TransaksiPenjualanDao
import com.kel4.notaly.dao.BarangDao
import com.kel4.notaly.dao.PelangganDao
import com.kel4.notaly.dao.BarangMasukDao
import com.kel4.notaly.dao.PengirimanDao
import com.kel4.notaly.model.Supplier
import com.kel4.notaly.model.Barang
import com.kel4.notaly.model.Pelanggan
import com.kel4.notaly.model.TransaksiPenjualan
import com.kel4.notaly.model.DetailPenjualan
import com.kel4.notaly.model.BarangMasuk
import com.kel4.notaly.model.Pengiriman

@Database(entities = [
    Barang::class,
    Pelanggan::class,
    Supplier::class,
    TransaksiPenjualan::class,
    DetailPenjualan::class,
    BarangMasuk::class,
    Pengiriman::class
], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun barangDao(): BarangDao
    abstract fun pelangganDao(): PelangganDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transaksiDao(): TransaksiPenjualanDao
    abstract fun detailPenjualanDao(): DetailPenjualanDao
    abstract fun barangMasukDao(): BarangMasukDao
    abstract fun pengirimanDao(): PengirimanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notaly_database.db"
                )
                    .createFromAsset("databases/database.db")
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}