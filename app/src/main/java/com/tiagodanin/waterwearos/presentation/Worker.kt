package com.tiagodanin.waterwearos.presentation

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tiagodanin.waterwearos.R
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tiagodanin.waterwearos.DrinkButtonReceiver


class ResetWaterCounterWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("WaterCounterPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("todayDrinkedLiters", 0f)
            apply()
        }
        Log.d("brs","Reset drinks on calendar")
        return Result.success()
    }
}

class DrinkNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        Log.d("brs","DrinkNotificationWorker fired")
        if (currentHour in 0..22) {
            Log.d("brs","Drink now!")
            createNotificationChannel()
            showNotification()
        } else {
            Log.d("brs","and snoozed internally")
        }
        return Result.success()
    }

    private fun createNotificationChannel() {
        val name = "DrinkNotificationChannel"
        val descriptionText = "Channel for Drink Notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("drink_notification_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // FLAG_UPDATE_CURRENT ?

        // Intent for increasing drink count
        val increaseIntent = Intent(applicationContext, DrinkButtonReceiver::class.java)
        val increasePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(applicationContext, 0, increaseIntent, PendingIntent.FLAG_IMMUTABLE)

        val action = NotificationCompat.Action.Builder(
            R.drawable.cup_water,  // Icon for the action
            "Sure! Add one glass",            // Title for the action
            increasePendingIntent
        ).build()

        val builder = NotificationCompat.Builder(applicationContext, "drink_notification_channel")
            .setSmallIcon(R.drawable.cup_water)
            .setContentTitle("Time to drink!")
            //.setContentText("It's time to drink water.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(action)  // Add the action button to the notification

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is granted, proceed with showing the notification
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(1, builder.build())
            }
        }
    }
}