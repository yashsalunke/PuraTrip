package com.ysdigi.puratrip.details

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Photo
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(viewModel: TripDetailsViewModel, tripId: String, userEmail: String) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Photos", "Plan", "Payments")
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    var showAddPhotoDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showManageUsersDialog by remember { mutableStateOf(false) }

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
                                progress = it.uploaded.toFloat() / it.total.toFloat(),
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
            if ((tabIndex == 0 || tabIndex == 2) && selectedPhotos.isEmpty()) { // Photos or Payments
                FloatingActionButton(onClick = {
                    if (tabIndex == 0) {
                        showAddPhotoDialog = true
                    } else {
                        showAddExpenseDialog = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index })
                }
            }
            when (tabIndex) {
                0 -> PhotosScreen(
                    photos = uiState.photos,
                    selectedPhotos = selectedPhotos,
                    onPhotoClick = { viewModel.togglePhotoSelection(it.id) },
                )
                1 -> PlanScreen(
                    plan = uiState.trip?.plan ?: "",
                    onPlanChanged = { newPlan -> viewModel.updatePlan(tripId, newPlan) }
                )
                2 -> PaymentsScreen(
                    uiState = uiState,
                    onDeleteExpense = { expenseId -> viewModel.deleteExpense(tripId, expenseId) }
                )
            }
        }
    }

    if (showAddPhotoDialog) {
        AddPhotoDialog(
            onDismiss = { showAddPhotoDialog = false },
            onAddPhotos = { imageUris ->
                viewModel.uploadImagesAndAddPhotos(tripId, imageUris, userEmail)
                showAddPhotoDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            users = uiState.trip?.users ?: emptyList(),
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
            onDismiss = { showManageUsersDialog = false },
            onAddUser = { email -> viewModel.addUser(tripId, email) },
            onRemoveUser = { email -> viewModel.removeUser(tripId, email) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosSelectionBar(viewModel: TripDetailsViewModel, tripId: String, selectedCount: Int) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()

    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = { viewModel.clearPhotoSelection() }) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = { viewModel.selectAllPhotos() }) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
            }
            IconButton(onClick = {
                downloadPhotos(
                    context,
                    uiState.photos.filter { selectedPhotos.contains(it.id) })
            }) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
            IconButton(onClick = { viewModel.deleteSelectedPhotos(tripId) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    )
}

fun downloadPhotos(context: Context, photos: List<Photo>) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    photos.forEach { photo ->
        val request = DownloadManager.Request(Uri.parse(photo.url))
            .setTitle("PuraTrip Photo")
            .setDescription("Downloading photo...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "PuraTrip_${System.currentTimeMillis()}.jpg"
            )
        downloadManager.enqueue(request)
    }
}

