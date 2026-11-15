package com.arjunvision.tvlauncher.platform.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arjunvision.tvlauncher.data.themes.ThemeEngine
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import java.util.concurrent.TimeUnit

/**
 * Daily: apply the right theme for today (works offline) and start monthly discovery when due.
 * If discovery needs the internet and it is down, a network-constrained retry is queued, so it
 * runs by itself when the connection comes back.
 */
class ThemeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        val category = inputData.getString(KEY_CATEGORY)?.let { runCatching { ThemeCategory.valueOf(it) }.getOrNull() }
        val isNetworkJob = inputData.getBoolean(KEY_NETWORK_JOB, false)
        val result = ThemeEngine(applicationContext).run(force = force, onlyCategory = category)
        if (result.needsNetworkRetry) {
            if (isNetworkJob) return Result.retry()
            ThemeWork.discoverWhenOnline(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val KEY_FORCE = "force"
        const val KEY_CATEGORY = "category"
        const val KEY_NETWORK_JOB = "network_job"
    }
}

object ThemeWork {
    private const val DAILY = "arjun-theme-daily"
    private const val DISCOVERY = "arjun-theme-discovery"

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<ThemeWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs as soon as there is a network; backs off politely on failure (rate limits). */
    fun discoverWhenOnline(context: Context, force: Boolean = false, category: ThemeCategory? = null) {
        val request = OneTimeWorkRequestBuilder<ThemeWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .setInputData(
                workDataOf(
                    ThemeWorker.KEY_FORCE to force,
                    ThemeWorker.KEY_NETWORK_JOB to true,
                    ThemeWorker.KEY_CATEGORY to category?.name,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(DISCOVERY, ExistingWorkPolicy.REPLACE, request)
    }
}
