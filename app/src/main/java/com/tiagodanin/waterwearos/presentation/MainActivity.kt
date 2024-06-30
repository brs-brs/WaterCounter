package com.tiagodanin.waterwearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.WEAR_OS_LARGE_ROUND
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.tiagodanin.waterwearos.R
import com.tiagodanin.waterwearos.presentation.theme.WaterWearOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

private val count: MutableState<Float> = mutableFloatStateOf(0f)

@Composable
fun WearApp() {
    WaterWearOSTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            timeText = {
                TimeText()
            },
        ) {
            ProgressIndicatorWater()
        }
    }
}

@Composable
fun ProgressIndicatorWater() {
    val dailyTarget = 2.0f  // liters
    val progressOfDay: Float = count.value / dailyTarget

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
        InfoWater()
    }
}

@Composable
fun InfoWater() {
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
            text = "Today it was \n${"%.1f".format(count.value)} liters"
        )
        Button(
            modifier = Modifier.padding(top = 5.dp),
            onClick = { count.value += 0.2f },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cup_water),
                contentDescription = "cup_water",
                modifier = Modifier
                    .size(ButtonDefaults.DefaultButtonSize)
                    .wrapContentSize(align = Alignment.Center),
            )
        }
    }
}

@Composable
fun DefaultPreview() {
    WearApp()
}