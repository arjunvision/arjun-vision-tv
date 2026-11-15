package com.arjunvision.tvlauncher.data.applications

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.arjunvision.tvlauncher.domain.models.LauncherApp

/**
 * Finds apps the user can open. TV apps normally expose LEANBACK_LAUNCHER;
 * phone-style apps only expose LAUNCHER, so both are queried.
 */
class InstalledAppsRepository(private val context: Context) {

    private val packageManager = context.packageManager

    fun launchableApps(): List<LauncherApp> =
        (queryLaunchers(Intent.CATEGORY_LEANBACK_LAUNCHER) + queryLaunchers(Intent.CATEGORY_LAUNCHER))
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    /** Returns false if the app cannot be opened (removed, disabled or not exported). */
    fun launch(app: LauncherApp): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    @Suppress("DEPRECATION") // queryIntentActivities(Intent, Int) is the API 30 overload; minSdk is 30.
    private fun queryLaunchers(category: String): List<LauncherApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category)
        return packageManager.queryIntentActivities(query, 0).mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            LauncherApp(
                packageName = activity.packageName,
                activityName = activity.name,
                label = info.loadLabel(packageManager).toString(),
            )
        }
    }
}
