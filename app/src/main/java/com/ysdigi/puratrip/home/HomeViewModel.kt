package com.ysdigi.puratrip.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.models.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    fun listenForTrips(email: String) {
        db.collection("trips").whereArrayContains("users", email)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HomeViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _trips.value = snapshot.toObjects()

                    // Check for and migrate any old data.
                    snapshot.documents.forEach { document ->
                        if (!document.contains("photoCount") || !document.contains("expenseCount") || !document.contains("totalAmount")) {
                            viewModelScope.launch {
                                migrateTripData(document.id)
                            }
                        }
                    }
                } else {
                    Log.d("HomeViewModel", "Current data: null")
                }
            }
    }

    private suspend fun migrateTripData(tripId: String) {
        try {
            Log.d("HomeViewModel", "Migrating data for trip: $tripId")
            val photosQuery = db.collection("trips").document(tripId).collection("photos").get().await()
            val expensesQuery = db.collection("trips").document(tripId).collection("expenses").get().await()

            val photoCount = photosQuery.size()
            val expenses = expensesQuery.toObjects<Expense>()
            val expenseCount = expenses.size
            val totalAmount = expenses.sumOf { it.amount }

            db.collection("trips").document(tripId).update(
                mapOf(
                    "photoCount" to photoCount,
                    "expenseCount" to expenseCount,
                    "totalAmount" to totalAmount
                )
            ).await()
            Log.d("HomeViewModel", "Successfully migrated data for trip: $tripId")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error migrating trip data for $tripId", e)
        }
    }

    suspend fun addTrip(tripName: String, users: List<String>): Boolean {
        return try {
            val trip = Trip(
                name = tripName,
                description = "This is a new trip called $tripName",
                users = users,
                photoCount = 0,
                expenseCount = 0,
                totalAmount = 0.0
            )
            db.collection("trips").add(trip).await()
            true
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error adding trip", e)
            false
        }
    }
}
