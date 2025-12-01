package com.ysdigi.puratrip.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val paidBy: String = "",
    val splitWith: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Date? = null,
    val isSettlement: Boolean = false,
    val createdBy: String = ""
)
