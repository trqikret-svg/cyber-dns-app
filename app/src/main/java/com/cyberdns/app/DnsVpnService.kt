package com.cyberdns.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val dns1 = intent?.getStringExtra("DNS1") ?: "178.22.122.100"
        val dns2 = intent?.getStringExtra("DNS2") ?: "185.51.200.2"

        startForegroundServiceNotification()
        startVpn(dns1, dns2)
        return START_STICKY
    }

    private fun startVpn(dns1: String, dns2: String) {
        try {
            val builder = Builder()
                .setSession("CyberDNS")
                .addAddress("10.0.0.2", 32)
                .addDnsServer(dns1)
                .addDnsServer(dns2)

            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "CyberDnsChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cyber DNS Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Cyber DNS فعال است")
            .setContentText("ترافیک دی‌ان‌اس روی سرور گیمینگ تنظیم شد")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(1, notification)
    }

    @Suppress("DEPRECATION")
    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
