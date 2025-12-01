package com.ysdigi.puratrip.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.R
import com.ysdigi.puratrip.details.TripDetailsActivity
import com.ysdigi.puratrip.login.LoginActivity
import com.ysdigi.puratrip.models.User
import com.ysdigi.puratrip.profile.ProfileActivity
import com.ysdigi.puratrip.ui.theme.PuraTripTheme
import kotlinx.coroutines.launch
import java.util.Currency

class HomeScreenActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private val userName = mutableStateOf("")
    private val userCurrency = mutableStateOf("USD")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        setContent {
            PuraTripTheme {
                HomeScreen(homeViewModel, userName.value, userCurrency.value, onLogout = {
                    Firebase.auth.signOut()
                    googleSignInClient.signOut().addOnCompleteListener {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val user = Firebase.auth.currentUser
        val db = Firebase.firestore
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userProfile = document.toObject(User::class.java)
                        userName.value = userProfile?.name ?: user.email ?: ""
                        userCurrency.value = userProfile?.currency ?: "USD"
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, userName: String, userCurrency: String, onLogout: () -> Unit) {
    val trips by viewModel.trips.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddTripDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val user = Firebase.auth.currentUser

    LaunchedEffect(Unit) {
        if (user != null) {
            viewModel.listenForTrips(user.email ?: "")
        }
    }

    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Welcome, $userName") },
                    actions = {
                        IconButton(onClick = {
                            context.startActivity(Intent(context, ProfileActivity::class.java))
                        }) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddTripDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Trip")
                }
            }
        ) { padding ->
            if (trips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No trips yet!\nClick the ‘+’ button to create one.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trips) { trip ->
                        TripItem(trip, userCurrency) {
                            val intent = Intent(context, TripDetailsActivity::class.java)
                            intent.putExtra("tripId", trip.id)
                            context.startActivity(intent)
                        }
                    }
                }
            }

            if (showAddTripDialog) {
                AddTripDialog(
                    onDismiss = { showAddTripDialog = false },
                    onAddTrip = { tripName, users ->
                        showAddTripDialog = false // Close the dialog immediately
                        val allUsers = users.toMutableList()
                        if (user != null && !allUsers.contains(user.email)) {
                            allUsers.add(user.email ?: "")
                        }
                        scope.launch {
                            val success = viewModel.addTrip(tripName, allUsers)
                            if (!success) {
                                snackbarHostState.showSnackbar("Failed to create trip. Please try again.")
                            }
                        }
                    }
                )
            }
        }

        if (isLoading) {
            LoadingOverlay()
        }
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

@Composable
fun AddTripDialog(onDismiss: () -> Unit, onAddTrip: (String, List<String>) -> Unit) {
    var tripName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    val users = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a new trip") },
        text = {
            Column {
                OutlinedTextField(
                    value = tripName,
                    onValueChange = { tripName = it },
                    label = { Text("Trip Name") },
                    isError = tripName.isBlank()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("User Email") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (userEmail.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                            users.add(userEmail)
                            userEmail = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add User")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(users) { email ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(email, modifier = Modifier.weight(1f).padding(vertical = 4.dp))
                            IconButton(onClick = { users.remove(email) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove User")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddTrip(tripName, users) },
                enabled = tripName.isNotBlank()
            ) {
                Text("Create Trip")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TripItem(trip: Trip, currency: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = trip.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Users", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${trip.users.size} users", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Photos", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${trip.photoCount} photos", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = "Expenses", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${trip.expenseCount} expenses", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Total Amount", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${getCurrencySymbol(currency)}%.2f".format(trip.totalAmount), style = MaterialTheme.typography.bodyMedium)
                }
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
