package com.daviapps.launchernodopamin.data

import android.content.Context

class RedZoneRepository(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getSelectedPackageNames(): Set<String> {
        return sharedPreferences.getStringSet(KEY_RED_ZONE_PACKAGES, emptySet()).orEmpty().toSet()
    }

    fun saveSelectedPackageNames(packageNames: Set<String>) {
        sharedPreferences
            .edit()
            .putStringSet(KEY_RED_ZONE_PACKAGES, packageNames.toSet())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_no_dopamin_preferences"
        const val KEY_RED_ZONE_PACKAGES = "red_zone_packages"
    }
}
