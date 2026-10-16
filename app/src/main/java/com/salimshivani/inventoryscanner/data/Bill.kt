package com.salimshivani.inventoryscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val BILL_STATUS_UNPAID = "unpaid"
const val BILL_STATUS_PAID = "paid"

// direction = "outward" -> receivable (customer owes the shop, settled by a payment *received*)
// direction = "inward"  -> payable (shop owes the supplier, settled by a payment *made*)
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Payment::class, parentColumns = ["id"], childColumns = ["payment_id"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("account_id"), Index("status"), Index("payment_id")]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    val direction: String,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    val status: String = BILL_STATUS_UNPAID,
    @ColumnInfo(name = "bill_date") val billDate: String,
    @ColumnInfo(name = "payment_id") val paymentId: Long? = null,
    val subtotal: Double = 0.0,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double = 0.0
)
