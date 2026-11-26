package com.alexandrdisaster.fridgetracker.core

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Single source of truth for pantry and shopping-list state.
 * Implementations must make finishing/removing an expired grocery and queuing it to the
 * shopping list one atomic operation, so no item is lost between the two lists.
 */
interface PantryRepository {
    fun observeGroceries(): Flow<List<GroceryItem>>
    fun observeShoppingItems(): Flow<List<ShoppingItem>>

    suspend fun saveGrocery(item: GroceryItem)
    suspend fun adjustAmount(groceryId: String, delta: Double)
    suspend fun removeGrocery(groceryId: String)
    suspend fun addShoppingItem(entry: ShoppingEntry)
    suspend fun setShoppingItemChecked(itemId: String, checked: Boolean)
    suspend fun removeShoppingItem(itemId: String)

    /** Moves expired groceries to the shopping list and returns the items that newly expired. */
    suspend fun processExpiredItems(today: LocalDate): List<GroceryItem>
}
