package com.alexandrdisaster.fridgetracker.data

import android.content.Context
import androidx.room.withTransaction
import com.alexandrdisaster.fridgetracker.core.*
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class RoomPantryRepository(context: Context) : PantryRepository {
    private val db = PantryDatabase.get(context)
    private val dao = db.pantry()

    override fun observeGroceries() = dao.groceries().map { rows -> rows.map(GroceryRow::item) }
    override fun observeShoppingItems() = dao.shopping().map { rows -> rows.map(ShoppingRow::item) }

    override suspend fun saveGrocery(item: GroceryItem) {
        require(item.amount.isFinite())
        val row = GroceryRow(item.id, item.name, item.amount, item.unit, item.expiryDate.toEpochDay())
        db.withTransaction {
            if (item.isFinished) moveToShopping(row.copy(amount = dao.grocery(item.id)?.amount ?: 1.0))
            else dao.save(row)
        }
    }

    override suspend fun adjustAmount(groceryId: String, delta: Double) {
        db.withTransaction {
            val row = dao.grocery(groceryId) ?: return@withTransaction
            val remaining = row.adjusted(delta)
            // Replenish the last positive quantity rather than queuing a zero-quantity entry.
            if (remaining == null) moveToShopping(row) else dao.save(remaining)
        }
    }

    override suspend fun processExpiredItems(today: LocalDate): List<GroceryItem> = db.withTransaction {
        val expired = dao.expired(today.toEpochDay())
        for (row in expired) moveToShopping(row)
        expired.map(GroceryRow::item)
    }

    private suspend fun moveToShopping(row: GroceryRow) {
        addShoppingItem(ShoppingEntry(row.name, row.amount, row.unit))
        dao.removeGrocery(row.id)
    }

    override suspend fun addShoppingItem(entry: ShoppingEntry) {
        require(entry.name.isNotBlank() && entry.unit.isNotBlank())
        require(entry.amount.isFinite() && entry.amount > 0.0)
        dao.add(ShoppingRow(UUID.randomUUID().toString(), entry.name, entry.amount, entry.unit))
    }

    override suspend fun removeGrocery(groceryId: String) = dao.removeGrocery(groceryId)
    override suspend fun setShoppingItemChecked(itemId: String, checked: Boolean) = dao.check(itemId, checked)
    override suspend fun removeShoppingItem(itemId: String) = dao.removeShopping(itemId)
}
