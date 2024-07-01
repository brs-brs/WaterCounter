package com.tiagodanin.waterwearos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tiagodanin.waterwearos.presentation.WaterCounterManager

class DrinkButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("brs", "Drink button: onReceive")
        val sharedPreferences = context.getSharedPreferences("WaterCounterPrefs", Context.MODE_PRIVATE)
        WaterCounterManager.handleDrinkButtonClicked(sharedPreferences)
    }
}