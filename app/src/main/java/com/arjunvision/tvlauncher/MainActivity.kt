package com.arjunvision.tvlauncher

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArjunVisionTvTheme {
                LauncherRoot(viewModel)
            }
        }
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
