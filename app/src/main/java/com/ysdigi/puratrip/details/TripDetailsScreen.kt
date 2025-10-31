package com.ysdigi.puratrip.details

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.ysdigi.puratrip.models.Expense
import com.ysdigi.puratrip.models.Photo
import com.ysdigi.puratrip.models.Settlement
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // State for PlanScreen edit mode
    var isPlanEditMode by remember { mutableStateOf(false) }

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
                        1 -> Icons.Default.List
                        2 -> Icons.Default.Paid
                        else -> Icons.Default.Help
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
                0 -> PhotosScreen(
                    photos = uiState.photos,
                    selectedPhotos = selectedPhotos,
                    onPhotoClick = { viewModel.togglePhotoSelection(it.id) },
                )
                1 -> PlanScreen(
                    plan = uiState.trip?.plan ?: "",
                    onPlanChanged = { newPlan -> viewModel.updatePlan(tripId, newPlan) },
                    isEditMode = isPlanEditMode,
                    onEditModeChanged = { isPlanEditMode = it }
                )
                2 -> PaymentsScreen(
                    uiState = uiState,
                    onDeleteExpense = { expenseId -> viewModel.deleteExpense(tripId, expenseId) }
                )
            }
        }
    }

    if (showAddPhotoDialog) {
        val context = LocalContext.current
        AddPhotoDialog(
            onDismiss = { showAddPhotoDialog = false },
            onAddPhotos = { imageUris ->
                viewModel.uploadImagesAndAddPhotos(tripId, imageUris, userEmail, context)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(photos: List<Photo>, selectedPhotos: Set<String>, onPhotoClick: (Photo) -> Unit) {
    val context = LocalContext.current
    var sortBy by remember { mutableStateOf("Date") }
    var sortDescending by remember { mutableStateOf(true) }
    var groupByDate by remember { mutableStateOf(false) }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val sortedPhotos = remember(photos, sortBy, sortDescending) {
        val comparator = when (sortBy) {
            "Size" -> compareBy<Photo> { it.size }
            "Uploaded By" -> compareBy<Photo> { it.uploadedBy }
            else -> compareBy<Photo> { it.uploadedAt }
        }
        if (sortDescending) photos.sortedWith(comparator.reversed()) else photos.sortedWith(comparator)
    }

    val groupedPhotos = remember(sortedPhotos, groupByDate) {
        if (groupByDate) {
            sortedPhotos.groupBy {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(it.uploadedAt)
            }
        } else {
            emptyMap()
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(text = "Sort by: $sortBy")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Date", "Size", "Uploaded By").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = {
                            sortBy = it
                            expanded = false
                        })
                    }
                }
            }

            IconButton(onClick = { sortDescending = !sortDescending }) {
                Icon(if (sortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = "Sort Order")
            }

            TextButton(onClick = { groupByDate = !groupByDate }) {
                Text(if (groupByDate) "Ungroup" else "Group by Date")
            }

            TextButton(onClick = {
                sortBy = "Date"
                sortDescending = true
                groupByDate = false
            }) {
                Text("Clear")
            }
        }

        if (groupByDate) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedPhotos.forEach { (date, photosInGroup) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(photosInGroup) { photo ->
                        PhotoItem(photo, selectedPhotos, onPhotoClick, imageLoader)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedPhotos) { photo ->
                    PhotoItem(photo, selectedPhotos, onPhotoClick, imageLoader)
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photo: Photo, selectedPhotos: Set<String>, onPhotoClick: (Photo) -> Unit, imageLoader: ImageLoader) {
    Box(modifier = Modifier.clickable { onPhotoClick(photo) }) {
        AsyncImage(
            model = photo.url,
            contentDescription = "Photo by ${photo.uploadedBy}",
            imageLoader = imageLoader,
            modifier = Modifier.aspectRatio(1f),
            contentScale = ContentScale.Crop
        )
        if (selectedPhotos.contains(photo.id)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            if (photo.uploadedBy.isNotEmpty()) {
                Text(
                    text = "Uploaded by: ${photo.uploadedBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            Text(
                text = "Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(photo.uploadedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            val sizeInKb = photo.size / 1024
            val sizeText = if (sizeInKb > 1024) {
                val sizeInMb = sizeInKb / 1024f
                "%.2f MB".format(sizeInMb)
            } else {
                "$sizeInKb KB"
            }
            Text(
                text = "Size: $sizeText",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}

class WebAppInterface(private val onStylesChanged: (Set<String>) -> Unit) {
    @JavascriptInterface
    fun updateStyle(styles: String) {
        // styles will be a comma separated string e.g., "bold,italic"
        onStylesChanged(styles.split(',').filter { it.isNotBlank() }.toSet())
    }
}

@Composable
fun PlanScreen(
    plan: String,
    onPlanChanged: (String) -> Unit,
    isEditMode: Boolean,
    onEditModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showLinkDialog by remember { mutableStateOf(false) }
    var activeStyles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var displayedPlan by remember { mutableStateOf(plan) }

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
        }
    }

    LaunchedEffect(plan) {
        displayedPlan = plan
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            val escapedPlan = displayedPlan
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
            webView.evaluateJavascript("setContent(\"$escapedPlan\");", null)
            webView.post {
                webView.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
                webView.evaluateJavascript("focusEditor();", null)
            }
        }
    }

    if (isEditMode) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Toolbar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val boldModifier = if (activeStyles.contains("bold")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val italicModifier = if (activeStyles.contains("italic")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val underlineModifier = if (activeStyles.contains("underline")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val highlightModifier = if (activeStyles.contains("highlight")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier

                IconButton(onClick = { webView.evaluateJavascript("execCmd('bold');", null) }, modifier = boldModifier) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { webView.evaluateJavascript("execCmd('italic');", null) }, modifier = italicModifier) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { webView.evaluateJavascript("execCmd('underline');", null) }, modifier = underlineModifier) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                }
                IconButton(
                    onClick = {
                        val command = if (activeStyles.contains("highlight")) {
                            "execCmd('hiliteColor', 'transparent');"
                        } else {
                            "execCmd('hiliteColor', 'yellow');"
                        }
                        webView.evaluateJavascript(command, null)
                    },
                    modifier = highlightModifier
                ) {
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
                        addJavascriptInterface(WebAppInterface { activeStyles = it }, "Android")

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val escapedPlan = displayedPlan
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
                                var editor = document.getElementById('editor');

                                document.addEventListener('selectionchange', function() {
                                    var styles = [];
                                    if (document.queryCommandState('bold')) styles.push('bold');
                                    if (document.queryCommandState('italic')) styles.push('italic');
                                    if (document.queryCommandState('underline')) styles.push('underline');

                                    var highlightColor = document.queryCommandValue('hiliteColor');
                                    if (highlightColor && highlightColor.toLowerCase() !== 'transparent' && highlight.toLowerCase() !== 'rgba(0, 0, 0, 0)') {
                                        styles.push('highlight');
                                    }
                                    Android.updateStyle(styles.join(','));
                                });

                                function execCmd(command, value) {
                                    document.execCommand(command, false, value || null);
                                }

                                function setContent(newContent) {
                                    if (editor.innerHTML !== newContent) {
                                        editor.innerHTML = newContent;
                                    }
                                }

                                function getContent() {
                                    return editor.innerHTML;
                                }

                                function focusEditor() {
                                    editor.focus();
                                    var range = document.createRange();
                                    range.selectNodeContents(editor);
                                    range.collapse(false);
                                    var sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);
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
                        if (cleanedHtml != null) {
                            displayedPlan = cleanedHtml
                            onPlanChanged(cleanedHtml)
                        }
                        onEditModeChanged(false)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plan")
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable { onEditModeChanged(true) }
        ) {
            AndroidView(
                factory = {
                    TextView(it).apply {
                        text = Html.fromHtml(displayedPlan, Html.FROM_HTML_MODE_COMPACT)
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                },
                update = {
                    it.text = Html.fromHtml(displayedPlan, Html.FROM_HTML_MODE_COMPACT)
                },
                modifier = Modifier.fillMaxSize()
            )
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
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

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
                ExpenseItem(it, onDeleteExpense, onExpenseClick = { selectedExpense = it })
            }
        }
    }

    selectedExpense?.let {
        ExpenseDetailsDialog(expense = it, onDismiss = { selectedExpense = null })
    }
}

@Composable
fun DebtsToSettle(settlements: List<Settlement>) {
    Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Debts to Settle", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            settlements.forEach { settlement ->
                ListItem(
                    headlineContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = settlement.from,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "Owes",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = settlement.to,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        }
                    },
                    trailingContent = {
                        Text("%,.2f".format(settlement.amount), color = MaterialTheme.colorScheme.primary)
                    }
                )
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
fun ExpenseItem(expense: Expense, onDeleteExpense: (String) -> Unit, onExpenseClick: (Expense) -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick(expense) }
    ) {
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
fun ExpenseDetailsDialog(expense: Expense, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.description) },
        text = {
            Column {
                Text("Amount: ${expense.amount}")
                Text("Paid by: ${expense.paidBy}")
                Text("Added on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(expense.timestamp as Date)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Split with:", fontWeight = FontWeight.Bold)
                expense.splitWith.forEach {
                    Text(it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
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
                    splitWith = splitWith,
                    timestamp = Date()
                )
                onAddExpense(expense)
            }) {
                Text("Add")
            }
        }
    )
}
