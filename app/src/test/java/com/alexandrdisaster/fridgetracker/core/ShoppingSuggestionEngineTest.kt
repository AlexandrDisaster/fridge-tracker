package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingSuggestionEngineTest {
    private val today = LocalDate.of(2026, 11, 26)

    private fun grocery(
        id: String,
        name: String,
        amount: Double,
        unit: String,
        expiry: LocalDate,
    ) = GroceryItem(id, name, amount, unit, expiry)

    @Test
    fun lowQuantitySuggestsIndependentlyOfExpiryWindow() {
        val item = grocery("g1", "Milk", 0.1, "bottle", today.plusDays(10))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(item), emptyList(), today)

        assertEquals(1, suggestions.size)
        assertEquals("Low remaining amount", suggestions.single().reason)
        assertEquals(item.amount, suggestions.single().suggestedAmount, 0.0)
        assertEquals(item.unit, suggestions.single().unit)
    }

    @Test
    fun expiryAtWindowBoundarySuggestsEvenWhenQuantityIsNotLow() {
        val item = grocery("g2", "Bread", 2.0, "pack", today.plusDays(3))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(item), emptyList(), today)

        assertEquals("Expires soon (2026-11-29)", suggestions.single().reason)
    }

    @Test
    fun combinesLowQuantityAndNearExpiryReasons() {
        val item = grocery("g3", "Cheese", 0.05, "jar", today.plusDays(2))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(item), emptyList(), today)

        assertTrue(suggestions.single().reason.contains("Low remaining amount"))
        assertTrue(suggestions.single().reason.contains("Expires soon"))
    }

    @Test
    fun basketNameMatchIgnoresCaseAndWhitespace() {
        val item = grocery("g4", "Milk Whole", 0.1, "bottle", today.plusDays(10))
        val shopping = listOf(ShoppingItem("s1", "  milk\twhole ", 1.0, "pcs"))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(item), shopping, today)

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun ignoresItemsThatExpiredBeforeToday() {
        val item = grocery("g5", "Yogurt", 0.1, "jar", today.minusDays(1))

        assertTrue(ShoppingSuggestionEngine.generate(listOf(item), emptyList(), today).isEmpty())
    }

    @Test
    fun expiringItemsSortAheadOfLowQuantityOnlyItems() {
        val lowOnly = grocery("g6", "Water", 0.1, "bottle", today.plusDays(20))
        val expiring = grocery("g7", "Yogurt", 2.0, "jar", today.plusDays(2))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(lowOnly, expiring), emptyList(), today)

        assertEquals(listOf("Yogurt", "Water"), suggestions.map(ShoppingSuggestion::name))
        assertFalse(suggestions.first().reason.contains("Low remaining amount"))
    }

    @Test
    fun unknownUnitsUseAConservativeSingleUnitThreshold() {
        val low = grocery("g8", "Special ingredient", 0.5, "scoop", today.plusDays(20))
        val plentiful = grocery("g9", "Another ingredient", 2.0, "scoop", today.plusDays(20))

        val suggestions = ShoppingSuggestionEngine.generate(listOf(low, plentiful), emptyList(), today)

        assertEquals(listOf("Special ingredient"), suggestions.map(ShoppingSuggestion::name))
    }
}
