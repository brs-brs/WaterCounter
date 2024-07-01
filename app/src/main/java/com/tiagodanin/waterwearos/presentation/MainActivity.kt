package com.tiagodanin.waterwearos.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import kotlinx.coroutines.delay
import androidx.work.*
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.material.*
import com.tiagodanin.waterwearos.R
import com.tiagodanin.waterwearos.presentation.theme.WaterWearOSTheme
import java.util.Calendar
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.tiagodanin.waterwearos.DrinkButtonReceiver

private const val DrinkGlassSize = 0.2F // liters
private const val DrinkReminderInterval = 30L // minutes


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()
        scheduleDailyDrinkReset()
        scheduleDrinkNotifications()

        val sharedPreferences = getSharedPreferences("WaterCounterPrefs", Context.MODE_PRIVATE)

        setContent {
            WearApp(sharedPreferences)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.d("brs", "Permission is NOT granted!!!")
                openSettings()
            } else {
                Log.d("brs", "Permission already granted")
            }
        } else {
            Log.d("brs", "For devices below Android 13 no need for permission")
        }
    }

    private fun openSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun scheduleDailyDrinkReset() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ResetWaterCounterWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ResetWaterCounterWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    private fun scheduleDrinkNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest1 = PeriodicWorkRequestBuilder<DrinkNotificationWorker>(DrinkReminderInterval, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DrinkNotificationWork",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest1
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            // target time
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If the calculated time is before now, add one day to the initial delay
        if (calendar.timeInMillis <= currentTimeMillis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = calendar.timeInMillis - currentTimeMillis
        Log.d("brs", "Initial delay for drink reset: $delay ms")
        return delay
    }
}

@Composable
fun WearApp(sharedPreferences: SharedPreferences) {
    WaterWearOSTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            timeText = {
                TimeText()
            },
        ) {
            ProgressIndicatorWater(sharedPreferences)
        }
    }
}

@Composable
fun ProgressIndicatorWater(sharedPreferences: SharedPreferences) {
    val dailyTarget = 2.0f  // liters

    var todayDrinkedLiters by remember { mutableStateOf(sharedPreferences.getFloat("todayDrinkedLiters", 0f)) }
    var stringButtonLastPressed by remember { mutableStateOf("") }

    LaunchedEffect(sharedPreferences) {
        while (true) {
            todayDrinkedLiters = sharedPreferences.getFloat("todayDrinkedLiters", 0f)
            stringButtonLastPressed = updateStringButtonLastPressed(sharedPreferences)
            delay(1000L)
        }
    }

    val progressOfDay: Float = todayDrinkedLiters / dailyTarget

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            startAngle = 295f,
            endAngle = 245f,
            progress = progressOfDay,
            strokeWidth = 5.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 10.dp)
        )
        InfoWater(
            todayDrinkedLiters = todayDrinkedLiters,
            stringButtonLastPressed = stringButtonLastPressed,
            sharedPreferences = sharedPreferences
        )
    }
}

fun pluralize(value: Long, singular: String): String {
    return if (value == 1L) "$value $singular" else "$value ${singular}s"
}

fun formatElapsedTime(elapsedMillis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMillis)

    return when {
        seconds < 60 -> pluralize(seconds, "second")
        minutes < 60 -> pluralize(minutes, "minute")
        hours < 24 -> pluralize(hours, "hour")
        else -> pluralize(days, "day")
    }
}

fun updateStringButtonLastPressed(sharedPreferences: SharedPreferences): String {
    val lastButtonPressTime = sharedPreferences.getLong("lastPressTime", 0L).takeIf { it != 0L }
    return lastButtonPressTime?.let {
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - it
        formatElapsedTime(elapsedMillis)
    } ?: "a while"
}

@Composable
fun InfoWater(
    todayDrinkedLiters: Float,
    stringButtonLastPressed: String,
    sharedPreferences: SharedPreferences
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = "Today it was \n${"%.1f".format(todayDrinkedLiters)} liters"
        )
        Row {
            // Drink button
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    val increaseIntent = Intent(context, DrinkButtonReceiver::class.java)
                    context.sendBroadcast(increaseIntent)
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cup_water),
                    contentDescription = "cup_water",
                    modifier = Modifier
                        .size(ButtonDefaults.DefaultButtonSize)
                        .wrapContentSize(align = Alignment.Center),
                )
            }

            // Clear button
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    with(sharedPreferences.edit()) {
                        putFloat("todayDrinkedLiters", 0f)
                        putLong("lastPressTime", 0L)
                        apply()
                    }
                    Log.d("brs", "Reset all sharedPreferences manually")
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clear),
                    contentDescription = "clear",
                    modifier = Modifier
                        .size(ButtonDefaults.SmallButtonSize)
                        .wrapContentSize(align = Alignment.Center),
                )
            }
        }
        Text(
            text = "Since last drink:\n$stringButtonLastPressed",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center
        )
    }
}

object WaterCounterManager {
    fun handleDrinkButtonClicked(sharedPreferences: SharedPreferences) {
        Log.d("brs", "Drink button: handleDrinkButtonClicked")
        with(sharedPreferences.edit()) {
            putFloat("todayDrinkedLiters", sharedPreferences.getFloat("todayDrinkedLiters", 0f) + DrinkGlassSize)
            putLong("lastPressTime", System.currentTimeMillis())
            apply()
        }
    }
}


@Preview(device = WearDevices.LARGE_ROUND)
@Composable
fun DefaultPreview(@PreviewParameter(MockSharedPreferencesProvider::class) sharedPreferences: SharedPreferences) {
    WearApp(sharedPreferences)
}

class MockSharedPreferencesProvider : PreviewParameterProvider<SharedPreferences> {
    override val values: Sequence<SharedPreferences> = sequenceOf(
        createMockSharedPreferences()
    )
}

fun createMockSharedPreferences(): SharedPreferences {
    return object : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun contains(key: String?): Boolean = map.containsKey(key)

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue

        override fun edit(): SharedPreferences.Editor {
            return object : SharedPreferences.Editor {
                override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                    map[key!!] = value
                    return this
                }

                override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                    map[key!!] = value
                    return this
                }

                override fun apply() {
                    // Do nothing in mock implementation
                }

                override fun clear(): SharedPreferences.Editor {
                    map.clear()
                    return this
                }

                override fun commit(): Boolean = true
                override fun putLong(key: String?, value: Long): SharedPreferences.Editor { map[key!!] = value; return this }
                override fun putInt(key: String?, value: Int): SharedPreferences.Editor { map[key!!] = value; return this }
                override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { map[key!!] = value; return this }
                override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { map[key!!] = values; return this }
                override fun remove(key: String?): SharedPreferences.Editor { map.remove(key); return this }
            }
        }

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = map[key] as? MutableSet<String>
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }
}