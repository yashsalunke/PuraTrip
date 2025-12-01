package com.ysdigi.puratrip.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import java.util.Currency

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        currencyCode // Fallback to the code if symbol not found
    }
}
