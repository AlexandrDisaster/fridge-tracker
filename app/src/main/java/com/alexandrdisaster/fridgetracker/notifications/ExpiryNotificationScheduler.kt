package com.alexandrdisaster.fridgetracker.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Build
import androidx.work.*
import com.alexandrdisaster.fridgetracker.data.RoomPantryRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val CHANNEL = "pantry-expiry"

object ExpiryNotificationScheduler {
    fun schedule(context: Context) {
        requireNotNull(context.getSystemService(NotificationManager::class.java)).createNotificationChannel(
            NotificationChannel(CHANNEL, "Expired groceries", NotificationManager.IMPORTANCE_DEFAULT)
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "pantry-daily-expiry", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ExpiryWorker>(1, TimeUnit.DAYS).build()
        )
    }
}

class ExpiryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val expired = try {
            RoomPantryRepository(context).processExpiredItems(LocalDate.now())
        } catch (locked: SQLiteDatabaseLockedException) {
            return Result.retry()
        }
        if (expired.isEmpty()) return Result.success()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return Result.success()
        if (!manager.areNotificationsEnabled()) return Result.success()

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Expired groceries moved to shopping list")
            .setContentText(expired.take(3).joinToString { it.name })
            .setAutoCancel(true)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            notification.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        }
        manager.notify(1, notification.build())
        return Result.success()
    }
}
