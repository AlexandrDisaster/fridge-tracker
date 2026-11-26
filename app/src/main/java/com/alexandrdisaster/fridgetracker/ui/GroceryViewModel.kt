package com.alexandrdisaster.fridgetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexandrdisaster.fridgetracker.core.GroceryItem
import com.alexandrdisaster.fridgetracker.core.PantryRepository
import com.alexandrdisaster.fridgetracker.core.ShoppingEntry
import com.alexandrdisaster.fridgetracker.core.ShoppingItem
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PantryUiState(
    val groceries: List<GroceryItem> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class GroceryViewModel(private val repository: PantryRepository) : ViewModel() {
    private val errors = MutableStateFlow<String?>(null)

    val state: StateFlow<PantryUiState> = combine(
        repository.observeGroceries(),
        repository.observeShoppingItems(),
        errors,
    ) { groceries, shoppingItems, error ->
        PantryUiState(
            groceries = groceries,
            shoppingItems = shoppingItems,
            isLoading = false,
            error = error,
        )
    }.catch { cause ->
        emit(PantryUiState(isLoading = false, error = cause.message ?: "Could not load pantry data."))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PantryUiState())

    init {
        launchOperation { repository.processExpiredItems(LocalDate.now()) }
    }

    fun saveGrocery(
        name: String,
        amount: Double,
        unit: String,
        expiryDate: LocalDate,
        existingId: String? = null,
    ) {
        val cleanName = name.trim()
        val cleanUnit = unit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty() || !amount.isFinite() || amount <= 0.0) {
            errors.value = "Enter a name, a positive amount, and a unit."
            return
        }
        val item = GroceryItem(
            id = existingId ?: UUID.randomUUID().toString(),
            name = cleanName,
            amount = amount,
            unit = cleanUnit,
            expiryDate = expiryDate,
        )
        launchOperation { repository.saveGrocery(item) }
    }

    fun adjustAmount(item: GroceryItem, delta: Double) {
        if (!delta.isFinite() || delta == 0.0) return
        launchOperation { repository.adjustAmount(item.id, delta) }
    }

    fun finishGrocery(item: GroceryItem) = adjustAmount(item, -item.amount)

    fun removeGrocery(groceryId: String) =
        launchOperation { repository.removeGrocery(groceryId) }

    fun addShoppingItem(name: String, amount: Double, unit: String) {
        val cleanName = name.trim()
        val cleanUnit = unit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty() || !amount.isFinite() || amount <= 0.0) {
            errors.value = "Enter a name, a positive amount, and a unit."
            return
        }
        launchOperation { repository.addShoppingItem(ShoppingEntry(cleanName, amount, cleanUnit)) }
    }

    fun setShoppingItemChecked(itemId: String, checked: Boolean) =
        launchOperation { repository.setShoppingItemChecked(itemId, checked) }

    fun removeShoppingItem(itemId: String) =
        launchOperation { repository.removeShoppingItem(itemId) }

    fun clearError() {
        errors.value = null
    }

    private fun launchOperation(operation: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operation()
            } catch (cause: Exception) {
                errors.value = cause.message ?: "Something went wrong. Please try again."
            }
        }
    }
}
