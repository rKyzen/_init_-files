package com.init.file.ui.screens.splash

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.init.file.R
import com.init.file.theme.DarkBackground
import com.init.file.theme.JetBrainsMonoFontFamily
import com.init.file.theme.LightBackground
import com.init.file.theme.MichromaFontFamily
import com.init.file.theme.SignalAccent
import com.init.file.ui.components.DotMatrixGrid
import com.init.file.ui.components.InitSegmentedProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isVideoFailed by remember { mutableStateOf(false) }
    var hasFinishedCalled by remember { mutableStateOf(false) }
    var isFadingOut by remember { mutableStateOf(false) }

    val splashAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "splash_fade"
    )

    val isLight = MaterialTheme.colorScheme.background == LightBackground || MaterialTheme.colorScheme.background != DarkBackground
    val rawVideoResId = if (isLight) R.raw.startup_white else R.raw.full_startup_in
    val bgColor = if (isLight) MaterialTheme.colorScheme.background else Color.Black

    if (rawVideoResId == 0 || isVideoFailed) {
        StaticFallbackSplashScreen(
            isLight = isLight,
            onSplashFinished = {
                if (!hasFinishedCalled) {
                    hasFinishedCalled = true
                    onSplashFinished()
                }
            }
        )
    } else {
        val exoPlayer = remember(rawVideoResId) {
            ExoPlayer.Builder(context).build().apply {
                val videoUri = Uri.parse("android.resource://${context.packageName}/$rawVideoResId")
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                prepare()
            }
        }

        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        coroutineScope.launch {
                            // Hold the video's last frame for 1 second before proceeding
                            delay(1000)
                            // Smoothly fade out the splash screen
                            isFadingOut = true
                            delay(500)
                            if (!hasFinishedCalled) {
                                hasFinishedCalled = true
                                onSplashFinished()
                            }
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isVideoFailed = true
                }
            }

            exoPlayer.addListener(listener)

            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .graphicsLayer { alpha = splashAlpha },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(if (isLight) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                    view.setBackgroundColor(if (isLight) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
                },
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
fun StaticFallbackSplashScreen(
    isLight: Boolean,
    onSplashFinished: () -> Unit
) {
    var bootStep by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0.1f) }
    var isFadingOut by remember { mutableStateOf(false) }

    val fallbackAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "fallback_fade"
    )

    LaunchedEffect(Unit) {
        delay(200)
        bootStep = 1
        progress = 0.35f
        delay(200)
        bootStep = 2
        progress = 0.70f
        delay(200)
        bootStep = 3
        progress = 1.0f
        delay(600)
        isFadingOut = true
        delay(450)
        onSplashFinished()
    }

    val bgColor = if (isLight) MaterialTheme.colorScheme.background else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .graphicsLayer { alpha = fallbackAlpha },
        contentAlignment = Alignment.Center
    ) {
        DotMatrixGrid()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Signal Accent Boot Indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name in Michroma
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = MichromaFontFamily,
                fontSize = 24.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Suite Mark
            Text(
                text = stringResource(R.string.brand_tagline),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Mechanical Segmented Progress
            InitSegmentedProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(0.7f),
                segments = 16
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Terminal Boot Lines
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (bootStep >= 1) {
                    BootTerminalLine(text = "sys_boot: initializing fs engine", isOk = true)
                }
                if (bootStep >= 2) {
                    BootTerminalLine(text = "storage: mounting volumes", isOk = true)
                }
                if (bootStep >= 3) {
                    BootTerminalLine(text = "system: sys_ready [ok]", isOk = true, isAccent = true)
                }
            }
        }
    }
}

@Composable
fun BootTerminalLine(text: String, isOk: Boolean, isAccent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = if (isAccent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isOk) {
            Text(
                text = "[ok]",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (isAccent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
