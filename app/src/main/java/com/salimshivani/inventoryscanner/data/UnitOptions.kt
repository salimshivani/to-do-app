package com.salimshivani.inventoryscanner.data

// One sellable/purchasable unit for an item — Unit 1 (the item's base unit, always present)
// plus up to two optional alternate units (e.g. Unit 1 = Pcs, Unit 2 = Box of 10 Pcs).
// ratioToBase is "how many Unit 1 make one of this unit" (always 1.0 for Unit 1 itself).
data class UnitOption(
    val label: String,
    val ratioToBase: Double,
    val purchasePrice: Double,
    val sellingPrice: Double
)

fun Item.unitOptions(): List<UnitOption> {
    val options = mutableListOf(
        UnitOption(unit?.ifBlank { "unit" } ?: "unit", 1.0, purchasePrice, price)
    )
    if (!unit2Name.isNullOrBlank()) {
        options.add(UnitOption(unit2Name, unit2Ratio ?: 1.0, unit2PurchasePrice ?: 0.0, unit2Price ?: 0.0))
    }
    if (!unit3Name.isNullOrBlank()) {
        options.add(UnitOption(unit3Name, unit3Ratio ?: 1.0, unit3PurchasePrice ?: 0.0, unit3Price ?: 0.0))
    }
    return options
}
