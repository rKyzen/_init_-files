package com.init.files.ui.screens.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.init.files.data.storage.ApkMetadata
import com.init.files.data.storage.FileManager
import com.init.files.data.storage.PdfRendererHelper
import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PreviewUiState(
    val fileItem: FileItem? = null,
    // Text / Code
    val textContent: String? = null,
    val isTextLoading: Boolean = false,
    val wordWrap: Boolean = false,
    val searchQuery: String = "",
    val textLineCount: Int = 0,
    // PDF
    val isPdf: Boolean = false,
    val isPdfLoading: Boolean = false,
    val pdfPageCount: Int = 0,
    val pdfPages: List<Bitmap> = emptyList(),
    // Image
    val imageRotation: Float = 0f,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    // Video / Audio Playback
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isBuffering: Boolean = false,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    // Metadata & Hashes
    val apkMetadata: ApkMetadata? = null,
    val sha256Hash: String = "CALCULATING...",
    val md5Hash: String = "CALCULATING...",
    val showDetailsSheet: Boolean = false
)

class PreviewViewModel(
    private val fileManager: FileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private val pdfHelper = PdfRendererHelper()
    private var exoPlayer: ExoPlayer? = null
    private var progressTrackingJob: Job? = null

    fun loadFile(context: Context, item: FileItem) {
        releasePlayer()
        _uiState.update {
            PreviewUiState(
                fileItem = item,
                sha256Hash = "CALCULATING...",
                md5Hash = "CALCULATING..."
            )
        }

        val ext = item.extension.lowercase()

        // 1. PDF File
        if (ext == "pdf") {
            _uiState.update { it.copy(isPdf = true) }
            loadPdfDocument(item.path)
        }
        // 2. Image File
        else if (item.category == FileCategory.IMAGES) {
            loadImageMetadata(item.path)
        }
        // 3. Text / Document File
        else if (item.category == FileCategory.DOCUMENTS || isTextExtension(ext)) {
            loadTextContent(item.path)
        }
        // 4. Video / Audio File
        else if (item.category == FileCategory.VIDEOS || item.category == FileCategory.AUDIO) {
            initPlayer(context, item.path)
        }

        // 5. APK File
        if (ext == "apk") {
            val apkInfo = fileManager.getApkMetadata(item.path)
            _uiState.update { it.copy(apkMetadata = apkInfo) }
        }

        // Checksums calculation in background
        viewModelScope.launch {
            val sha256 = fileManager.calculateChecksum(item.path, "SHA-256")
            val md5 = fileManager.calculateChecksum(item.path, "MD5")
            _uiState.update {
                it.copy(
                    sha256Hash = sha256,
                    md5Hash = md5
                )
            }
        }
    }

    private fun loadPdfDocument(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPdfLoading = true) }
            val docInfo = pdfHelper.getDocumentInfo(path)
            if (docInfo != null && docInfo.pageCount > 0) {
                _uiState.update { it.copy(pdfPageCount = docInfo.pageCount) }
                val pages = mutableListOf<Bitmap>()
                // Render pages (up to 50 pages for memory safety)
                val maxPages = minOf(docInfo.pageCount, 50)
                for (i in 0 until maxPages) {
                    val bmp = pdfHelper.renderPage(path, i, targetWidth = 1080)
                    if (bmp != null) {
                        pages.add(bmp)
                    }
                }
                _uiState.update { it.copy(pdfPages = pages, isPdfLoading = false) }
            } else {
                _uiState.update { it.copy(isPdfLoading = false) }
            }
        }
    }

    private fun loadImageMetadata(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                _uiState.update {
                    it.copy(
                        imageWidth = options.outWidth,
                        imageHeight = options.outHeight,
                        imageRotation = 0f
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun rotateImage() {
        _uiState.update { it.copy(imageRotation = (it.imageRotation + 90f) % 360f) }
    }

    private fun loadTextContent(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTextLoading = true) }
            val (content, lineCount) = withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    if (file.length() > 2 * 1024 * 1024) { // Max 2MB preview
                        val lines = file.bufferedReader().useLines { it.take(1500).toList() }
                        Pair(lines.joinToString("\n") + "\n\n... [TRUNCATED - FILE > 2MB]", lines.size)
                    } else {
                        val text = file.readText()
                        Pair(text, text.lines().size)
                    }
                } catch (e: Exception) {
                    Pair("Error loading text content: ${e.message}", 0)
                }
            }
            _uiState.update {
                it.copy(
                    textContent = content,
                    textLineCount = lineCount,
                    isTextLoading = false
                )
            }
        }
    }

    fun toggleWordWrap() {
        _uiState.update { it.copy(wordWrap = !it.wordWrap) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleDetailsSheet() {
        _uiState.update { it.copy(showDetailsSheet = !it.showDetailsSheet) }
    }

    // --- Media3 ExoPlayer In-App Video & Audio Controls ---

    private fun initPlayer(context: Context, path: String) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(File(path).toURI().toString())
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlayingNow) }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isBuff = playbackState == Player.STATE_BUFFERING
                        val dur = duration.coerceAtLeast(0L)
                        _uiState.update { it.copy(isBuffering = isBuff, durationMs = dur) }
                    }
                })
            }
            startTrackingProgress()
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skip(deltaMs: Long) {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(target)
        }
    }

    fun toggleResizeMode() {
        _uiState.update {
            val next = if (it.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            it.copy(resizeMode = next)
        }
    }

    private fun startTrackingProgress() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _uiState.update { it.copy(currentPositionMs = pos, durationMs = dur) }
                }
                delay(300)
            }
        }
    }

    fun releasePlayer() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    private fun isTextExtension(ext: String): Boolean {
        return setOf(
            "txt", "md", "json", "xml", "kt", "java", "py", "c", "cpp", "h", "hpp",
            "js", "ts", "html", "css", "scss", "yml", "yaml", "sh", "bat", "cmd",
            "properties", "gradle", "kts", "sql", "log", "csv", "tsv", "ini", "conf", "env"
        ).contains(ext.lowercase())
    }
}
