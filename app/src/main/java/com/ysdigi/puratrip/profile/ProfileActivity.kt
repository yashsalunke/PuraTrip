package com.ysdigi.puratrip.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ysdigi.puratrip.ui.theme.PuraTripTheme

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PuraTripTheme {
                ProfileScreen()
            }
        }
    }
}
