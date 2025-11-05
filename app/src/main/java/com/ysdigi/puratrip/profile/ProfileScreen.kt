package com.ysdigi.puratrip.profile

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.login.LoginActivity
import com.ysdigi.puratrip.models.User
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val user = Firebase.auth.currentUser
    val db = Firebase.firestore
    var userProfile by remember { mutableStateOf<User?>(null) }
    var newName by remember { mutableStateOf("") }
    var newCurrency by remember { mutableStateOf("USD") }
    val currencies = listOf("USD", "EUR", "GBP", "JPY", "INR")
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = (context as? Activity)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userProfile = document.toObject(User::class.java)
                        newName = userProfile?.name ?: ""
                        newCurrency = userProfile?.currency ?: "USD"
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        backDispatcher?.onBackPressed()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f)
                )
                if (user?.isEmailVerified == true) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color.Green,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "${getCurrencySymbol(newCurrency)} - $newCurrency",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    currencies.forEach { currencyCode ->
                        DropdownMenuItem(
                            text = { Text("${getCurrencySymbol(currencyCode)} - $currencyCode") },
                            onClick = {
                                newCurrency = currencyCode
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (user != null) {
                        val updatedUser = User(
                            name = newName,
                            email = userProfile?.email ?: "",
                            currency = newCurrency,
                            verified = user.isEmailVerified
                        )
                        db.collection("users").document(user.uid).set(updatedUser)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                activity?.finish()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            if (user?.isEmailVerified == false) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        user.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Verification email sent.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resend Verification Email")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    user?.email?.let { email ->
                        Firebase.auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Firebase.auth.signOut()
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    activity?.finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (e: Exception) {
        currencyCode // Fallback to the code if symbol not found
    }
}
