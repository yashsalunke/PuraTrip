package com.ysdigi.puratrip.details

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.ui.theme.PuraTripTheme

class TripDetailsActivity : ComponentActivity() {
    private val viewModel: TripDetailsViewModel by viewModels()
    private val userName = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tripId = intent.getStringExtra("tripId") ?: return
        viewModel.initialize(tripId)
        setContent {
            PuraTripTheme {
                TripDetailsScreen(viewModel, tripId, userName.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserName()
    }

    private fun fetchUserName() {
        val user = Firebase.auth.currentUser
        val db = Firebase.firestore

        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userName.value = document.getString("name") ?: user.email ?: ""
                    }
                }
        }
    }
}
