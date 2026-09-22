package com.salimshivani.inventoryscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                mobile_number TEXT,
                name TEXT NOT NULL,
                address TEXT,
                created_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_mobile_number ON accounts(mobile_number)")
        db.execSQL("ALTER TABLE transactions ADD COLUMN account_id INTEGER REFERENCES accounts(id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id ON transactions(account_id)")
    }
}

@Database(entities = [Item::class, Transaction::class, Account::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao

    suspend fun replaceAllData(items: List<Item>, transactions: List<Transaction>, accounts: List<Account>) {
        withTransaction {
            transactionDao().deleteAll()
            itemDao().deleteAll()
            accountDao().deleteAll()
            accountDao().insertAll(accounts)
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
