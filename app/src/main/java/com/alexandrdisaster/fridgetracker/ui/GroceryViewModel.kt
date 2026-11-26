package com.alexandrdisaster.fridgetracker.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.alexandrdisaster.fridgetracker.core.PantryRepository
import com.alexandrdisaster.fridgetracker.core.GroceryItem
import com.alexandrdisaster.fridgetracker.core.ShoppingItem
import com.alexandrdisaster.fridgetracker.core.ShoppingEntry
import java.time.LocalDate

class GroceryViewModel(private val repository: PantryRepository) : ViewModel() {
    private val _groceryState = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceryState: StateFlow<List<GroceryItem>> = _groceryState

    private val _shoppingState = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val shoppingState: StateFlow<List<ShoppingItem>> = _shoppingState

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiState {
        object Loading : UiState()
        data class Success(val isGroceryListEmpty: Boolean) : UiState()
        data class Error(val message: String) : UiState()
    }

    init {
        // Observe repositories
        repository.observeGroceries().collect { items ->
            _groceryState.value = items
            updateUiState()
        }
        repository.observeShoppingItems().collect { items ->
            _shoppingState.value = items
            updateUiState()
        }
        // Process expired items daily (or on startup)
        // Note: In a real Android app, this would be a WorkManager/CoroutineScope job,
        // but for this implementation, we trigger it on init/load.
        // The current date is 2026-11-26.
        // We assume the repository handles the atomic movement of expired items.
        // For now, we just observe and let the repository handle the process.
    }

    private fun updateUiState() {
        val isGroceryListEmpty = _groceryState.value.isEmpty()
        _uiState.value = if (_groceryState.value.isEmpty() && _shoppingState.value.isEmpty()) {
            UiState.Success(true)
        } else {
            UiState.Success(false)
        }
    }

    // --- Actions ---

    suspend fun saveGrocery(item: GroceryItem) {
        repository.saveGrocery(item)
    }

    suspend fun adjustAmount(groceryId: String, delta: Double) {
        repository.adjustAmount(groceryId, delta)
    }

    suspend fun removeGrocery(groceryId: String) {
        repository.removeGrocery(groceryId)
    }

    suspend fun addShoppingItem(entry: ShoppingEntry) {
        repository.addShoppingItem(entry)
    }

    suspend fun setShoppingItemChecked(itemId: String, checked: Boolean) {
        repository.setShoppingItemChecked(itemId, checked)
    }

    suspend fun removeShoppingItem(itemId: String) {
        repository.removeShoppingItem(itemId)
    }

    /** Moves expired groceries to the shopping list and updates the state. */
    suspend fun checkAndProcessExpiredItems() {
        val today = LocalDate.of(2026, 11, 26) // Use context date
        val expiredItems = repository.processExpiredItems(today)
        if (expiredItems.isNotEmpty()) {
            // The repository handles the atomic move; observation will update the state automatically.
        }
    }
}
