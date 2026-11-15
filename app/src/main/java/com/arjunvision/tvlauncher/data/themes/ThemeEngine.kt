package com.arjunvision.tvlauncher.data.themes

import android.content.Context
import com.arjunvision.tvlauncher.BuildConfig
import com.arjunvision.tvlauncher.domain.models.InspirationItem
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import com.arjunvision.tvlauncher.domain.models.ThemeSpec
import com.arjunvision.tvlauncher.domain.themes.ThemeFactory
import com.arjunvision.tvlauncher.domain.themes.ThemeScheduler
import com.arjunvision.tvlauncher.platform.network.currentNetworkState
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Outcome of one engine run, so the worker knows whether to retry when the internet returns. */
data class EngineResult(val message: String, val needsNetworkRetry: Boolean)

/**
 * Discover → download → create original theme → save → auto-apply on the right date.
 * Works offline: scheduling and applying only use what is already saved.
 */
class ThemeEngine(private val context: Context) {

    private val repository = ThemeRepository.get(context)
    private val cache = ThemeAssetCache(context)

    private val sources: List<InspirationSource> = buildList {
        add(WikimediaCommonsSource())
        if (BuildConfig.PEXELS_API_KEY.isNotBlank()) add(PexelsSource(BuildConfig.PEXELS_API_KEY))
        if (BuildConfig.PIXABAY_API_KEY.isNotBlank()) add(PixabaySource(BuildConfig.PIXABAY_API_KEY))
    }

    val sourceNames: List<String> get() = sources.map { it.name }

    /** Monthly cadence: discovery is due after 25 days, or when an upcoming festival has no fresh theme. */
    fun discoveryDue(today: LocalDate = LocalDate.now()): Boolean {
        val s = repository.state.value
        val stale = System.currentTimeMillis() - s.lastDiscoveryMillis > TimeUnit.DAYS.toMillis(25)
        val missing = ThemeScheduler.categoriesToPrepare(today).any { c ->
            s.savedThemes.none { it.category == c && it.generated }
        }
        return stale || missing
    }

    /** Daily check. [force] = user pressed "Discover now"; [onlyCategory] = make one category now. */
    suspend fun run(force: Boolean = false, onlyCategory: ThemeCategory? = null): EngineResult {
        val today = LocalDate.now()
        var message = "Themes checked"
        var retry = false
        if (force || onlyCategory != null || discoveryDue(today)) {
            val net = currentNetworkState(context)
            when {
                !net.internet -> {
                    message = if (net.wifi) "Wi-Fi has no internet. Using saved themes; will retry." else "Offline. Using saved themes; will retry."
                    retry = true
                }
                !HttpJson.isReachable(SERVICE_CHECK_URL) -> {
                    message = "Image service unavailable. Using saved themes; will retry."
                    retry = true
                }
                else -> message = discover(today, onlyCategory)
            }
            repository.update { it.copy(lastDiscoveryResult = message) }
        }
        applyAutomaticTheme(today)
        cleanup()
        return EngineResult(message, retry)
    }

    private suspend fun discover(today: LocalDate, onlyCategory: ThemeCategory?): String {
        val categories = onlyCategory?.let { listOf(it) }
            ?: ThemeScheduler.categoriesToPrepare(today).take(MAX_CATEGORIES_PER_RUN)
        val width = cache.targetWidth()
        val created = mutableListOf<String>()
        val found = mutableListOf<InspirationItem>()
        var rateLimited = false
        for (category in categories) {
            val items = mutableListOf<InspirationItem>()
            for (source in sources) {
                try {
                    items += source.search(category, RESULTS_PER_SOURCE, width)
                } catch (e: RateLimitedException) {
                    rateLimited = true
                } catch (e: Exception) {
                    // One failing source never stops the others.
                }
            }
            val known = repository.state.value.inspirations.associateBy { it.id }
            val fresh = items.distinctBy { it.id }.filter { known[it.id]?.failed != true }
            val theme = createFromFirstWorking(category, fresh, found)
            if (theme != null) created += theme.name
        }
        repository.update { s ->
            val merged = (found + s.inspirations).distinctBy { it.id }.sortedByDescending { it.discoveredAt }
            s.copy(inspirations = merged.take(MAX_INSPIRATIONS), lastDiscoveryMillis = System.currentTimeMillis())
        }
        return when {
            created.isNotEmpty() -> "Created ${created.size} new theme(s): ${created.joinToString()}"
            rateLimited -> "Image service rate limit reached. Will try again later."
            else -> "No suitable licensed images found this time."
        }
    }

    /** Tries candidates in order; broken URLs are marked failed and skipped next time. */
    private suspend fun createFromFirstWorking(
        category: ThemeCategory,
        candidates: List<InspirationItem>,
        found: MutableList<InspirationItem>,
    ): ThemeSpec? {
        for (item in candidates.take(4)) {
            val file = cache.download(item)
            if (file == null) {
                found += item.copy(failed = true)
                continue
            }
            val withPath = item.copy(localPath = file.absolutePath)
            found += withPath
            found += candidates.filter { it.id != item.id }.take(3) // keep metadata for the inspiration row
            val theme = ThemeFactory.generate(category, withPath, file.absolutePath, cache.dominantColor(file), System.currentTimeMillis())
            repository.update { s -> s.copy(savedThemes = (s.savedThemes.filter { it.id != theme.id } + theme)) }
            return theme
        }
        return null
    }

    /** Creates a theme from one inspiration the user picked in the studio. */
    suspend fun createFromInspiration(item: InspirationItem): ThemeSpec? {
        val found = mutableListOf<InspirationItem>()
        val theme = createFromFirstWorking(item.category, listOf(item), found)
        repository.update { s -> s.copy(inspirations = (found + s.inspirations).distinctBy { it.id }) }
        return theme
    }

    /** Applies the scheduled theme unless automatic themes are off or the user locked a theme. */
    suspend fun applyAutomaticTheme(today: LocalDate = LocalDate.now()) {
        val s = repository.state.value
        if (!s.autoEnabled || s.locked) return
        val category = ThemeScheduler.decide(today).category
        val target = s.savedThemes.filter { it.category == category && it.generated }.maxByOrNull { it.createdAt }
            ?: ThemeFactory.builtIn(category)
        if (s.activeThemeId != target.id) repository.update { it.copy(activeThemeId = target.id) }
    }

    /** Keeps saved themes, the active theme and the newest 2 per category; deletes unused images. */
    private suspend fun cleanup() {
        val s = repository.update { s ->
            val keep = s.savedThemes
                .groupBy { it.category }
                .flatMap { (_, list) ->
                    val sorted = list.sortedByDescending { it.createdAt }
                    sorted.filter { it.saved || it.id == s.activeThemeId } + sorted.take(2)
                }
                .distinctBy { it.id }
            s.copy(savedThemes = keep)
        }
        val keepPaths = (s.savedThemes.mapNotNull { it.wallpaperPath } +
            s.inspirations.take(KEEP_INSPIRATION_IMAGES).mapNotNull { it.localPath }).toSet()
        cache.cleanup(keepPaths)
    }

    companion object {
        const val SERVICE_CHECK_URL = "https://commons.wikimedia.org/w/api.php?action=query&meta=siteinfo&format=json"
        private const val MAX_CATEGORIES_PER_RUN = 3
        private const val RESULTS_PER_SOURCE = 6
        private const val MAX_INSPIRATIONS = 60
        private const val KEEP_INSPIRATION_IMAGES = 24
    }
}
