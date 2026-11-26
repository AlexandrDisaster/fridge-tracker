package com.alexandrdisaster.fridgetracker.data

import android.content.Context
import androidx.room.withTransaction
import com.alexandrdisaster.fridgetracker.core.*
import com.alexandrdisaster.fridgetracker.notifications.PantryAlertNotifications
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class RoomPantryRepository internal constructor(
    private val db: PantryDatabase,
    private val appContext: Context,
) : PantryRepository {
    constructor(context: Context) : this(
        PantryDatabase.get(context.applicationContext),
        context.applicationContext,
    )

    private val dao = db.pantry()

    override fun observeGroceries() = dao.groceries().map { rows -> rows.map(GroceryRow::item) }
    override fun observeShoppingItems() = dao.shopping().map { rows -> rows.map(ShoppingRow::item) }

    override suspend fun saveGrocery(item: GroceryItem) {
        require(item.amount.isFinite())
        val row = GroceryRow(item.id, item.name, item.amount, item.unit, item.expiryDate.toEpochDay())
        var finished = false
        db.withTransaction {
            if (item.isFinished) {
                moveToShopping(row.copy(amount = dao.grocery(item.id)?.amount ?: 1.0))
                finished = true
            } else {
                saveOrStack(row)
            }
        }
        if (finished) PantryAlertNotifications.finished(appContext, item.name)
    }

    override suspend fun adjustAmount(groceryId: String, delta: Double) {
        var finishedItem: GroceryRow? = null
        db.withTransaction {
            val row = dao.grocery(groceryId) ?: return@withTransaction
            val remaining = row.adjusted(delta)
            // Replenish the last positive quantity rather than queuing a zero-quantity entry.
            if (remaining == null) {
                moveToShopping(row)
                finishedItem = row
            } else {
                dao.save(remaining)
            }
        }
        finishedItem?.let { PantryAlertNotifications.finished(appContext, it.name) }
    }

    override suspend fun processExpiredItems(today: LocalDate): List<GroceryItem> {
        val expired = db.withTransaction {
            val rows = dao.expired(today.toEpochDay())
            for (row in rows) moveToShopping(row)
            rows.map(GroceryRow::item)
        }
        PantryAlertNotifications.expired(appContext, expired.map(GroceryItem::name))
        return expired
    }

    /** Merge identical product batches, but keep different units or expiry dates distinct. */
    private suspend fun saveOrStack(row: GroceryRow) {
        val matchingBatch = dao.groceriesExpiringOn(row.expiryDay).firstOrNull { candidate ->
            candidate.id != row.id &&
                normalize(candidate.name) == normalize(row.name) &&
                normalize(candidate.unit) == normalize(row.unit)
        }
        if (matchingBatch == null) {
            dao.save(row)
            return
        }

        val combinedAmount = matchingBatch.amount + row.amount
        require(combinedAmount.isFinite()) { "Combined grocery amount must be finite." }
        dao.save(matchingBatch.copy(amount = combinedAmount))
        dao.removeGrocery(row.id)
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), "")

    private suspend fun moveToShopping(row: GroceryRow) {
        addShoppingItem(ShoppingEntry(row.name, row.amount, row.unit))
        dao.removeGrocery(row.id)
    }

    override suspend fun addShoppingItem(entry: ShoppingEntry) {
        require(entry.name.isNotBlank() && entry.unit.isNotBlank())
        require(entry.amount.isFinite() && entry.amount > 0.0)
        dao.add(ShoppingRow(UUID.randomUUID().toString(), entry.name, entry.amount, entry.unit))
    }

    override suspend fun restockShoppingItem(
        shoppingItemId: String, name: String, amount: Double, unit: String, expiryDate: LocalDate,
    ) {
        db.withTransaction {
            if (dao.removeShopping(shoppingItemId) == 0) return@withTransaction
            require(name.isNotBlank() && unit.isNotBlank())
            require(amount.isFinite() && amount > 0.0)
            saveOrStack(GroceryRow(UUID.randomUUID().toString(), name.trim(), amount, unit.trim(), expiryDate.toEpochDay()))
        }
    }

    override suspend fun removeGrocery(groceryId: String) = dao.removeGrocery(groceryId)
    override suspend fun setShoppingItemChecked(itemId: String, checked: Boolean) = dao.check(itemId, checked)
    override suspend fun removeShoppingItem(itemId: String) {
        dao.removeShopping(itemId)
    }
}
