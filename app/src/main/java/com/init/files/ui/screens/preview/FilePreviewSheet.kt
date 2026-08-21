package com.init.files.ui.screens.preview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.formatByteSize
import com.init.files.theme.JetBrainsMonoFontFamily
import com.init.files.theme.MichromaFontFamily
import com.init.files.theme.SignalAccent
import com.init.files.ui.components.InitBadge
import com.init.files.ui.components.InitButton
import com.init.files.ui.components.InitCard
import com.init.files.ui.components.InitSectionHeader
import com.init.files.ui.components.InitTopBar
import com.init.files.ui.components.InitVideoLoading
import com.init.files.ui.screens.browse.shareSelectedFiles
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FilePreviewSheet(
    fileItem: FileItem,
    viewModel: PreviewViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(fileItem) {
        viewModel.loadFile(context, fileItem)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    Scaffold(
        topBar = {
            InitTopBar(
                title = when {
                    state.isPdf -> "pdf reader"
                    fileItem.category == FileCategory.IMAGES -> "image viewer"
                    fileItem.category == FileCategory.VIDEOS -> "video player"
                    fileItem.category == FileCategory.AUDIO -> "audio player"
                    fileItem.category == FileCategory.DOCUMENTS -> "doc viewer"
                    else -> "file preview"
                },
                subtitle = fileItem.name,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDetailsSheet() }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = if (state.showDetailsSheet) SignalAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { shareSelectedFiles(context, listOf(fileItem)) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { openWithSystem(context, fileItem) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in External App", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Reader Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        // 1. PDF Reader
                        state.isPdf -> {
                            BuiltInPdfReader(
                                pages = state.pdfPages,
                                pageCount = state.pdfPageCount,
                                isLoading = state.isPdfLoading
                            )
                        }

                        // 2. Video Player
                        fileItem.category == FileCategory.VIDEOS -> {
                            BuiltInVideoPlayer(
                                state = state,
                                viewModel = viewModel
                            )
                        }

                        // 3. Image Viewer
                        fileItem.category == FileCategory.IMAGES -> {
                            BuiltInImageViewer(
                                file = File(fileItem.path),
                                state = state,
                                onRotate = { viewModel.rotateImage() }
                            )
                        }

                        // 4. Audio Player
                        fileItem.category == FileCategory.AUDIO -> {
                            BuiltInAudioPlayer(
                                item = fileItem,
                                state = state,
                                viewModel = viewModel
                            )
                        }

                        // 5. Document / Code Viewer
                        fileItem.category == FileCategory.DOCUMENTS || state.textContent != null -> {
                            BuiltInDocReader(
                                textContent = state.textContent,
                                isLoading = state.isTextLoading,
                                lineCount = state.textLineCount,
                                wordWrap = state.wordWrap,
                                onToggleWordWrap = { viewModel.toggleWordWrap() }
                            )
                        }

                        // 6. Generic Binary / APK Viewer
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FileOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = fileItem.name,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${fileItem.formattedSize} • ${fileItem.extension.uppercase()}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Collapsible Technical Metadata Sheet
            AnimatedVisibility(
                visible = state.showDetailsSheet,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "file attributes",
                                fontFamily = MichromaFontFamily,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { viewModel.toggleDetailsSheet() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        TechnicalAttributesCard(fileItem = fileItem, state = state)
                    }
                }
            }
        }
    }
}

/**
 * Built-in PDF Reader with continuous scroll and page counter.
 */
@Composable
fun BuiltInPdfReader(
    pages: List<Bitmap>,
    pageCount: Int,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InitVideoLoading(size = 72.dp, label = "RENDERING PDF PAGES...")
        }
    } else if (pages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "NO PAGES RENDERED",
                    fontFamily = MichromaFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        val listState = rememberLazyListState()
        val firstVisibleIndex by remember {
            derivedStateOf { listState.firstVisibleItemIndex + 1 }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(pages) { index, bmp ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Page ${index + 1}",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            // Floating Page Counter Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "page $firstVisibleIndex of $pageCount",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Built-in Video Player with custom Nothing OS playback controls.
 */
@OptIn(UnstableApi::class)
@Composable
fun BuiltInVideoPlayer(
    state: PreviewUiState,
    viewModel: PreviewViewModel
) {
    val player = viewModel.getPlayer()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Video View Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (player != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = state.resizeMode
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view ->
                        view.player = player
                        view.resizeMode = state.resizeMode
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (state.isBuffering) {
                InitVideoLoading(size = 64.dp, label = "BUFFERING...")
            }
        }

        // Custom Nothing OS Player Controls Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Scrubber Slider
                val maxDur = state.durationMs.coerceAtLeast(1L).toFloat()
                val currentPos = state.currentPositionMs.coerceIn(0L, state.durationMs).toFloat()

                Slider(
                    value = currentPos,
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..maxDur,
                    colors = SliderDefaults.colors(
                        thumbColor = SignalAccent,
                        activeTrackColor = SignalAccent,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Timestamp & Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatDuration(state.currentPositionMs)} / ${formatDuration(state.durationMs)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.skip(-10000L) }) {
                            Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.togglePlayPause() },
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.skip(10000L) }) {
                            Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        IconButton(onClick = { viewModel.toggleResizeMode() }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Resize Mode", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Built-in High-Res Image Viewer with zoom, pan, and rotate.
 */
@Composable
fun BuiltInImageViewer(
    file: File,
    state: PreviewUiState,
    onRotate: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = file,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                    rotationZ = state.imageRotation
                )
        )

        // Overlay Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.imageWidth > 0 && state.imageHeight > 0) {
                    Text(
                        text = "${state.imageWidth}×${state.imageHeight}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Built-in Audio Player with scrubber and time indicators.
 */
@Composable
fun BuiltInAudioPlayer(
    item: FileItem,
    state: PreviewUiState,
    viewModel: PreviewViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = item.name,
            fontFamily = MichromaFontFamily,
            fontSize = 14.sp,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${formatDuration(state.currentPositionMs)} / ${formatDuration(state.durationMs)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        val maxDur = state.durationMs.coerceAtLeast(1L).toFloat()
        val currentPos = state.currentPositionMs.coerceIn(0L, state.durationMs).toFloat()

        Slider(
            value = currentPos,
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..maxDur,
            colors = SliderDefaults.colors(
                thumbColor = SignalAccent,
                activeTrackColor = SignalAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.skip(-10000L) }) {
                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = MaterialTheme.colorScheme.onSurface)
            }

            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { viewModel.togglePlayPause() },
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            IconButton(onClick = { viewModel.skip(10000L) }) {
                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Built-in Document & Code Reader with numbered gutter and word-wrap toggle.
 */
@Composable
fun BuiltInDocReader(
    textContent: String?,
    isLoading: Boolean,
    lineCount: Int,
    wordWrap: Boolean,
    onToggleWordWrap: () -> Unit
) {
    val context = LocalContext.current

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InitVideoLoading(size = 72.dp, label = "LOADING DOCUMENT CONTENT...")
        }
    } else if (textContent == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unable to render document preview.", fontFamily = JetBrainsMonoFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Document toolbar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$lineCount lines",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onToggleWordWrap, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.WrapText,
                                contentDescription = "Word Wrap",
                                tint = if (wordWrap) SignalAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Document Content", textContent))
                                Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Code Content with Gutter
            val lines = textContent.lines()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!wordWrap) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                contentPadding = PaddingValues(12.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = String.format("%4d ", index + 1),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = line,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview / Document adapter for backward compatibility.
 */
@Composable
fun CodeTextPreview(
    textContent: String?,
    isLoading: Boolean
) {
    BuiltInDocReader(
        textContent = textContent,
        isLoading = isLoading,
        lineCount = textContent?.lines()?.size ?: 0,
        wordWrap = false,
        onToggleWordWrap = {}
    )
}

/**
 * Technical attributes card (Checksums, APK metadata, permissions).
 */
@Composable
fun TechnicalAttributesCard(
    fileItem: FileItem,
    state: PreviewUiState
) {
    val file = File(fileItem.path)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailAttributeRow("PATH", fileItem.path)
        DetailAttributeRow("SIZE", "${formatByteSize(fileItem.sizeBytes)} (${fileItem.sizeBytes} bytes)")
        DetailAttributeRow("LAST MODIFIED", dateFormat.format(Date(fileItem.lastModified)))
        DetailAttributeRow(
            "PERMISSIONS",
            "${if (file.canRead()) "r" else "-"}${if (file.canWrite()) "w" else "-"}${if (file.canExecute()) "x" else "-"}"
        )
        DetailAttributeRow("SHA-256", state.sha256Hash)
        DetailAttributeRow("MD5", state.md5Hash)

        if (state.apkMetadata != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            DetailAttributeRow("PACKAGE NAME", state.apkMetadata.packageName)
            DetailAttributeRow("VERSION", "${state.apkMetadata.versionName} (${state.apkMetadata.versionCode})")
            DetailAttributeRow("TARGET SDK", state.apkMetadata.targetSdk.toString())
        }
    }
}

@Composable
private fun DetailAttributeRow(label: String, value: String) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
            }
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun openWithSystem(context: Context, item: FileItem) {
    try {
        val file = File(item.path)
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, item.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}
