package com.cyberdns.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class DnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }

        val dns1 = intent?.getStringExtra("DNS1") ?: "202.133.91.159"
        val dns2 = intent?.getStringExtra("DNS2") ?: "82.22.175.205"

        establishVpn(dns1, dns2)
        return START_STICKY
    }

    private fun establishVpn(dns1: String, dns2: String) {
        try {
            vpnInterface?.close()
            val builder = Builder()
                .setSession("Nero DNS Pro")
                .addAddress("10.8.0.2", 32)
                .addDnsServer(dns1)
                .addDnsServer(dns2)

            // متد بومی اندروید که به صورت خودکار آیکون کلید VPN را بالای گوشی می‌آورد
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
