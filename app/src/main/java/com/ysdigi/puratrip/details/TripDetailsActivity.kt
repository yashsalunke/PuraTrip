package com.ysdigi.puratrip.details

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.ysdigi.puratrip.ui.theme.PuraTripTheme

class TripDetailsActivity : ComponentActivity() {
    private val viewModel: TripDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tripId = intent.getStringExtra("tripId") ?: return
        viewModel.initialize(tripId)
        val sharedPreferences = getSharedPreferences("PuraTripPrefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", null) ?: return

        setContent {
            PuraTripTheme {
                TripDetailsScreen(viewModel, tripId, email)
            }
        }
    }
}
