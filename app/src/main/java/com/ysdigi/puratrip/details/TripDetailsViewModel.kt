package com.ysdigi.puratrip.details

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Settlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.InputStream

class TripDetailsViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState: StateFlow<TripDetailsUiState> = _uiState

    private val _uploadProgress = MutableStateFlow<UploadProgress?>(null)
    val uploadProgress: StateFlow<UploadProgress?> = _uploadProgress

    private val _selectedPhotos = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotos: StateFlow<Set<String>> = _selectedPhotos

    fun initialize(tripId: String) {
        viewModelScope.launch {
            combine(
                repository.getTripStream(tripId),
                repository.getPhotosStream(tripId),
                repository.getExpensesStream(tripId)
            ) { trip, photos, expenses ->
                val balances = calculateBalances(expenses, trip?.users ?: emptyList())
                val settlements = calculateSettlements(balances)
                TripDetailsUiState(trip, photos, expenses, balances, settlements)
            }.collect { _uiState.value = it }
        }
    }

    fun togglePhotoSelection(photoId: String) {
        val currentSelection = _selectedPhotos.value.toMutableSet()
        if (currentSelection.contains(photoId)) {
            currentSelection.remove(photoId)
        } else {
            currentSelection.add(photoId)
        }
        _selectedPhotos.value = currentSelection
    }

    fun selectAllPhotos() {
        _selectedPhotos.value = _uiState.value.photos.map { it.id }.toSet()
    }

    fun clearPhotoSelection() {
        _selectedPhotos.value = emptySet()
    }

    fun deleteSelectedPhotos(tripId: String) {
        viewModelScope.launch {
            _selectedPhotos.value.forEach {
                repository.deletePhoto(tripId, it)
            }
            clearPhotoSelection()
        }
    }

    private fun calculateBalances(expenses: List<Expense>, users: List<String>): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        users.forEach { balances[it] = 0.0 }

        expenses.forEach { expense ->
            val amountPaid = expense.amount
            val paidBy = expense.paidBy
            val splitAmong = expense.splitWith
            if (splitAmong.isNotEmpty()) {
                val share = amountPaid / splitAmong.size
                balances[paidBy] = (balances[paidBy] ?: 0.0) + amountPaid
                splitAmong.forEach { user ->
                    balances[user] = (balances[user] ?: 0.0) - share
                }
            }
        }
        return balances
    }

    private fun calculateSettlements(balances: Map<String, Double>): List<Settlement> {
        val debtors = balances.filter { it.value < 0 }.toMutableMap()
        val creditors = balances.filter { it.value > 0 }.toMutableMap()
        val settlements = mutableListOf<Settlement>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtor, debtorAmount) = debtors.entries.first()
            val (creditor, creditorAmount) = creditors.entries.first()

            val amountToSettle = minOf(-debtorAmount, creditorAmount)

            settlements.add(Settlement(from = debtor, to = creditor, amount = amountToSettle))

            val newDebtorAmount = debtorAmount + amountToSettle
            if (newDebtorAmount > -0.01) { // Use a small epsilon for float comparison
                debtors.remove(debtor)
            } else {
                debtors[debtor] = newDebtorAmount
            }

            val newCreditorAmount = creditorAmount - amountToSettle
            if (newCreditorAmount < 0.01) { // Use a small epsilon for float comparison
                creditors.remove(creditor)
            } else {
                creditors[creditor] = newCreditorAmount
            }
        }
        return settlements
    }

    fun updatePlan(tripId: String, plan: String) {
        viewModelScope.launch {
            repository.updatePlan(tripId, plan)
        }
    }

    fun addExpense(tripId: String, expense: Expense) {
        viewModelScope.launch {
            repository.addExpense(tripId, expense)
        }
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        viewModelScope.launch {
            repository.deleteExpense(tripId, expenseId)
        }
    }

    fun uploadImagesAndAddPhotos(tripId: String, imageUris: List<Uri>, uploadedBy: String, context: Context) {
        viewModelScope.launch {
            _uploadProgress.value = UploadProgress(0, imageUris.size)
            try {
                imageUris.forEachIndexed { index, uri ->
                    val size = context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
                    repository.uploadImageAndAddPhoto(tripId, uri, uploadedBy, size)
                    _uploadProgress.value = UploadProgress(index + 1, imageUris.size)
                }
            } finally {
                _uploadProgress.value = null
            }
        }
    }

    fun deletePhoto(tripId: String, photoId: String) {
        viewModelScope.launch {
            repository.deletePhoto(tripId, photoId)
        }
    }

    fun addUser(tripId: String, userEmail: String) {
        viewModelScope.launch {
            repository.addUserToTrip(tripId, userEmail)
        }
    }

    fun removeUser(tripId: String, userEmail: String) {
        viewModelScope.launch {
            repository.removeUserFromTrip(tripId, userEmail)
        }
    }
}
