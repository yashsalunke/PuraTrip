package com.ysdigi.puratrip.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ysdigi.puratrip.home.HomeScreenActivity
import com.ysdigi.puratrip.ui.theme.PuraTripTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("PuraTripPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("email", null)

        if (savedEmail != null) {
            val intent = Intent(this, HomeScreenActivity::class.java)
            intent.putExtra("email", savedEmail)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            PuraTripTheme {
                LoginScreen { email ->
                    sharedPreferences.edit().putString("email", email).apply()
                    val intent = Intent(this, HomeScreenActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                if (android.util.Patterns.EMAIL_ADDRESS.matcher(email.text).matches()) {
                    onLoginSuccess(email.text)
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Login")
        }
    }
}
