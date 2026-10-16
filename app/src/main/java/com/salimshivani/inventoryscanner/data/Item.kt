package com.salimshivani.inventoryscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val BARCODE_SOURCE_SCANNED = "scanned"
const val BARCODE_SOURCE_GENERATED = "generated"

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    @ColumnInfo(name = "hsn_code") val hsnCode: String? = null,
    val unit: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "barcode_source") val barcodeSource: String = BARCODE_SOURCE_SCANNED,
    @ColumnInfo(name = "label_printed_at") val labelPrintedAt: String? = null,
    val price: Double = 0.0,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double = 0.0,
    val mrp: Double = 0.0,
    @ColumnInfo(name = "gst_percent") val gstPercent: Double = 0.0
)
