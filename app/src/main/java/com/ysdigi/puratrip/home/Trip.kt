package com.ysdigi.puratrip.home

import com.google.firebase.firestore.DocumentId

data class Trip(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val users: List<String> = emptyList(),
    val plan: String = "",
    val photoCount: Int = 0,
    val expenseCount: Int = 0,
    val totalAmount: Double = 0.0
)
