package com.alexandrdisaster.fridgetracker.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.database.sqlite.SQLiteDatabaseLockedException
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alexandrdisaster.fridgetracker.data.RoomPantryRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val CHANNEL = "pantry-alerts"

object ExpiryNotificationScheduler {
    fun schedule(context: Context) {
        PantryAlertNotifications.ensureChannel(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "pantry-daily-expiry",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}

object PantryAlertNotifications {
    private val notificationIds = AtomicInteger(1)

    internal fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Pantry alerts", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun expired(context: Context, groceryNames: List<String>) {
        if (groceryNames.isEmpty()) return
        val names = groceryNames.take(3).joinToString()
        val extra = groceryNames.size - 3
        val message = if (extra > 0) "$names and $extra more moved to your shopping list"
        else "$names moved to your shopping list"
        show(context, "Groceries expired", message)
    }

    fun finished(context: Context, groceryName: String) {
        show(context, "$groceryName finished", "Added to your shopping list")
    }

    private fun show(context: Context, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            notification.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationIds.getAndIncrement(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
        manager.notify(notificationIds.getAndIncrement(), notification.build())
    }
}

class ExpiryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        RoomPantryRepository(applicationContext).processExpiredItems(LocalDate.now())
        Result.success()
    } catch (locked: SQLiteDatabaseLockedException) {
        Result.retry()
    }
}
