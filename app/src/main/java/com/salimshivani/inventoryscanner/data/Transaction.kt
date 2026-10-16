package com.salimshivani.inventoryscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val DIRECTION_INWARD = "inward"
const val DIRECTION_OUTWARD = "outward"

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("item_id"), Index("timestamp"), Index("account_id"), Index("bill_id")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "item_id") val itemId: Long,
    val direction: String,
    val quantity: Double,
    val timestamp: String,
    val note: String? = null,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    @ColumnInfo(name = "bill_id") val billId: Long? = null,
    val rate: Double = 0.0,
    @ColumnInfo(name = "gst_percent") val gstPercent: Double = 0.0,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0,
    @ColumnInfo(name = "line_total") val lineTotal: Double = 0.0
)
