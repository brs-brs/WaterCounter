package com.tiagodanin.waterwearos.presentation
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.material.*
import com.tiagodanin.waterwearos.R
import com.tiagodanin.waterwearos.presentation.theme.WaterWearOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("WaterCounterPrefs", Context.MODE_PRIVATE)
        val savedTodayDrinkedLiters = sharedPreferences.getFloat("todayDrinkedLiters", 0f)
        val savedLastPressTime = sharedPreferences.getLong("lastPressTime", 0L)
        // Set the loaded values to the MutableState variables
        todayDrinkedLiters.value = savedTodayDrinkedLiters
        lastButtonPressTime.value = if (savedLastPressTime == 0L) null else savedLastPressTime
        setContent {
            WearApp(sharedPreferences)
        }
    }
}

private val todayDrinkedLiters: MutableState<Float> = mutableFloatStateOf(0f)
private val lastButtonPressTime: MutableState<Long?> = mutableStateOf(null)

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
    val progressOfDay: Float = todayDrinkedLiters.value / dailyTarget

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
        InfoWater(sharedPreferences)
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

@Composable
fun InfoWater(sharedPreferences: SharedPreferences) {
    var timeSinceLastPress by remember { mutableStateOf("") }
    var lastButtonPressTime by remember { mutableStateOf<Long?>(null) }

    fun updateTimeSinceLastPress() {
        timeSinceLastPress = lastButtonPressTime?.let {
            val currentTime = System.currentTimeMillis()
            val elapsedMillis = currentTime - it
            formatElapsedTime(elapsedMillis)
        } ?: "a while"
    }

    // Retrieve last button press time and count from shared preferences
    LaunchedEffect(Unit) {
        lastButtonPressTime = sharedPreferences.getLong("lastPressTime", 0L).takeIf { it != 0L }
    }

    // Launching a coroutine to update the time difference every second
    LaunchedEffect(Unit) {
        while (true) {
            updateTimeSinceLastPress()
            delay(1000L)
        }
    }

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
            text = "Today it was \n${"%.1f".format(todayDrinkedLiters.value)} liters"
        )
        Row {
            // Drink button
            Button(
                modifier = Modifier.padding(top = 10.dp),
                onClick = {
                    todayDrinkedLiters.value += 0.2f
                    val currentMillis = System.currentTimeMillis()
                    lastButtonPressTime = currentMillis
                    with(sharedPreferences.edit()) {
                        putFloat("todayDrinkedLiters", todayDrinkedLiters.value)
                        putLong("lastPressTime", currentMillis)
                        apply()
                    }
                    updateTimeSinceLastPress()
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
                    todayDrinkedLiters.value = 0f
                    lastButtonPressTime = null
                    with(sharedPreferences.edit()) {
                        putFloat("todayDrinkedLiters", 0f)
                        putLong("lastPressTime", 0L)
                        apply()
                    }
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
            text = "Since last drink:\n$timeSinceLastPress",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colors.primary,
            textAlign = TextAlign.Center
        )
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