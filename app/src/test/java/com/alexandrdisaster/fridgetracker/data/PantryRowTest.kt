package com.alexandrdisaster.fridgetracker.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PantryRowTest {
    private val date = LocalDate.of(2024, 2, 29)
    private val milk = GroceryRow("milk", "Milk", 1.5, "L", date.toEpochDay())

    @Test fun exactAndExcessConsumptionFinishTheItem() {
        assertNull(milk.adjusted(-1.5))
        assertNull(milk.adjusted(-2.0))
    }

    @Test fun partialConsumptionPreservesExpiryAndIdentity() {
        assertEquals(milk.item().copy(amount = 1.0), milk.adjusted(-0.5)?.item())
        assertEquals(date, milk.item().expiryDate)
    }

    @Test fun checkedShoppingStateSurvivesConversion() {
        assertTrue(ShoppingRow("milk", "Milk", 1.5, "L", true).item().isChecked)
    }
}
