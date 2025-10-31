package com.ysdigi.puratrip.models

import com.google.firebase.firestore.DocumentId

data class Expense(
    @DocumentId val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val paidBy: String = "",
    val splitWith: List<String> = emptyList()
)
