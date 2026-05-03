package com.daviapps.launchernodopamin.data

import android.content.Intent
import android.content.pm.PackageManager.MATCH_ALL
import android.os.Build
import android.content.pm.PackageManager
import java.util.Locale

class InstalledAppsRepository(
    private val packageManager: PackageManager,
    private val selfPackageName: String
) {
    fun getLaunchableApps(): List<LaunchableApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return queryLauncherActivities(launcherIntent)
            .asSequence()
            .map { resolveInfo ->
                LaunchableApp(
                    label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty(),
                    packageName = resolveInfo.activityInfo.packageName
                )
            }
            .filter { app ->
                app.label.isNotBlank() && app.packageName != selfPackageName
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun queryLauncherActivities(intent: Intent) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, MATCH_ALL)
        }
}
