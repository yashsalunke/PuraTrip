package com.ysdigi.puratrip.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Expense(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val paidBy: String = "",
    val splitWith: List<String> = emptyList(),
    val timestamp: Date = Date()
)
