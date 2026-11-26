package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate

data class ShoppingSuggestion(
    val groceryId: String, 
    val name: String, 
    val suggestedAmount: Double, 
    val unit: String, 
    val reason: String
)

object ShoppingSuggestionEngine {
    
    // Sensible low thresholds based on baseline selectable units
    private fun isLowAmount(unit: String, amount: Double): Boolean {
        return when (unit.lowercase()) {
            "pcs", "pack", "bottle", "jar", "can", "bunch" -> amount <= 1.0
            "kg", "l" -> amount <= 0.25
            "g", "ml" -> amount <= 250.0
            else -> false // Default to not low if unit is unknown
        }
    }

    fun generate(
        groceries: List<GroceryItem>, 
        shoppingItems: List<ShoppingItem>, 
        today: LocalDate, 
        expiryWindowDays: Long = 3
    ): List<ShoppingSuggestion> {
        
        val expiryLimit = today.plusDays(expiryWindowDays)
        
        // Set of names from shoppingItems for suppression check (case/space-insensitive)
        val shoppingNames = shoppingItems.map { it.name.lowercase().replace(" ", "") }.toSet()

        val suggestions = mutableListOf<Pair<Long, ShoppingSuggestion>>() // Pair: 0=quantity, 1=expiry, followed by suggestion

        for (grocery in groceries) {
            // 1. Check if item is unexpired and within the window
            val isUnexpired = grocery.expiryDate.isAfter(today.minusDays(1)) // Assume expiry date is inclusive of the day before it expires
            val isExpiringSoon = grocery.expiryDate.isBefore(expiryLimit.plusDays(1))

            if (!isUnexpired || !isExpiringSoon) {
                continue // Ignore expired or outside the window
            }

            // 2. Check for low amount
            val lowAmount = isLowAmount(grocery.unit, grocery.amount)
            
            // 3. Suppress if in shopping list
            val nameMatch = grocery.name.lowercase().replace(" ", "")
            if (shoppingNames.contains(nameMatch)) {
                continue
            }

            // 4. Determine reasons
            val reasons = mutableListOf<String>()
            if (lowAmount) {
                reasons.add("Low remaining amount")
            }
            if (isExpiringSoon) {
                reasons.add("Expires soon (${grocery.expiryDate})")
            }
            
            // 5. Create suggestion
            val finalReason = reasons.joinToString(" and ")
            
            val suggestion = ShoppingSuggestion(
                groceryId = grocery.id,
                name = grocery.name,
                suggestedAmount = grocery.amount,
                unit = grocery.unit,
                reason = finalReason
            )
            
            // 6. Order: 1 (expiry-soon) comes before 0 (quantity-only).
            // Assign score: 1 if expiring soon, 0 if quantity only (and thus, not expiring soon, but still low).
            // Since we already checked isExpiringSoon, we use that as the primary sort key.
            val sortKey = if (isExpiringSoon) 1L else 0L
            suggestions.add(Pair(sortKey, suggestion))
        }
        
        // Sort: expiry-soon (1) before quantity-only (0)
        suggestions.sortBy { it.first }

        // Return only the suggestions
        return suggestions.map { it.second }
    }
}