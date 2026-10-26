package com.cyberdns.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private var isConnected = false
    private lateinit var btnToggle: Button
    private lateinit var etDns1: EditText
    private lateinit var etDns2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#070913"))
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "NERO CYBER DNS"
            setTextColor(Color.parseColor("#00F3FF"))
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text = "دی‌ان‌اس اختصاصی گیمینگ و کاهش پینگ"
            setTextColor(Color.parseColor("#8B9BB4"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 80)
        }

        val boxBg1 = GradientDrawable().apply {
            setColor(Color.parseColor("#12182B"))
            cornerRadius = 24f
            setStroke(2, Color.parseColor("#00F3FF"))
        }

        etDns1 = EditText(this).apply {
            setText("178.22.122.100")
            setTextColor(Color.WHITE)
            background = boxBg1
            setPadding(40, 36, 40, 36)
        }

        val spacer1 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(10, 32)
        }

        val boxBg2 = GradientDrawable().apply {
            setColor(Color.parseColor("#12182B"))
            cornerRadius = 24f
            setStroke(2, Color.parseColor("#9D00FF"))
        }

        etDns2 = EditText(this).apply {
            setText("185.51.200.2")
            setTextColor(Color.WHITE)
            background = boxBg2
            setPadding(40, 36, 40, 36)
        }

        val spacer2 = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(10, 64)
        }

        btnToggle = Button(this).apply {
            text = "اتصال به سرور گیمینگ"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00F3FF"))
            setPadding(40, 40, 40, 40)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(etDns1)
        root.addView(spacer1)
        root.addView(etDns2)
        root.addView(spacer2)
        root.addView(btnToggle)

        setContentView(root)

        btnToggle.setOnClickListener {
            if (isConnected) {
                disconnectDns()
            } else {
                prepareVpn()
            }
        }
    }

    @Suppress("DEPRECATION")
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
        btnToggle.text = "اتصال به سرور گیمینگ"
        btnToggle.setBackgroundColor(Color.parseColor("#00F3FF"))
        btnToggle.setTextColor(Color.BLACK)
    }
}
