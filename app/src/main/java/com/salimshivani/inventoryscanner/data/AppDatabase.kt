package com.salimshivani.inventoryscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction

@Database(entities = [Item::class, Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun transactionDao(): TransactionDao

    suspend fun replaceAllData(items: List<Item>, transactions: List<Transaction>) {
        withTransaction {
            transactionDao().deleteAll()
            itemDao().deleteAll()
            itemDao().insertAll(items)
            transactionDao().insertAll(transactions)
        }
    }

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory.db"
                ).build().also { instance = it }
            }
    }
}
