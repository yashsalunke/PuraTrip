package com.ysdigi.puratrip.details

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ysdigi.puratrip.models.Photo
import java.text.SimpleDateFormat
import java.util.*

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
                android.os.Environment.DIRECTORY_DOWNLOADS,
                "PuraTrip_${System.currentTimeMillis()}.jpg"
            )
        downloadManager.enqueue(request)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    photos: List<Photo>,
    selectedPhotos: Set<String>,
    onPhotoClick: (Photo) -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val context = LocalContext.current
    var sortBy by remember { mutableStateOf("Date") }
    var sortDescending by remember { mutableStateOf(true) }
    var groupBy by remember { mutableStateOf("None") }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }

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

    val groupedPhotos = remember(sortedPhotos, groupBy) {
        when (groupBy) {
            "Date" -> sortedPhotos.groupBy {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(it.uploadedAt)
            }
            "Uploaded By" -> sortedPhotos.groupBy { it.uploadedBy }
            else -> emptyMap()
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

            var groupExpanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { groupExpanded = true }) {
                    Text(if (groupBy == "None") "Group by" else "Grouped by $groupBy")
                }
                DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    listOf("None", "Date", "Uploaded By").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = {
                            groupBy = it
                            groupExpanded = false
                        })
                    }
                }
            }

            TextButton(onClick = {
                sortBy = "Date"
                sortDescending = true
                groupBy = "None"
            }) {
                Text("Clear")
            }
        }

        if (groupBy != "None") {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedPhotos.forEach { (group, photosInGroup) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(photosInGroup) { photo ->
                        PhotoItem(photo, selectedPhotos, { selectedPhoto = it }, onPhotoClick, imageLoader)
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
                    PhotoItem(photo, selectedPhotos, { selectedPhoto = it }, onPhotoClick, imageLoader)
                }
            }
        }
    }

    selectedPhoto?.let { photo ->
        PhotoViewerDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null },
            onDelete = {
                onDeletePhoto(photo.id)
                selectedPhoto = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoItem(
    photo: Photo,
    selectedPhotos: Set<String>,
    onPhotoTap: (Photo) -> Unit,
    onPhotoLongPress: (Photo) -> Unit,
    imageLoader: ImageLoader
) {
    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { onPhotoTap(photo) },
                onLongPress = { onPhotoLongPress(photo) }
            )
        }
    ) {
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
    }
}

@Composable
fun PhotoViewerDialog(photo: Photo, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val currentUser = Firebase.auth.currentUser
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            AsyncImage(
                model = photo.url,
                contentDescription = "Full screen photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                if (currentUser?.email == photo.uploadedBy) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete photo", tint = Color.White)
                    }
                }
            }
        }
    }
}
