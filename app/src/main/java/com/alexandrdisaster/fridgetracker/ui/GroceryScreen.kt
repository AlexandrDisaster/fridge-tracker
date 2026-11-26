package com.alexandrdisaster.fridgetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alexandrdisaster.fridgetracker.core.GroceryItem
import com.alexandrdisaster.fridgetracker.core.ShoppingItem
import kotlinx.coroutines.launch

@Composable
fun GroceryScreen(viewModel: GroceryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val groceryItems by viewModel.groceryState.collectAsState()
    val shoppingItems by viewModel.shoppingState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fridge Tracker") }) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is GroceryViewModel.UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GroceryViewModel.UiState.Error -> {
                    Text("Error: ${(uiState as GroceryViewModel.UiState.Error).message}", modifier = Modifier.align(Alignment.Center))
                }
                is GroceryViewModel.UiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Fridge Section
                        Text("Fridge Inventory", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                        if (groceryItems.isEmpty()) {
                            Text("No groceries in the fridge.", modifier = Modifier.padding(16.dp))
                        } else {
                            GroceryList(groceryItems = groceryItems, viewModel = viewModel)
                        }

                        Divider()

                        // Shopping List Section
                        Text("Shopping List", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
                        if (shoppingItems.isEmpty()) {
                            Text("Shopping list is empty.", modifier = Modifier.padding(16.dp))
                        } else {
                            ShoppingList(shoppingItems = shoppingItems, viewModel = viewModel)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Actions
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(onClick = {
                                coroutineScope.launch {
                                    viewModel.checkAndProcessExpiredItems()
                                }
                            }) {
                                Text("Check Expired")
                            }
                            // Simplified Add Button placeholder
                            Button(onClick = { /* Show Add Grocery/Shopping Dialog */ }) {
                                Text("Add Item")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroceryItemCard(item: GroceryItem, viewModel: GroceryViewModel) {
    val isExpired = item.expiryDate.isBefore(java.time.LocalDate.now())
    val cardColor = if (isExpired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text("${item.amount} ${item.unit}", style = MaterialTheme.typography.titleSmall)
            }
            Text("Expiry: ${item.expiryDate}", style = MaterialTheme.typography.bodySmall)
            
            if (isExpired) {
                Text("⚠️ EXPIRED", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Consume Button
                Button(onClick = {
                    // Consume half the amount as example
                    val delta = -item.amount / 2.0 
                    coroutineScope.launch { viewModel.adjustAmount(item.id, delta) }
                }, modifier = Modifier.padding(end = 4.dp)) {
                    Text("Consume")
                }
                // Remove Button
                Button(onClick = {
                    coroutineScope.launch { viewModel.removeGrocery(item.id) }
                }) {
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun GroceryList(groceryItems: List<GroceryItem>, viewModel: GroceryViewModel) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(groceryItems) { item ->
            GroceryItemCard(item = item, viewModel = viewModel)
        }
    }
}

@Composable
fun ShoppingItem(item: ShoppingItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = { checked ->
            // Assuming item.id is sufficient to update state
            // In a real app, we'd pass the viewModel and item ID
            // For simulation, we rely on the ViewModel scope
        })
        Text(item.name, style = MaterialTheme.typography.bodyMedium)
        Text("${item.amount} ${item.unit}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ShoppingList(shoppingItems: List<ShoppingItem>, viewModel: GroceryViewModel) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(shoppingItems) { item ->
            // Note: Real implementation needs lambda for viewModel calls
            ShoppingItem(item = item)
        }
    }
}
