package com.alexandrdisaster.fridgetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexandrdisaster.fridgetracker.data.RoomPantryRepository
import com.alexandrdisaster.fridgetracker.notifications.ExpiryNotificationScheduler
import com.alexandrdisaster.fridgetracker.ui.GroceryScreen
import com.alexandrdisaster.fridgetracker.ui.GroceryViewModel

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ExpiryNotificationScheduler.schedule(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = RoomPantryRepository(applicationContext)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GroceryViewModel(repository) as T
        }

        setContent {
            MaterialTheme {
                val groceryViewModel: GroceryViewModel = viewModel(factory = factory)
                GroceryScreen(groceryViewModel)
            }
        }
    }
}
