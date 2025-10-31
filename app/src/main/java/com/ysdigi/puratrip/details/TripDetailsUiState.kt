package com.ysdigi.puratrip.details

import com.ysdigi.puratrip.home.Trip
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Photo

data class TripDetailsUiState(
    val trip: Trip? = null,
    val photos: List<Photo> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val balances: Map<String, Double> = emptyMap(),
    val settlements: List<Settlement> = emptyList()
)
