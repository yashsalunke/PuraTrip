package com.ysdigi.puratrip.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ysdigi.puratrip.details.TripDetailsActivity
import com.ysdigi.puratrip.ui.theme.PuraTripTheme
import kotlinx.coroutines.launch

class HomeScreenActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra("email") ?: ""
        setContent {
            PuraTripTheme {
                HomeScreen(homeViewModel, email)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, email: String) {
    val trips by viewModel.trips.collectAsState()
    var showAddTripDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.listenForTrips(email)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Trips") }) },
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
                    TripItem(trip) {
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
                    if (!allUsers.contains(email)) {
                        allUsers.add(email)
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
fun TripItem(trip: Trip, onClick: () -> Unit) {
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
                    Icon(Icons.Default.AttachMoney, contentDescription = "Total Amount", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$%.2f".format(trip.totalAmount), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
