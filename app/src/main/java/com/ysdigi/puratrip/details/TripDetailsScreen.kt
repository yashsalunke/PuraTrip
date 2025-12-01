package com.ysdigi.puratrip.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(viewModel: TripDetailsViewModel, tripId: String, userName: String) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Photos", "Plan", "Payments")
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current

    // State for PlanScreen edit mode
    var isPlanEditMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        val db = Firebase.firestore
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userProfile = document.toObject(User::class.java)
                    }
                }
        }
    }

    Box {
        Scaffold(
            topBar = {
                if (selectedPhotos.isNotEmpty()) {
                    PhotosSelectionBar(viewModel, tripId, selectedPhotos.size)
                } else {
                    Column {
                        TopAppBar(
                            title = { Text(uiState.trip?.name ?: "Trip Details") },
                            actions = {
                                IconButton(onClick = { showManageUsersDialog = true }) {
                                    Icon(Icons.Default.Group, contentDescription = "Manage Users")
                                }
                            }
                        )
                        uploadProgress?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { it.uploaded.toFloat() / it.total.toFloat() },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${it.uploaded} / ${it.total}")
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (selectedPhotos.isEmpty()) {
                    when (tabIndex) {
                        0 -> FloatingActionButton(onClick = { showAddPhotoDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Photo")
                        }
                        1 -> if (!isPlanEditMode) {
                            FloatingActionButton(onClick = { isPlanEditMode = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Plan")
                            }
                        }
                        2 -> FloatingActionButton(onClick = { showAddExpenseDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        val icon = when (index) {
                            0 -> Icons.Default.Photo
                            1 -> Icons.AutoMirrored.Filled.List
                            2 -> Icons.Default.Paid
                            else -> Icons.AutoMirrored.Filled.Help
                        }
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            content = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(icon, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(title)
                                }
                            }
                        )
                    }
                }
                when (tabIndex) {
                    0 -> if (uiState.photos.isEmpty()) {
                        EmptyStateMessage("No photos yet!\nTap the '+' button to add your first photo.")
                    } else {
                        PhotosScreen(
                            photos = uiState.photos,
                            selectedPhotos = selectedPhotos,
                            onPhotoClick = { viewModel.togglePhotoSelection(it.id) },
                            onDeletePhoto = { viewModel.deletePhoto(tripId, it) }
                        )
                    }
                    1 -> if (uiState.trip?.plan.isNullOrEmpty() && !isPlanEditMode) {
                        EmptyStateMessage("No plan yet!\nTap the 'Edit' button to create a plan.")
                    } else {
                        PlanScreen(
                            plan = uiState.trip?.plan ?: "",
                            onPlanChanged = { newPlan -> viewModel.updatePlan(tripId, newPlan) },
                            isEditMode = isPlanEditMode,
                            onEditModeChanged = { isPlanEditMode = it }
                        )
                    }
                    2 -> if (uiState.expenses.isEmpty()) {
                        EmptyStateMessage("No expenses yet!\nTap the '+' button to add your first expense.")
                    } else {
                        PaymentsScreen(
                            uiState = uiState,
                            onDeleteExpense = { expenseId -> viewModel.deleteExpense(tripId, expenseId) },
                            currency = userProfile?.currency ?: "INR",
                            onSettleUp = { from, to, amount -> viewModel.settleUp(tripId, from, to, amount) },
                            onUpdateExpense = { viewModel.updateExpense(tripId, it) }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            LoadingOverlay()
        }
    }

    if (showAddPhotoDialog) {
        AddPhotoDialog(
            onDismiss = { showAddPhotoDialog = false },
            onAddPhotos = { imageUris ->
                viewModel.uploadImagesAndAddPhotos(tripId, imageUris, userName, context)
                showAddPhotoDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            users = uiState.trip?.users ?: emptyList(),
            userNames = uiState.userNames,
            onDismiss = { showAddExpenseDialog = false },
            onAddExpense = {
                viewModel.addExpense(tripId, it)
                showAddExpenseDialog = false
            }
        )
    }

    if (showManageUsersDialog) {
        ManageUsersDialog(
            users = uiState.trip?.users ?: emptyList(),
            userNames = uiState.userNames,
            onDismiss = { showManageUsersDialog = false },
            onAddUser = { email -> viewModel.addUser(tripId, email) },
            onRemoveUser = { email -> viewModel.removeUser(tripId, email) }
        )
    }
}
