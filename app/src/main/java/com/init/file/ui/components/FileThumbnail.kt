package com.init.file.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.FileItem
import com.init.file.ui.screens.browse.getFileIcon
import java.io.File

/**
 * High-performance file thumbnail with smart image/video preview, APK icon extraction,
 * and Nothing OS category fallback.
 */
@Composable
fun FileThumbnail(
    item: FileItem,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    iconSize: Dp = 24.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val isImageOrVideo = remember(item.path, item.category) {
        !item.isDirectory && (
            item.category == FileCategory.IMAGES ||
            item.category == FileCategory.VIDEOS ||
            item.mimeType?.startsWith("image/") == true ||
            item.mimeType?.startsWith("video/") == true ||
            isImageExtension(item.extension) ||
            isVideoExtension(item.extension)
        )
    }

    val isApk = remember(item.extension) {
        !item.isDirectory && item.extension.equals("apk", ignoreCase = true)
    }

    val apkIconBitmap = remember(item.path, isApk) {
        if (isApk) getApkIcon(context, item.path) else null
    }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isImageOrVideo -> {
                    val file = remember(item.path) { File(item.path) }
                    val imageRequest = remember(item.path) {
                        ImageRequest.Builder(context)
                            .data(file)
                            .crossfade(150)
                            .build()
                    }

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = item.name,
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Video badge overlay
                    if (item.category == FileCategory.VIDEOS || isVideoExtension(item.extension) || item.mimeType?.startsWith("video/") == true) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.size(iconSize.coerceAtMost(28.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                apkIconBitmap != null -> {
                    Image(
                        bitmap = apkIconBitmap.asImageBitmap(),
                        contentDescription = "APK Icon",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }

                else -> {
                    Icon(
                        imageVector = getFileIcon(item),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

private fun isImageExtension(ext: String): Boolean {
    val lower = ext.lowercase()
    return lower in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg", "avif", "ico", "dng")
}

private fun isVideoExtension(ext: String): Boolean {
    val lower = ext.lowercase()
    return lower in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv", "m4v", "ts")
}

private fun getApkIcon(context: Context, path: String): Bitmap? {
    return try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageArchiveInfo(path, 0)
        packageInfo?.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = path
            appInfo.publicSourceDir = path
            val drawable: Drawable = appInfo.loadIcon(packageManager)
            drawable.toBitmap(width = 96, height = 96, config = Bitmap.Config.ARGB_8888)
        }
    } catch (_: Exception) {
        null
    }
}
