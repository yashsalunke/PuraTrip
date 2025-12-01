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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.min

class TripDetailsViewModel : ViewModel() {

    private val repository = TripRepository()

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState: StateFlow<TripDetailsUiState> = _uiState

    private val _uploadProgress = MutableStateFlow<UploadProgress?>(null)
    val uploadProgress: StateFlow<UploadProgress?> = _uploadProgress

    private val _selectedPhotos = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotos: StateFlow<Set<String>> = _selectedPhotos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun initialize(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val tripData = repository.getTripStream(tripId).first()
            val photos = repository.getPhotosStream(tripId).first()
            val expenses = repository.getExpensesStream(tripId).first()
            val userEmails = tripData?.users ?: emptyList()
            val userNames = repository.getUserNames(userEmails)
            val balances = calculateBalances(expenses, userEmails)
            val settlements = calculateSettlements(balances)
            _uiState.value = TripDetailsUiState(tripData, photos, expenses, balances, settlements, userNames)
            _isLoading.value = false

            combine(
                repository.getTripStream(tripId),
                repository.getPhotosStream(tripId),
                repository.getExpensesStream(tripId)
            ) { trip, photos, expenses ->
                val updatedUserEmails = trip?.users ?: emptyList()
                val updatedUserNames = repository.getUserNames(updatedUserEmails)
                val updatedBalances = calculateBalances(expenses, updatedUserEmails)
                val updatedSettlements = calculateSettlements(updatedBalances)
                TripDetailsUiState(trip, photos, expenses, updatedBalances, updatedSettlements, updatedUserNames)
            }.collect { _uiState.value = it }
        }
    }

    fun settleUp(tripId: String, from: String, to: String, amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addSettlement(tripId, from, to, amount)
            } finally {
                _isLoading.value = false
            }
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
            _isLoading.value = true
            try {
                _selectedPhotos.value.forEach {
                    repository.deletePhoto(tripId, it)
                }
                clearPhotoSelection()
            } finally {
                _isLoading.value = false
            }
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

            val amountToSettle = min(-debtorAmount, creditorAmount)

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
            _isLoading.value = true
            try {
                repository.updatePlan(tripId, plan)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExpense(tripId: String, expense: Expense) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addExpense(tripId, expense)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateExpense(tripId: String, expense: Expense) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateExpense(tripId, expense)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(tripId: String, expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteExpense(tripId, expenseId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadImagesAndAddPhotos(tripId: String, imageUris: List<Uri>, uploadedBy: String, context: Context) {
        viewModelScope.launch {
            _uploadProgress.value = UploadProgress(0, imageUris.size)
            _isLoading.value = true
            try {
                imageUris.forEachIndexed { index, uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        repository.uploadImageAndAddPhoto(tripId, bytes, uploadedBy)
                    }
                    _uploadProgress.value = UploadProgress(index + 1, imageUris.size)
                }
            } finally {
                _uploadProgress.value = null
                _isLoading.value = false
            }
        }
    }

    fun deletePhoto(tripId: String, photoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deletePhoto(tripId, photoId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addUser(tripId: String, userEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addUserToTrip(tripId, userEmail)
                // Refresh the user list
                val trip = repository.getTripStream(tripId).first()
                val userEmails = trip?.users ?: emptyList()
                val userNames = repository.getUserNames(userEmails)
                _uiState.value = _uiState.value.copy(
                    trip = trip,
                    userNames = userNames
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeUser(tripId: String, userEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.removeUserFromTrip(tripId, userEmail)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
