package com.ysdigi.puratrip.models

data class User(
    val name: String = "",
    val email: String = "",
    val currency: String = "INR",
    val verified: Boolean = false
)
