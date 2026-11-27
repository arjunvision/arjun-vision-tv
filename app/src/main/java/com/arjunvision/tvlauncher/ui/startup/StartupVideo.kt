package com.arjunvision.tvlauncher.ui.startup

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import com.arjunvision.tvlauncher.R

/**
 * Startup: Sri's own "Arjun Vision Logo Reveal" video (res/raw, 5 s, with sound).
 * - Any remote key skips straight to Home (Back is also handled by the root BackHandler).
 * - Drawn on a TextureView so the screen transition can fade it into Home
 *   (a SurfaceView/VideoView would ignore alpha and cut abruptly).
 * - If the video can't play for any reason, the animated logo intro ([StartupScreen]) runs instead.
 */
@Composable
fun StartupVideo(motion: Float, onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    var failed by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    if (failed) {
        StartupScreen(motion = motion, onFinished = onFinished)
        return
    }

    fun complete() {
        if (!done) {
            done = true
            finish()
        }
    }

    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // matches the video's own light background: no black flash before frame 1
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) complete()
                true
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                TextureView(context).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                            try {
                                context.resources.openRawResourceFd(R.raw.arjun_vision_logo_reveal).use { afd ->
                                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                }
                                player.setSurface(Surface(texture))
                                player.setOnCompletionListener { complete() }
                                player.setOnErrorListener { _, _, _ ->
                                    failed = true
                                    true
                                }
                                player.setOnPreparedListener { it.start() }
                                player.prepareAsync()
                            } catch (e: Exception) {
                                failed = true
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                            runCatching { player.stop() }
                            return true
                        }
                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                    }
                }
            },
        )
    }
}
