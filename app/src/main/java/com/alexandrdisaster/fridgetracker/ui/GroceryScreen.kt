package com.alexandrdisaster.fridgetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alexandrdisaster.fridgetracker.core.GroceryItem
import com.alexandrdisaster.fridgetracker.core.ShoppingItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class PantryTab { Fridge, Shopping }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryScreen(viewModel: GroceryViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(PantryTab.Fridge) }
    var editingItem by remember { mutableStateOf<GroceryItem?>(null) }
    var showGroceryDialog by rememberSaveable { mutableStateOf(false) }
    var showShoppingDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = {
                    Column {
                        Text("Fridge Tracker")
                        Text(
                            "Know what you have. Waste less.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                })
                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    Tab(
                        selected = selectedTab == PantryTab.Fridge,
                        onClick = { selectedTab = PantryTab.Fridge },
                        text = { Text("Fridge (${state.groceries.size})") },
                    )
                    Tab(
                        selected = selectedTab == PantryTab.Shopping,
                        onClick = { selectedTab = PantryTab.Shopping },
                        text = { Text("Shopping (${state.shoppingItems.count { !it.isChecked }})") },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == PantryTab.Fridge) {
                    editingItem = null
                    showGroceryDialog = true
                } else {
                    showShoppingDialog = true
                }
            }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { padding ->
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.padding(padding).fillMaxSize().wrapContentSize(Alignment.Center),
            )
            selectedTab == PantryTab.Fridge -> {
                if (state.groceries.isEmpty()) {
                    EmptyState(
                        title = "Your fridge is empty",
                        description = "Add groceries to track quantities and expiry dates.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "Your inventory",
                                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(state.groceries, key = GroceryItem::id) { item ->
                            GroceryCard(
                                item = item,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onEdit = {
                                    editingItem = item
                                    showGroceryDialog = true
                                },
                                onUseOne = { viewModel.adjustAmount(item, -minOf(1.0, item.amount)) },
                                onFinish = { viewModel.finishGrocery(item) },
                                onRemove = { viewModel.removeGrocery(item.id) },
                            )
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
            else -> {
                if (state.shoppingItems.isEmpty()) {
                    EmptyState(
                        title = "Shopping list is clear",
                        description = "Items you finish or that expire will appear here automatically.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                "To pick up",
                                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 2.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(state.shoppingItems, key = ShoppingItem::id) { item ->
                            ShoppingCard(
                                item = item,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onCheckedChange = { checked ->
                                    viewModel.setShoppingItemChecked(item.id, checked)
                                },
                                onRemove = { viewModel.removeShoppingItem(item.id) },
                            )
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    if (showGroceryDialog) {
        GroceryEditorDialog(
            item = editingItem,
            onDismiss = {
                showGroceryDialog = false
                editingItem = null
            },
            onSave = { name, amount, unit, expiry ->
                viewModel.saveGrocery(name, amount, unit, expiry, editingItem?.id)
                showGroceryDialog = false
                editingItem = null
            },
        )
    }

    if (showShoppingDialog) {
        ShoppingEditorDialog(
            onDismiss = { showShoppingDialog = false },
            onSave = { name, amount, unit ->
                viewModel.addShoppingItem(name, amount, unit)
                showShoppingDialog = false
            },
        )
    }
}

@Composable
private fun EmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroceryCard(
    item: GroceryItem,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onUseOne: () -> Unit,
    onFinish: () -> Unit,
    onRemove: () -> Unit,
) {
    val today = LocalDate.now()
    val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, item.expiryDate)
    val expired = daysLeft < 0
    val status = when {
        expired -> "Expired ${-daysLeft} day${if (daysLeft == -1L) "" else "s"} ago"
        daysLeft == 0L -> "Use today"
        daysLeft == 1L -> "Expires tomorrow"
        else -> "Expires in $daysLeft days"
    }
    val colors = CardDefaults.cardColors(
        containerColor = when {
            expired -> MaterialTheme.colorScheme.errorContainer
            daysLeft in 0..2 -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    )

    Card(modifier = modifier.fillMaxWidth(), colors = colors) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(status, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${formatAmount(item.amount)} ${item.unit}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onUseOne) { Text("Use 1") }
                TextButton(onClick = onFinish) { Text("Finished") }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun ShoppingCard(
    item: ShoppingItem,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = onCheckedChange)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "${formatAmount(item.amount)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryEditorDialog(
    item: GroceryItem?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, LocalDate) -> Unit,
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var amount by remember(item?.id) { mutableStateOf(item?.let { formatAmount(it.amount) } ?: "1") }
    var unit by remember(item?.id) { mutableStateOf(item?.unit ?: "pcs") }
    var expiry by remember(item?.id) { mutableStateOf(item?.expiryDate ?: LocalDate.now().plusDays(7)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && unit.isNotBlank() && parsedAmount != null &&
        parsedAmount.isFinite() && parsedAmount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add to your fridge" else "Edit grocery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Grocery name") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Amount") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Unit") },
                        singleLine = true,
                    )
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Expiry date: $expiry")
                }
                Text(
                    "The expiry date is treated as the last usable day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(name.trim(), parsedAmount ?: 0.0, unit.trim(), expiry) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiry.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expiry = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ShoppingEditorDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pcs") }
    val parsedAmount = amount.toDoubleOrNull()
    val valid = name.isNotBlank() && unit.isNotBlank() && parsedAmount != null &&
        parsedAmount.isFinite() && parsedAmount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to shopping list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Amount") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Unit") },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onSave(name.trim(), parsedAmount ?: 0.0, unit.trim()) },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
