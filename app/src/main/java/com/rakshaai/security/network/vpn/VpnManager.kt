package com.rakshaai.security.network.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnManager {

    private val _isVpnRunning = MutableStateFlow(false)
    val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

    fun updateVpnRunningState(isRunning: Boolean) {
        _isVpnRunning.value = isRunning
    }

    fun prepareVpn(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    fun startVpnService(context: Context) {
        val intent = Intent(context, RakshaVpnService::class.java).apply {
            action = RakshaVpnService.ACTION_START_VPN
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Throwable) {
            context.startService(intent)
        }
    }

    fun stopVpnService(context: Context) {
        val intent = Intent(context, RakshaVpnService::class.java).apply {
            action = RakshaVpnService.ACTION_STOP_VPN
        }
        try {
            context.startService(intent)
        } catch (_: Throwable) {
            context.stopService(intent)
        }
    }
}
