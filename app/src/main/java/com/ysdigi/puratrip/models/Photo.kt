package com.ysdigi.puratrip.models

import com.google.firebase.firestore.DocumentId

data class Photo(
    @DocumentId val id: String = "",
    val url: String = "",
    val uploadedBy: String = ""
)
