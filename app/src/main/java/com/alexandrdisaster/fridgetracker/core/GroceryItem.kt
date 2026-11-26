package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate

data class GroceryItem(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val expiryDate: LocalDate,
) {
    init {
        require(name.isNotBlank()) { "Grocery name must not be blank" }
        require(amount >= 0.0) { "Grocery amount cannot be negative" }
        require(unit.isNotBlank()) { "Grocery unit must not be blank" }
    }

    val isFinished: Boolean get() = amount <= 0.0
}

data class ShoppingItem(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val isChecked: Boolean = false,
)

data class ShoppingEntry(
    val name: String,
    val amount: Double,
    val unit: String,
)
