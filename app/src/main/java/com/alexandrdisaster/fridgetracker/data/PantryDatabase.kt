package com.alexandrdisaster.fridgetracker.data

import android.content.Context
import androidx.room.*
import com.alexandrdisaster.fridgetracker.core.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "groceries")
internal data class GroceryRow(
    @PrimaryKey val id: String, val name: String, val amount: Double, val unit: String,
    val expiryDay: Long,
) {
    fun adjusted(delta: Double): GroceryRow? {
        require(delta.isFinite())
        val remaining = amount + delta
        require(remaining.isFinite())
        return if (remaining > 0.0) copy(amount = remaining) else null
    }

    fun item() = GroceryItem(id, name, amount, unit, LocalDate.ofEpochDay(expiryDay))
}

@Entity(tableName = "shopping")
internal data class ShoppingRow(
    @PrimaryKey val id: String, val name: String, val amount: Double, val unit: String,
    val isChecked: Boolean = false,
) {
    fun item() = ShoppingItem(id, name, amount, unit, isChecked)
}

@Dao
internal interface PantryDao {
    @Query("SELECT * FROM groceries ORDER BY expiryDay, name, id")
    fun groceries(): Flow<List<GroceryRow>>

    @Query("SELECT * FROM shopping ORDER BY isChecked, name, id")
    fun shopping(): Flow<List<ShoppingRow>>

    @Query("SELECT * FROM groceries WHERE id = :id")
    suspend fun grocery(id: String): GroceryRow?

    // The printed date is the final usable day, in the device's local time zone.
    @Query("SELECT * FROM groceries WHERE expiryDay < :today ORDER BY expiryDay, name, id")
    suspend fun expired(today: Long): List<GroceryRow>

    @Upsert suspend fun save(row: GroceryRow)
    @Insert suspend fun add(row: ShoppingRow)

    @Query("DELETE FROM groceries WHERE id = :id")
    suspend fun removeGrocery(id: String)

    @Query("DELETE FROM shopping WHERE id = :id")
    suspend fun removeShopping(id: String): Int

    @Query("UPDATE shopping SET isChecked = :checked WHERE id = :id")
    suspend fun check(id: String, checked: Boolean)
}

@Database(entities = [GroceryRow::class, ShoppingRow::class], version = 1, exportSchema = false)
internal abstract class PantryDatabase : RoomDatabase() {
    abstract fun pantry(): PantryDao

    companion object {
        @Volatile private var instance: PantryDatabase? = null

        fun get(context: Context): PantryDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PantryDatabase::class.java, "pantry.db")
                .build().also { instance = it }
        }
    }
}
