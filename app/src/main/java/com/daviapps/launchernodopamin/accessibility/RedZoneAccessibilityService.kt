package com.daviapps.launchernodopamin.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.daviapps.launchernodopamin.data.RedZoneDailyUsagePolicy
import com.daviapps.launchernodopamin.data.RedZoneDailyUsageRepository
import com.daviapps.launchernodopamin.data.RedZoneEnforcementRepository
import com.daviapps.launchernodopamin.data.RedZoneRepository

class RedZoneAccessibilityService : AccessibilityService() {
    private val monitorIntervalMillis = 1_000L
    private val blockOverlayDelayMillis = 500L
    private val handler = Handler(Looper.getMainLooper())
    private var currentForegroundPackage: String? = null
    private var currentSessionId: Long = 0L
    private var blockedSessionId: Long? = null
    private var monitorRunnable: Runnable? = null
    private var overlayView: View? = null
    private var trackedRedZonePackageName: String? = null
    private var trackedSessionStartElapsedMillis: Long? = null

    private lateinit var redZoneRepository: RedZoneRepository
    private lateinit var dailyUsageRepository: RedZoneDailyUsageRepository
    private lateinit var enforcementRepository: RedZoneEnforcementRepository
    private lateinit var windowManager: WindowManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }

        redZoneRepository = RedZoneRepository(applicationContext)
        dailyUsageRepository = RedZoneDailyUsageRepository(applicationContext)
        enforcementRepository = RedZoneEnforcementRepository(applicationContext)
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: return

        if (!enforcementRepository.getEnforcementState().isMonitoringActive) {
            finishTrackedRedZoneSession(SystemClock.elapsedRealtime())
            dismissBlockOverlay()
            cancelMonitoring()
            currentForegroundPackage = null
            return
        }

        handleForegroundPackage(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        finishTrackedRedZoneSession(SystemClock.elapsedRealtime())
        cancelMonitoring()
        dismissBlockOverlay()
        super.onDestroy()
    }

    private fun handleForegroundPackage(packageName: String) {
        if (packageName == currentForegroundPackage) {
            return
        }

        val nowElapsedMillis = SystemClock.elapsedRealtime()
        finishTrackedRedZoneSession(nowElapsedMillis)
        currentForegroundPackage = packageName
        currentSessionId += 1L
        blockedSessionId = null
        cancelMonitoring()

        if (packageName == applicationContext.packageName) {
            return
        }

        val limitMinutes = redZoneRepository
            .getRedZoneAppConfigs()[packageName]
            ?.timeLimitMinutes
            ?.takeIf { limit -> limit > 0 }
            ?: return

        beginTrackedRedZoneSession(
            packageName = packageName,
            sessionId = currentSessionId,
            nowElapsedMillis = nowElapsedMillis,
            limitMinutes = limitMinutes
        )
    }

    private fun beginTrackedRedZoneSession(
        packageName: String,
        sessionId: Long,
        nowElapsedMillis: Long,
        limitMinutes: Int
    ) {
        trackedRedZonePackageName = packageName
        trackedSessionStartElapsedMillis = nowElapsedMillis

        if (
            checkAndShowBlockOverlay(
                packageName = packageName,
                sessionId = sessionId,
                nowElapsedMillis = nowElapsedMillis,
                fallbackLimitMinutes = limitMinutes
            )
        ) {
            return
        }

        monitorRunnable = object : Runnable {
            override fun run() {
                val shouldStopMonitoring = checkAndShowBlockOverlay(
                    packageName = packageName,
                    sessionId = sessionId,
                    nowElapsedMillis = SystemClock.elapsedRealtime(),
                    fallbackLimitMinutes = limitMinutes
                )

                if (
                    shouldStopMonitoring ||
                    currentForegroundPackage != packageName ||
                    trackedRedZonePackageName != packageName
                ) {
                    monitorRunnable = null
                    return
                }

                handler.postDelayed(this, monitorIntervalMillis)
            }
        }
        handler.postDelayed(monitorRunnable!!, monitorIntervalMillis)
    }

    private fun checkAndShowBlockOverlay(
        packageName: String,
        sessionId: Long,
        nowElapsedMillis: Long,
        fallbackLimitMinutes: Int
    ): Boolean {
        if (
            currentForegroundPackage != packageName ||
            trackedRedZonePackageName != packageName ||
            blockedSessionId == sessionId ||
            overlayView != null
        ) {
            return true
        }

        val latestLimitMinutes = redZoneRepository
            .getRedZoneAppConfigs()[packageName]
            ?.timeLimitMinutes
            ?.takeIf { limit -> limit > 0 }
            ?: run {
                finishTrackedRedZoneSession(nowElapsedMillis)
                return true
            }

        val accumulatedMillis = dailyUsageRepository.getAccumulatedUsageMillis(packageName)
        val sessionStartElapsedMillis = trackedSessionStartElapsedMillis

        if (
            !RedZoneDailyUsagePolicy.isLimitReached(
                accumulatedMillis = accumulatedMillis,
                sessionStartElapsedMillis = sessionStartElapsedMillis,
                nowElapsedMillis = nowElapsedMillis,
                limitMinutes = latestLimitMinutes
            )
        ) {
            return false
        }

        val confirmedForegroundPackage = enforcementRepository.getForegroundPackageName()
        if (confirmedForegroundPackage != null && confirmedForegroundPackage != packageName) {
            return false
        }

        finishTrackedRedZoneSession(nowElapsedMillis)
        blockedSessionId = sessionId
        dailyUsageRepository.markLimitNotified(
            packageName = packageName,
            limitMinutes = latestLimitMinutes
        )
        closeCurrentAppAndShowBlockOverlay(
            packageName = packageName,
            limitMinutes = latestLimitMinutes.takeIf { limit -> limit > 0 } ?: fallbackLimitMinutes
        )

        return true
    }

    private fun closeCurrentAppAndShowBlockOverlay(
        packageName: String,
        limitMinutes: Int
    ) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed(
            {
                showBlockOverlay(
                    packageName = packageName,
                    limitMinutes = limitMinutes
                )
            },
            blockOverlayDelayMillis
        )
    }

    private fun finishTrackedRedZoneSession(nowElapsedMillis: Long) {
        val packageName = trackedRedZonePackageName
        val sessionStartElapsedMillis = trackedSessionStartElapsedMillis

        trackedRedZonePackageName = null
        trackedSessionStartElapsedMillis = null

        if (packageName == null || sessionStartElapsedMillis == null) {
            return
        }

        val elapsedMillis = RedZoneDailyUsagePolicy.sessionElapsedMillis(
            sessionStartElapsedMillis = sessionStartElapsedMillis,
            nowElapsedMillis = nowElapsedMillis
        )

        if (elapsedMillis > 0L) {
            dailyUsageRepository.addUsageMillis(
                packageName = packageName,
                usageMillis = elapsedMillis
            )
        }
    }

    private fun showBlockOverlay(
        packageName: String,
        limitMinutes: Int
    ) {
        if (overlayView != null) {
            return
        }

        val appLabel = runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName)

        val rootView = FrameLayout(this).apply {
            setBackgroundColor(0xB3000000.toInt())
            isClickable = true
            isFocusable = true
        }

        val cardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(28f)
                setColor(0xFF000000.toInt())
                setStroke(dpInt(1f), 0x38FFFFFF.toInt())
            }
            setPadding(dpInt(22f), dpInt(24f), dpInt(22f), dpInt(24f))
        }

        val titleView = TextView(this).apply {
            text = "Tempo limite ultrapassado"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
        }

        val messageView = TextView(this).apply {
            text = "$appLabel ultrapassou o limite de $limitMinutes min."
            setTextColor(0xADFFFFFF.toInt())
            textSize = 15f
            setLineSpacing(0f, 1.18f)
        }

        val okButton = TextView(this).apply {
            text = "OK"
            gravity = Gravity.CENTER
            setTextColor(0xFF000000.toInt())
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16f)
                setColor(0xFFFFFFFF.toInt())
            }
            setPadding(dpInt(18f), dpInt(14f), dpInt(18f), dpInt(14f))
            setOnClickListener {
                dismissBlockOverlay()
            }
        }

        cardView.addView(titleView)
        cardView.addView(messageView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpInt(14f)
        })
        cardView.addView(okButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpInt(22f)
        })

        rootView.addView(cardView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            leftMargin = dpInt(24f)
            rightMargin = dpInt(24f)
        })

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(rootView, layoutParams)
        overlayView = rootView
    }

    private fun dismissBlockOverlay() {
        overlayView?.let { view ->
            runCatching {
                windowManager.removeView(view)
            }
        }
        overlayView = null
    }

    private fun cancelMonitoring() {
        monitorRunnable?.let { handler.removeCallbacks(it) }
        monitorRunnable = null
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun dpInt(value: Float): Int {
        return dp(value).toInt()
    }
}
