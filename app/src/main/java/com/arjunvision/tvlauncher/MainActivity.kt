package com.arjunvision.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.arjunvision.tvlauncher.ui.LauncherRoot
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.theme.ArjunVisionTvTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Refreshes tiles when an app is installed, removed or updated while the launcher is open. */
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.refreshDevices()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArjunVisionTvTheme {
                LauncherRoot(viewModel)
            }
        }
        // The theme's windowBackground avoids a white flash at launch; after the first frame it
        // would only be one more full-screen fill per frame, so drop it.
        window.decorView.post { window.setBackgroundDrawable(null) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setForeground(true)
        viewModel.refreshDevices()
    }

    override fun onPause() {
        viewModel.setForeground(false)
        super.onPause()
    }

    /** Any key resets the screensaver timer; while the screensaver shows, the key only wakes it. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.consumeKeyForScreensaver(event)) return true
        viewModel.onUserActivity()
        return super.dispatchKeyEvent(event)
    }

    /** Pressing the TV's Home button while the launcher is open (singleTask) lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
    }
}
