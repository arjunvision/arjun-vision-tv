package com.arjunvision.tvlauncher.ui.wallpapers

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.data.wallpapers.Wallpaper
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import com.arjunvision.tvlauncher.ui.theme.LauncherStyle
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMB_WIDTH = 480

private data class Choice(
    /** null = automatic, "theme" = active theme photo, else a wallpaper id. */
    val id: String?,
    val name: String,
    val detail: String,
    /** "asset:<path>" or an absolute file path; null draws the automatic gradient. */
    val source: String?,
)

/**
 * Theme & Wallpaper Studio: every Home wallpaper available offline. Changing the image here never
 * downloads anything and never changes the theme colours.
 */
@Composable
fun WallpapersScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val firstTile = remember { FocusRequester() }
    val current = state.settings.wallpaperId
    val autoName = if (state.settings.profile == com.arjunvision.tvlauncher.domain.models.Profile.FATHER) {
        "Iron Man · Vengeance" // shown only when the art is present; otherwise a calm default photo
    } else {
        "Iron Man · Shadow"
    }

    val iron = viewModel.wallpapers.userArt
    val licensed = viewModel.wallpapers.bundled

    LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LauncherColors.Background, LauncherColors.Surface)))
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Text(text = "Wallpaper", color = LauncherColors.TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Home background for both profiles. Automatic follows the profile; a chosen photo stays until you change it. Licensed photos work offline — credits are in the app's ATTRIBUTION.md.",
            color = LauncherColors.TextSecondary,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(18.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(4) }) {
                Text(
                    text = "Choose a background",
                    color = LauncherColors.Blue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                )
            }
            itemsIndexed(
                listOfNotNull(
                    Choice(null, "Automatic", autoName, null),
                    LauncherStyle.wallpaperPath?.let { Choice("theme", "Active theme photo", LauncherStyle.themeName, it) },
                ),
            ) { index, choice ->
                WallpaperTile(
                    choice = choice,
                    selected = (current == null && choice.id == null) || (choice.id != null && current == choice.id),
                    fallback = LauncherStyle.wallpaperPath,
                    background = LauncherColors.Blue,
                    onClick = { viewModel.setWallpaper(choice.id, choice.name) },
                    modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                )
            }

            if (iron.isNotEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "IRON MAN  ·  YOUR IMAGES, PERSONAL USE",
                        color = LauncherColors.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                itemsIndexed(iron) { _, wallpaper ->
                    WallpaperTile(
                        choice = wallpaper.toChoice(),
                        selected = current == wallpaper.id,
                        fallback = LauncherStyle.wallpaperPath,
                        background = Color(0xFFB3121F),
                        onClick = { viewModel.setWallpaper(wallpaper.id, wallpaper.name) },
                    )
                }
            }

            if (licensed.isNotEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Text(
                        text = "LICENSED PHOTOS  ·  WORK OFFLINE",
                        color = LauncherColors.Blue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            itemsIndexed(licensed) { _, wallpaper ->
                WallpaperTile(
                    choice = wallpaper.toChoice(),
                    selected = current == wallpaper.id,
                    fallback = LauncherStyle.wallpaperPath,
                    background = LauncherColors.Violet,
                    onClick = { viewModel.setWallpaper(wallpaper.id, wallpaper.name) },
                )
            }
        }
    }
}

private fun Wallpaper.toChoice() = Choice(id, name, category, source)

@Composable
private fun WallpaperTile(
    choice: Choice,
    selected: Boolean,
    fallback: String?,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = rememberWallpaperThumb(choice.source ?: fallback)
    FocusCard(
        onClick = onClick,
        motion = 1f,
        shape = RoundedCornerShape(16.dp),
        background = Brush.linearGradient(
            listOf(background.copy(alpha = 0.42f), LauncherColors.Surface),
        ),
        modifier = modifier.fillMaxWidth().height(112.dp),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Readability scrim for the name over any photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.86f),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxSize().padding(14.dp),
        ) {
            Text(
                text = choice.name,
                color = LauncherColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = choice.detail,
                color = LauncherColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Text(
                text = "CURRENT",
                color = Color(0xFF140F04),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LauncherColors.Blue)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

private val thumbCache = ConcurrentHashMap<String, ImageBitmap>()

/** Small RGB_565 thumbnail from assets or a downloaded theme photo; shared across tiles. */
@Composable
private fun rememberWallpaperThumb(source: String?): ImageBitmap? {
    val context = LocalContext.current
    val thumb by produceState(initialValue = source?.let { thumbCache[it] }, source) {
        if (source == null || value != null) return@produceState
        value = withContext(Dispatchers.IO) { loadThumb(context, source) }
    }
    return thumb
}

private fun loadThumb(context: android.content.Context, source: String): ImageBitmap? = runCatching {
    val open: () -> java.io.InputStream = if (source.startsWith("asset:")) {
        { context.assets.open(source.removePrefix("asset:")) }
    } else {
        if (!File(source).exists()) return null
        { File(source).inputStream() }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open().use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMB_WIDTH) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        // Photos have no alpha; RGB_565 halves thumbnail memory on the 1 GB TV.
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    open().use { BitmapFactory.decodeStream(it, null, options) }
        ?.asImageBitmap()
        ?.also { thumbCache[source] = it }
}.getOrNull()
