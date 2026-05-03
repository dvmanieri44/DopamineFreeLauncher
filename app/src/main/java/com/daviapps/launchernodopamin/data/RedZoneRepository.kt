package com.daviapps.launchernodopamin.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RedZoneRepository(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getSelectedPackageNames(): Set<String> {
        return getRedZoneAppConfigs().keys
    }

    fun getRedZoneAppConfigs(): Map<String, RedZoneAppConfig> {
        val savedConfigs = sharedPreferences.getString(KEY_RED_ZONE_APP_CONFIGS, null)

        if (!savedConfigs.isNullOrBlank()) {
            return runCatching {
                parseConfigs(savedConfigs)
            }.getOrElse {
                loadLegacyConfigs()
            }
        }

        return loadLegacyConfigs()
    }

    fun saveRedZoneAppConfig(config: RedZoneAppConfig) {
        val updatedConfigs = getRedZoneAppConfigs().toMutableMap().apply {
            put(config.packageName, config)
        }
        persistConfigs(updatedConfigs.values)
    }

    fun removeRedZoneAppConfig(packageName: String) {
        val updatedConfigs = getRedZoneAppConfigs().toMutableMap().apply {
            remove(packageName)
        }
        persistConfigs(updatedConfigs.values)
    }

    private fun loadLegacyConfigs(): Map<String, RedZoneAppConfig> {
        return sharedPreferences
            .getStringSet(KEY_RED_ZONE_PACKAGES, emptySet())
            .orEmpty()
            .associateWith { packageName ->
                RedZoneAppConfig(
                    packageName = packageName,
                    timeLimitMinutes = null
                )
            }
    }

    private fun parseConfigs(serializedConfigs: String): Map<String, RedZoneAppConfig> {
        val jsonArray = JSONArray(serializedConfigs)
        val configs = linkedMapOf<String, RedZoneAppConfig>()

        for (index in 0 until jsonArray.length()) {
            val configObject = jsonArray.optJSONObject(index) ?: continue
            val packageName = configObject.optString(KEY_PACKAGE_NAME).trim()

            if (packageName.isEmpty()) {
                continue
            }

            val timeLimitMinutes = if (
                configObject.has(KEY_TIME_LIMIT_MINUTES) &&
                !configObject.isNull(KEY_TIME_LIMIT_MINUTES)
            ) {
                configObject.optInt(KEY_TIME_LIMIT_MINUTES)
            } else {
                null
            }

            configs[packageName] = RedZoneAppConfig(
                packageName = packageName,
                timeLimitMinutes = timeLimitMinutes
            )
        }

        return configs
    }

    private fun persistConfigs(configs: Collection<RedZoneAppConfig>) {
        val sortedConfigs = configs.sortedBy { config -> config.packageName }
        val jsonArray = JSONArray()

        sortedConfigs.forEach { config ->
            val jsonObject = JSONObject()
                .put(KEY_PACKAGE_NAME, config.packageName)

            if (config.timeLimitMinutes == null) {
                jsonObject.put(KEY_TIME_LIMIT_MINUTES, JSONObject.NULL)
            } else {
                jsonObject.put(KEY_TIME_LIMIT_MINUTES, config.timeLimitMinutes)
            }

            jsonArray.put(jsonObject)
        }

        sharedPreferences
            .edit()
            .putString(KEY_RED_ZONE_APP_CONFIGS, jsonArray.toString())
            .putStringSet(
                KEY_RED_ZONE_PACKAGES,
                sortedConfigs.map { config -> config.packageName }.toSet()
            )
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "launcher_no_dopamin_preferences"
        const val KEY_RED_ZONE_PACKAGES = "red_zone_packages"
        const val KEY_RED_ZONE_APP_CONFIGS = "red_zone_app_configs"
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_TIME_LIMIT_MINUTES = "timeLimitMinutes"
    }
}
