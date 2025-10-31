package com.ysdigi.puratrip.details

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ysdigi.puratrip.home.Trip
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Photo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TripRepository {

    private val db = Firebase.firestore
    private val storage = Firebase.storage

    fun getTripStream(tripId: String): Flow<Trip?> = callbackFlow {
        val listenerRegistration = db.collection("trips").document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TripRepository", "Trip listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                try {
                    trySend(snapshot?.toObject<Trip>()).isSuccess
                } catch (e: Exception) {
                    Log.e("TripRepository", "Error converting trip", e)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getPhotosStream(tripId: String): Flow<List<Photo>> = callbackFlow {
        val listenerRegistration = db.collection("trips").document(tripId).collection("photos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TripRepository", "Photos listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                try {
                    trySend(snapshot?.toObjects<Photo>() ?: emptyList()).isSuccess
                } catch (e: Exception) {
                    Log.e("TripRepository", "Error converting photos", e)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getExpensesStream(tripId: String): Flow<List<Expense>> = callbackFlow {
        val listenerRegistration = db.collection("trips").document(tripId).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("TripRepository", "Expenses listen failed.", error)
                    close(error)
                    return@addSnapshotListener
                }
                 try {
                    trySend(snapshot?.toObjects<Expense>() ?: emptyList()).isSuccess
                } catch (e: Exception) {
                    Log.e("TripRepository", "Error converting expenses", e)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun updatePlan(tripId: String, plan: String) {
        db.collection("trips").document(tripId).update("plan", plan).await()
    }

    suspend fun addExpense(tripId: String, expense: Expense) {
        db.collection("trips").document(tripId).collection("expenses").add(expense).await()
        db.collection("trips").document(tripId).update(
            "expenseCount", FieldValue.increment(1),
            "totalAmount", FieldValue.increment(expense.amount)
        ).await()
    }

    suspend fun deleteExpense(tripId: String, expenseId: String) {
        val expenseDoc = db.collection("trips").document(tripId).collection("expenses").document(expenseId).get().await()
        val expense = expenseDoc.toObject<Expense>()
        db.collection("trips").document(tripId).collection("expenses").document(expenseId).delete().await()
        if (expense != null) {
            db.collection("trips").document(tripId).update(
                "expenseCount", FieldValue.increment(-1),
                "totalAmount", FieldValue.increment(-expense.amount)
            ).await()
        }
    }

    suspend fun uploadImageAndAddPhoto(tripId: String, imageUri: Uri, uploadedBy: String, size: Long) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child("trips/$tripId/$fileName")
        storageRef.putFile(imageUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()
        val photo = Photo(url = downloadUrl, uploadedBy = uploadedBy, size = size)
        db.collection("trips").document(tripId).collection("photos").add(photo).await()
        db.collection("trips").document(tripId).update("photoCount", FieldValue.increment(1)).await()
    }

    suspend fun deletePhoto(tripId: String, photoId: String) {
        val photoDoc = db.collection("trips").document(tripId).collection("photos").document(photoId).get().await()
        val photo = photoDoc.toObject<Photo>()
        photo?.url?.let {
            try {
                storage.getReferenceFromUrl(it).delete().await()
            } catch (e: Exception) {
                Log.e("TripRepository", "Error deleting photo from storage", e)
            }
        }
        db.collection("trips").document(tripId).collection("photos").document(photoId).delete().await()
        db.collection("trips").document(tripId).update("photoCount", FieldValue.increment(-1)).await()
    }

    suspend fun addUserToTrip(tripId: String, userEmail: String) {
        db.collection("trips").document(tripId).update("users", FieldValue.arrayUnion(userEmail)).await()
    }

    suspend fun removeUserFromTrip(tripId: String, userEmail: String) {
        db.collection("trips").document(tripId).update("users", FieldValue.arrayRemove(userEmail)).await()
    }
}
