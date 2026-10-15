package com.salimshivani.inventoryscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// direction = "outward" -> payment *received* from a customer, settling receivable bills
// direction = "inward"  -> payment *made* to a supplier, settling payable bills
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("account_id")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    val direction: String,
    val amount: Double,
    @ColumnInfo(name = "payment_date") val paymentDate: String,
    val note: String? = null
)
