package com.daviapps.launchernodopamin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daviapps.launchernodopamin.ui.screens.HomeScreen
import com.daviapps.launchernodopamin.ui.screens.HomeViewModel
import com.daviapps.launchernodopamin.ui.theme.LauncherNoDopaminTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LauncherNoDopaminTheme {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(
                        context = applicationContext,
                        packageManager = packageManager,
                        selfPackageName = packageName
                    )
                )

                HomeScreen(
                    viewModel = homeViewModel,
                    onLaunchApp = ::launchApp,
                    onOpenUsageAccessSettings = ::openUsageAccessSettings,
                    onOpenAccessibilitySettings = ::openAccessibilitySettings
                )
            }
        }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openUsageAccessSettings() {
        startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openAccessibilitySettings() {
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
