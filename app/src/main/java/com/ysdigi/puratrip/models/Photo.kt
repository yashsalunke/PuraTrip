package com.ysdigi.puratrip.models

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Photo(
    @DocumentId val id: String = "",
    val url: String = "",
    val uploadedBy: String = "",
    val size: Long = 0,
    val uploadedAt: Date = Date()
)
