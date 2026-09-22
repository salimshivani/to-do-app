package com.salimshivani.inventoryscanner.data

data class DatewiseRow(
    val date: String,
    val direction: String,
    val total_quantity: Double,
    val transaction_count: Int
)

data class ItemwiseRow(
    val item_id: Long,
    val barcode: String,
    val item_name: String,
    val hsn_code: String?,
    val inward_total: Double,
    val outward_total: Double,
    val net_stock: Double
)

data class HsnwiseRow(
    val hsn_code: String,
    val inward_total: Double,
    val outward_total: Double,
    val net_stock: Double,
    val item_count: Int
)

data class AccountwiseRow(
    val account_id: Long?,
    val account_name: String,
    val mobile_number: String?,
    val inward_total: Double,
    val outward_total: Double,
    val net_stock: Double,
    val transaction_count: Int
)

data class TransactionWithItem(
    val id: Long,
    val item_id: Long,
    val direction: String,
    val quantity: Double,
    val timestamp: String,
    val note: String?,
    val account_id: Long?,
    val barcode: String,
    val item_name: String,
    val hsn_code: String?,
    val account_name: String?,
    val account_mobile: String?
)

data class TransactionWithAccount(
    val id: Long,
    val item_id: Long,
    val direction: String,
    val quantity: Double,
    val timestamp: String,
    val note: String?,
    val account_name: String?
)