@Composable
fun ManageUsersDialog(
    users: List<String>,
    onDismiss: () -> Unit,
    onAddUser: (String) -> Unit,
    onRemoveUser: (String) -> Unit
) {
    var userEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Users") },
        text = {
            Column {
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
                            onAddUser(userEmail)
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

@Composable
fun PhotosScreen(photos: List<Photo>, selectedPhotos: Set<String>, onPhotoClick: (Photo) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos) { photo ->
            Box(modifier = Modifier.clickable { onPhotoClick(photo) }) {
                AsyncImage(
                    model = photo.url,
                    contentDescription = "Photo by ${photo.uploadedBy}",
                    modifier = Modifier.aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                if (selectedPhotos.contains(photo.id)) {
                    Box(modifier = Modifier.matchParentSize().align(Alignment.Center)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanScreen(plan: String, onPlanChanged: (String) -> Unit) {
    val context = LocalContext.current
    var showLinkDialog by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // --- Toolbar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { webView.evaluateJavascript("execCmd('bold');", null) }) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
            }
            IconButton(onClick = { webView.evaluateJavascript("execCmd('italic');", null) }) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
            }
            IconButton(onClick = { webView.evaluateJavascript("execCmd('underline');", null) }) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
            }
            IconButton(onClick = { webView.evaluateJavascript("execCmd('hiliteColor', 'yellow');", null) }) {
                Icon(Icons.Default.Highlight, contentDescription = "Highlight")
            }
            IconButton(onClick = { showLinkDialog = true }) {
                Icon(Icons.Default.Link, contentDescription = "Hyperlink")
            }

            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.FormatSize, contentDescription = "Font Size")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    (1..7).forEach { size ->
                        DropdownMenuItem(
                            text = { Text("Size $size") },
                            onClick = {
                                webView.evaluateJavascript("execCmd('fontSize', '$size');", null)
                                expanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = { webView.evaluateJavascript("execCmd('removeFormat');", null) }) {
                Icon(Icons.Default.FormatClear, contentDescription = "Clear Formatting")
            }
        }

        // --- WebView editor ---
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val escapedPlan = plan
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                            view?.evaluateJavascript("setContent(\"$escapedPlan\");", null)
                        }
                    }

                    val editorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <style>
                                body { margin: 0; padding: 0; font-family: sans-serif; }
                                #editor {
                                    height: 100vh;
                                    padding: 8px;
                                    outline: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div id="editor" contenteditable="true"></div>
                            <script>
                                var savedRange;
                                var editor = document.getElementById('editor');

                                editor.addEventListener('focus', function() {
                                    restoreSelection();
                                });

                                editor.addEventListener('blur', function() {
                                    saveSelection();
                                });

                                editor.addEventListener('mouseup', saveSelection);
                                editor.addEventListener('keyup', saveSelection);

                                function saveSelection() {
                                    var sel = window.getSelection();
                                    if (sel.rangeCount > 0) {
                                        savedRange = sel.getRangeAt(0);
                                    }
                                }

                                function restoreSelection() {
                                    var sel = window.getSelection();
                                    if (savedRange) {
                                        sel.removeAllRanges();
                                        sel.addRange(savedRange);
                                    }
                                }

                                function execCmd(command, value) {
                                    restoreSelection();
                                    document.execCommand(command, false, value || null);
                                    saveSelection();
                                }

                                function setContent(newContent) {
                                    if (editor.innerHTML !== newContent) {
                                        editor.innerHTML = newContent;
                                    }
                                }

                                function getContent() {
                                    return editor.innerHTML;
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()

                    loadDataWithBaseURL(null, editorHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outline)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                webView.evaluateJavascript("getContent()") { html ->
                    val cleanedHtml = html
                        ?.removePrefix("\"")
                        ?.removeSuffix("\"")
                        ?.replace("\\u003C", "<")
                        ?.replace("\\\"", "\"")
                        ?.replace("\\n", "")
                        ?.replace("\\r", "")
                        ?.replace("\\\'", "'")
                    onPlanChanged(cleanedHtml ?: "")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Plan")
        }
    }

    // --- Add Link Dialog ---
    if (showLinkDialog) {
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Add Hyperlink") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    webView.evaluateJavascript("execCmd('createLink', '$url');", null)
                    showLinkDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun PaymentsScreen(uiState: TripDetailsUiState, onDeleteExpense: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        BalanceSummary(uiState.balances)
        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.settlements.isNotEmpty()) {
            DebtsToSettle(uiState.settlements)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text("Expenses", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.expenses) {
                ExpenseItem(it, onDeleteExpense)
            }
        }
    }
}

@Composable
fun DebtsToSettle(settlements: List<Settlement>) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Debts to Settle", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            settlements.forEach { settlement ->
                Text("${settlement.from} owes ${settlement.to} %.2f".format(settlement.amount))
            }
        }
    }
}

@Composable
fun BalanceSummary(balances: Map<String, Double>) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Balances", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            balances.forEach { (user, balance) ->
                val color = if (balance >= 0) Color(0xFF006400) else Color.Red // Darker Green
                Text("$user: %.2f".format(balance), color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, onDeleteExpense: (String) -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold)
                Text("Amount: ${expense.amount}")
                Text("Paid by: ${expense.paidBy}")
            }
            IconButton(onClick = { onDeleteExpense(expense.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Expense")
            }
        }
    }
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
                Button(onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
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
    val imageFile = File.createTempFile("JPEG_", ".jpg", context.getExternalFilesDir("Pictures"))
    return FileProvider.getUriForFile(context, "com.ysdigi.puratrip.fileprovider", imageFile)
}

@Composable
fun AddExpenseDialog(users: List<String>, onDismiss: () -> Unit, onAddExpense: (Expense) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var paidBy by remember { mutableStateOf(users.firstOrNull() ?: "") }
    val splitWith = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(8.dp))

                Text("Paid by:")
                Box {
                    Text(paidBy, modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(8.dp))
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        users.forEach { user ->
                            DropdownMenuItem(text = { Text(user) }, onClick = {
                                paidBy = user
                                expanded = false
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Split with:")
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(users) {
                            user ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = splitWith.contains(user), onCheckedChange = { isChecked ->
                                if (isChecked) splitWith.add(user) else splitWith.remove(user)
                            })
                            Text(user)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val expense = Expense(
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    description = description,
                    paidBy = paidBy,
                    splitWith = splitWith
                )
                onAddExpense(expense)
            }) {
                Text("Add")
            }
        }
    )
}
