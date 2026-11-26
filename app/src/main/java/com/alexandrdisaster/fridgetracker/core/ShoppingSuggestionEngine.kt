package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate

data class ShoppingSuggestion(
    val groceryId: String,
    val name: String,
    val suggestedAmount: Double,
    val unit: String,
    val reason: String,
)

object ShoppingSuggestionEngine {
    private data class RankedSuggestion(
        val priority: Int,
        val inputOrder: Int,
        val suggestion: ShoppingSuggestion,
    )

    fun generate(
        groceries: List<GroceryItem>,
        shoppingItems: List<ShoppingItem>,
        today: LocalDate,
        expiryWindowDays: Long = 3,
    ): List<ShoppingSuggestion> {
        require(expiryWindowDays >= 0) { "Expiry window cannot be negative." }

        val existingNames = shoppingItems.mapTo(mutableSetOf()) { normalizeName(it.name) }
        val lastSuggestedDay = today.plusDays(expiryWindowDays)

        return groceries.mapIndexedNotNull { index, grocery ->
            val normalizedName = normalizeName(grocery.name)
            if (normalizedName.isEmpty() || normalizedName in existingNames) return@mapIndexedNotNull null
            if (grocery.expiryDate.isBefore(today)) return@mapIndexedNotNull null

            val expiresSoon = !grocery.expiryDate.isAfter(lastSuggestedDay)
            val lowAmount = isLowAmount(grocery.amount, grocery.unit)
            if (!expiresSoon && !lowAmount) return@mapIndexedNotNull null

            val reasons = buildList {
                if (lowAmount) add("Low remaining amount")
                if (expiresSoon) add("Expires soon (${grocery.expiryDate})")
            }
            val suggestion = ShoppingSuggestion(
                groceryId = grocery.id,
                name = grocery.name,
                suggestedAmount = grocery.amount,
                unit = grocery.unit,
                reason = reasons.joinToString(" and "),
            )
            RankedSuggestion(
                priority = if (expiresSoon) 0 else 1,
                inputOrder = index,
                suggestion = suggestion,
            )
        }.sortedWith(compareBy<RankedSuggestion> { it.priority }.thenBy { it.inputOrder })
            .map(RankedSuggestion::suggestion)
    }

    private fun isLowAmount(amount: Double, unit: String): Boolean {
        if (!amount.isFinite()) return false
        return when (unit.trim().lowercase()) {
            "pcs", "pack", "bottle", "jar", "can", "bunch" -> amount <= 1.0
            "kg", "l" -> amount <= 0.25
            "g", "ml" -> amount <= 250.0
            else -> amount <= 1.0
        }
    }

    private fun normalizeName(name: String): String =
        name.trim().lowercase().replace(Regex("\\s+"), "")
}
