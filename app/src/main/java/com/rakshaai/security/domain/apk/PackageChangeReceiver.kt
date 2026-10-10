package com.rakshaai.security.domain.apk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        if (packageName == context.packageName) return

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    ApkMonitoringManager.handleAppInstallationEvent(context, packageName, isUpdate = false)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                ApkMonitoringManager.handleAppInstallationEvent(context, packageName, isUpdate = true)
            }
        }
    }
}
