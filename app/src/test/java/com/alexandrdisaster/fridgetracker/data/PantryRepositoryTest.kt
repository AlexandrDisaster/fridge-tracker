package com.alexandrdisaster.fridgetracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.alexandrdisaster.fridgetracker.core.GroceryItem
import com.alexandrdisaster.fridgetracker.core.ShoppingEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PantryRepositoryTest {
    private lateinit var database: PantryDatabase
    private lateinit var repository: RoomPantryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PantryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPantryRepository(database, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun consumingLastAmountAtomicallyMovesGroceryToShoppingList() = runBlocking {
        val milk = GroceryItem("milk-1", "Milk", 1.0, "L", LocalDate.of(2026, 12, 1))
        repository.saveGrocery(milk)

        repository.adjustAmount(milk.id, -1.0)

        assertTrue(repository.observeGroceries().first().isEmpty())
        val shopping = repository.observeShoppingItems().first()
        assertEquals(1, shopping.size)
        assertEquals("Milk", shopping.single().name)
        assertEquals(1.0, shopping.single().amount, 0.0)
        assertEquals("L", shopping.single().unit)
    }

    @Test
    fun matchingNameAndExpiryDateStackAmountsIntoOneInventoryRow() = runBlocking {
        val expiry = LocalDate.of(2026, 11, 29)
        repository.saveGrocery(GroceryItem("banana-first", "Bananas", 5.0, "pcs", expiry))
        repository.saveGrocery(GroceryItem("banana-second", " bananas ", 5.0, "pcs", expiry))

        val inventory = repository.observeGroceries().first()

        assertEquals(1, inventory.size)
        assertEquals("banana-first", inventory.single().id)
        assertEquals(10.0, inventory.single().amount, 0.0)
    }

    @Test
    fun sameProductWithDifferentExpiryDatesRemainsSeparateBatches() = runBlocking {
        repository.saveGrocery(GroceryItem("banana-first", "Bananas", 5.0, "pcs", LocalDate.of(2026, 11, 29)))
        repository.saveGrocery(GroceryItem("banana-second", "Bananas", 5.0, "pcs", LocalDate.of(2026, 11, 30)))

        assertEquals(2, repository.observeGroceries().first().size)
    }

    @Test
    fun restockMovesCheckedItemOnceWithChosenFields() = runBlocking {
        repository.addShoppingItem(ShoppingEntry("Milk", 1.0, "bottle"))
        val id = repository.observeShoppingItems().first().single().id
        repository.setShoppingItemChecked(id, true)
        val expiry = LocalDate.of(2026, 12, 8)

        repeat(2) { repository.restockShoppingItem(id, "Whole milk", 2.5, "L", expiry) }

        assertTrue(repository.observeShoppingItems().first().isEmpty())
        val grocery = repository.observeGroceries().first().single()
        assertEquals(GroceryItem(grocery.id, "Whole milk", 2.5, "L", expiry), grocery)
    }

    @Test
    fun expirySweepLeavesLastUsableDayAndQueuesOlderItemsExactlyOnce() = runBlocking {
        val today = LocalDate.of(2026, 11, 26)
        val expired = GroceryItem("yogurt-old", "Yogurt", 2.0, "cups", today.minusDays(1))
        val stillUsableToday = GroceryItem("yogurt-today", "Yogurt", 1.0, "cup", today)
        repository.saveGrocery(expired)
        repository.saveGrocery(stillUsableToday)

        val firstSweep = repository.processExpiredItems(today)
        val secondSweep = repository.processExpiredItems(today)

        assertEquals(listOf(expired), firstSweep)
        assertTrue(secondSweep.isEmpty())
        assertEquals(listOf(stillUsableToday), repository.observeGroceries().first())
        val shopping = repository.observeShoppingItems().first()
        assertEquals(1, shopping.size)
        assertEquals(expired.name, shopping.single().name)
        assertEquals(expired.amount, shopping.single().amount, 0.0)
    }
}
