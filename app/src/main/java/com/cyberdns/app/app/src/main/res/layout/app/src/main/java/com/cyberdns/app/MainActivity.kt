package com.cyberdns.app

import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isConnected = false
    private lateinit var btnToggle: Button
    private lateinit var etDns1: EditText
    private lateinit var etDns2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnToggle = findViewById(R.id.btnToggle)
        etDns1 = findViewById(R.id.etDns1)
        etDns2 = findViewById(R.id.etDns2)

        btnToggle.setOnClickListener {
            if (isConnected) {
                disconnectDns()
            } else {
                prepareVpn()
            }
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 100)
        } else {
            onActivityResult(100, RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            connectDns()
        }
    }

    private fun connectDns() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            putExtra("DNS1", etDns1.text.toString().trim())
            putExtra("DNS2", etDns2.text.toString().trim())
        }
        startService(intent)
        isConnected = true
        btnToggle.text = "قطع اتصال"
        btnToggle.setBackgroundColor(Color.parseColor("#FF0055"))
        btnToggle.setTextColor(Color.WHITE)
    }

    private fun disconnectDns() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
        isConnected = false
        btnToggle.text = "اتصال دی‌ان‌اس"
        btnToggle.setBackgroundColor(Color.parseColor("#00F3FF"))
        btnToggle.setTextColor(Color.BLACK)
    }
}
