package com.ysdigi.puratrip.details

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.models.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    users: List<String>,
    userNames: Map<String, String>,
    onDismiss: () -> Unit,
    onAddExpense: (Expense) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var isAmountValid by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var isDescriptionValid by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var paidBy by remember { mutableStateOf(users.firstOrNull() ?: "") }
    val splitWith = remember { mutableStateListOf<String>() }
    val currentUser = Firebase.auth.currentUser

    fun validateAmount() {
        val amountValue = amount.toDoubleOrNull()
        isAmountValid = amountValue != null && amountValue > 0
    }

    fun validateDescription() {
        isDescriptionValid = description.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validateAmount()
                    },
                    label = { Text("Amount") },
                    isError = !isAmountValid,
                    supportingText = {
                        if (!isAmountValid) {
                            Text("Amount must be a number greater than 0")
                        }
                    }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        validateDescription()
                    },
                    label = { Text("Description") },
                    isError = !isDescriptionValid,
                    supportingText = {
                        if (!isDescriptionValid) {
                            Text("Description cannot be empty")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = userNames[paidBy] ?: paidBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid by") },
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
                        users.forEach { user ->
                            DropdownMenuItem(text = { Text(userNames[user] ?: user) }, onClick = {
                                paidBy = user
                                expanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Split with:")
                Column {
                    users.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = splitWith.contains(user), onCheckedChange = { isChecked ->
                                if (isChecked) splitWith.add(user) else splitWith.remove(user)
                            })
                            Text(userNames[user] ?: user)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                validateAmount()
                validateDescription()
                if (isAmountValid && isDescriptionValid) {
                    val expense = Expense(
                        amount = amount.toDouble(),
                        description = description,
                        paidBy = paidBy,
                        splitWith = splitWith,
                        timestamp = Date(),
                        createdBy = currentUser?.email ?: ""
                    )
                    onAddExpense(expense)
                }
            }) {
                Text("Add")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: Expense,
    users: List<String>,
    userNames: Map<String, String>,
    onDismiss: () -> Unit,
    onUpdateExpense: (Expense) -> Unit
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var isAmountValid by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf(expense.description) }
    var isDescriptionValid by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var paidBy by remember { mutableStateOf(expense.paidBy) }
    val splitWith = remember { mutableStateListOf<String>().also { it.addAll(expense.splitWith) } }

    fun validateAmount() {
        val amountValue = amount.toDoubleOrNull()
        isAmountValid = amountValue != null && amountValue > 0
    }

    fun validateDescription() {
        isDescriptionValid = description.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validateAmount()
                    },
                    label = { Text("Amount") },
                    isError = !isAmountValid,
                    supportingText = {
                        if (!isAmountValid) {
                            Text("Amount must be a number greater than 0")
                        }
                    }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        validateDescription()
                    },
                    label = { Text("Description") },
                    isError = !isDescriptionValid,
                    supportingText = {
                        if (!isDescriptionValid) {
                            Text("Description cannot be empty")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = userNames[paidBy] ?: paidBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid by") },
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
                        users.forEach { user ->
                            DropdownMenuItem(text = { Text(userNames[user] ?: user) }, onClick = {
                                paidBy = user
                                expanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Split with:")
                Column {
                    users.forEach { user ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = splitWith.contains(user), onCheckedChange = { isChecked ->
                                if (isChecked) splitWith.add(user) else splitWith.remove(user)
                            })
                            Text(userNames[user] ?: user)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                validateAmount()
                validateDescription()
                if (isAmountValid && isDescriptionValid) {
                    val updatedExpense = expense.copy(
                        amount = amount.toDouble(),
                        description = description,
                        paidBy = paidBy,
                        splitWith = splitWith
                    )
                    onUpdateExpense(updatedExpense)
                }
            }) {
                Text("Save")
            }
        }
    )
}

@Composable
fun ExpenseDetailsDialog(expense: Expense, onDismiss: () -> Unit, onEdit: () -> Unit, userNames: Map<String, String>, currency: String) {
    val currentUser = Firebase.auth.currentUser
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.description) },
        text = {
            Column {
                Text("Amount: ${getCurrencySymbol(currency)}${expense.amount}")
                Text("Paid by: ${userNames[expense.paidBy] ?: expense.paidBy}")
                Text("Added on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(expense.timestamp as Date)}")
                Text("Created by: ${userNames[expense.createdBy] ?: expense.createdBy}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Split with:", fontWeight = FontWeight.Bold)
                expense.splitWith.forEach {
                    Text(userNames[it] ?: it)
                }
            }
        },
        confirmButton = {
            Row {
                if (currentUser?.email == expense.createdBy) {
                    Button(onClick = onEdit) {
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}

@Composable
fun AddPhotoDialog(onDismiss: () -> Unit, onAddPhotos: (List<Uri>) -> Unit) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                onAddPhotos(uris)
            }
        }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            imageUri?.let { onAddPhotos(listOf(it)) }
        }
    }


    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                val newImageUri = createImageUri(context)
                imageUri = newImageUri
                cameraLauncher.launch(newImageUri)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Photo") },
        text = {
            Column {
                Button(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) {
                    Text("Select from Gallery")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Take a Photo")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun createImageUri(context: Context): Uri {
    val imageFile = File.createTempFile("JPEG_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    return FileProvider.getUriForFile(context, "com.ysdigi.puratrip.fileprovider", imageFile)
}

@Composable
fun ManageUsersDialog(
    users: List<String>,
    userNames: Map<String, String>,
    onDismiss: () -> Unit,
    onAddUser: (String) -> Unit,
    onRemoveUser: (String) -> Unit
) {
    var userEmail by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }

    fun validateEmail(email: String) {
        isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Users") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = {
                            userEmail = it
                            validateEmail(it)
                        },
                        label = { Text("User Email") },
                        modifier = Modifier.weight(1f),
                        isError = !isEmailValid,
                        supportingText = {
                            if (!isEmailValid) {
                                Text("Invalid email format")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (userEmail.isNotBlank() && isEmailValid) {
                                onAddUser(userEmail)
                                userEmail = ""
                                isEmailValid = true
                            }
                        },
                        enabled = userEmail.isNotBlank() && isEmailValid
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add User")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    items(users) { email ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(userNames[email] ?: email, modifier = Modifier.weight(1f).padding(vertical = 4.dp))
                            IconButton(onClick = { onRemoveUser(email) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove User")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
