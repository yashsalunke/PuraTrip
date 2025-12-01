package com.ysdigi.puratrip.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ysdigi.puratrip.login.LoginActivity
import com.ysdigi.puratrip.ui.theme.PuraTripTheme

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("pura_trip_prefs", MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)

        if (hasSeenOnboarding) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            setContent {
                PuraTripTheme {
                    OnboardingScreen {
                        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
