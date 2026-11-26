package com.alexandrdisaster.fridgetracker.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductCatalogTest {
    @Test
    fun suggestionsUseKnownProductAndReferenceDate() {
        val from = LocalDate.of(2026, 12, 30)
        assertEquals(3, ProductCatalog.estimatedFridgeDays(" Yogurt "))
        assertEquals(LocalDate.of(2027, 1, 4), ProductCatalog.suggestedExpiryDate("Tomatoes", from))
        assertNull(ProductCatalog.estimatedFridgeDays("tomato sauce"))
        assertNull(ProductCatalog.suggestedExpiryDate("unknown", from))
    }
}
