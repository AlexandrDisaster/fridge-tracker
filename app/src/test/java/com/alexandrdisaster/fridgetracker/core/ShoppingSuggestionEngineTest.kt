package com.alexandrdisaster.fridgetracker.core

import org.junit.Test
import java.time.LocalDate
import org.junit.Assert.*

class ShoppingSuggestionEngineTest {

    // Helper method to create items
    private fun createGrocery(id: String, name: String, amount: Double, unit: String, expiryDate: LocalDate): GroceryItem {
        return GroceryItem(id, name, amount, unit, expiryDate)
    }

    private fun createShoppingItem(name: String): ShoppingItem {
        return ShoppingItem("s1", name, 1.0, "pcs")
    }

    @Test
    fun testLowQuantitySuggestion() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            createGrocery("g1", "Milk", 0.1, "bottle", today.plusDays(10)) // Low
        )
        val shoppingItems = emptyList<ShoppingItem>()
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        assertFalse(suggestions.isEmpty())
        assertEquals("Low remaining amount", suggestions.first().reason)
        assertEquals(0.1, suggestions.first().suggestedAmount, 0.001)
        assertEquals("Milk", suggestions.first().name)
    }

    @Test
    fun testNearExpirySuggestion() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            createGrocery("g2", "Bread", 1.0, "pack", today.plusDays(3)) // Expires within window (3 days)
        )
        val shoppingItems = emptyList<ShoppingItem>()
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        assertFalse(suggestions.isEmpty())
        assertEquals("Expires soon (2026-11-29)", suggestions.first().reason)
        assertEquals("Bread", suggestions.first().name)
    }

    @Test
    fun testCombinedReasonSuggestion() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            createGrocery("g3", "Cheese", 0.05, "jar", today.plusDays(2)) // Low AND near expiry
        )
        val shoppingItems = emptyList<ShoppingItem>()
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        assertFalse(suggestions.isEmpty())
        // Expected combined reason: "Low remaining amount and Expires soon (2026-11-28)"
        assertTrue(suggestions.first().reason.contains("Low remaining amount") && suggestions.first().reason.contains("Expires soon"))
        assertEquals("Cheese", suggestions.first().name)
    }

    @Test
    fun testSuppressExistingBasketEntry() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            createGrocery("g4", "Milk", 0.1, "bottle", today.plusDays(10)) // Low, but already in basket
        )
        val shoppingItems = listOf(
            createShoppingItem("Milk") // Case/space-insensitive match
        )
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        assertTrue(suggestions.isEmpty()) // Should be suppressed
    }

    @Test
    fun testIgnoreExpiredItems() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            createGrocery("g5", "Expired Yogurt", 0.1, "jar", today.minusDays(1)) // Expired yesterday
        )
        val shoppingItems = emptyList<ShoppingItem>()
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        assertTrue(suggestions.isEmpty()) // Should be ignored
    }
    
    @Test
    fun testOrderingExpirySoonBeforeLowQuantity() {
        val today = LocalDate.of(2026, 11, 26)
        val groceries = listOf(
            // Low quantity, not near expiry
            createGrocery("g6", "Water", 0.1, "bottle", today.plusDays(20)), 
            // Near expiry, not low quantity
            createGrocery("g7", "Yogurt", 1.0, "jar", today.plusDays(2)) 
        )
        val shoppingItems = emptyList<ShoppingItem>()
        
        val suggestions = ShoppingSuggestionEngine.generate(groceries, shoppingItems, today)
        
        // Expect Yogurt (expiry soon) first, then Water (low quantity)
        assertEquals("Yogurt", suggestions.first().name)
        assertEquals("Water", suggestions.last().name)
    }
}