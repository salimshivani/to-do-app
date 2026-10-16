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
    @ColumnInfo(name = "gst_percent") val gstPercent: Double = 0.0,
    // Alternate units (e.g. Unit 1 = Pcs, Unit 2 = Box of 10 Pcs, Unit 3 = Case of 5 Box).
    // `unit` above is Unit 1's name; its ratio to itself is always 1. Unit 2/3 are optional,
    // each with its own independently-editable purchase/selling price (not auto-derived from
    // the ratio, since a bulk unit is often priced at a discount rather than a strict multiple).
    @ColumnInfo(name = "unit2_name") val unit2Name: String? = null,
    @ColumnInfo(name = "unit2_ratio") val unit2Ratio: Double? = null,
    @ColumnInfo(name = "unit2_price") val unit2Price: Double? = null,
    @ColumnInfo(name = "unit2_purchase_price") val unit2PurchasePrice: Double? = null,
    @ColumnInfo(name = "unit3_name") val unit3Name: String? = null,
    @ColumnInfo(name = "unit3_ratio") val unit3Ratio: Double? = null,
    @ColumnInfo(name = "unit3_price") val unit3Price: Double? = null,
    @ColumnInfo(name = "unit3_purchase_price") val unit3PurchasePrice: Double? = null
)
