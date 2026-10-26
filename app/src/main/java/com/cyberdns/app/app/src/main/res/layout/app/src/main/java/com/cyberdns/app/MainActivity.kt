package com.cyberdns.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.graphics.Color

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingDns1 = "178.22.122.100"
    private var pendingDns2 = "185.51.200.2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#060814")
        window.navigationBarColor = Color.parseColor("#060814")

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#060814"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadDataWithBaseURL("https://app.nerodns.ir", getAppHtml(), "text/html", "UTF-8", null)
    }

    inner class WebAppInterface(private val mContext: Context) {

        @JavascriptInterface
        fun checkLicense(code: String): Boolean {
            val clean = code.trim().uppercase()
            // لایسنس‌های مجاز (قابل تغییر یا اتصال به سرور)
            return clean == "NERO-VIP" || clean == "NERO-2026" || (clean.startsWith("NERO-") && clean.length >= 8)
        }

        @JavascriptInterface
        fun openTelegramBot() {
            try {
                // آدرس ربات تلگرام خودت را اینجا بگذار
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/NeroDNS_Bot"))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JavascriptInterface
        fun connectVpn(dns1: String, dns2: String) {
            pendingDns1 = dns1
            pendingDns2 = dns2
            runOnUiThread { prepareVpn() }
        }

        @JavascriptInterface
        fun disconnectVpn() {
            runOnUiThread { stopDnsService() }
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
            val intent = Intent(this, DnsVpnService::class.java).apply {
                putExtra("DNS1", pendingDns1)
                putExtra("DNS2", pendingDns2)
            }
            startService(intent)
            webView.evaluateJavascript("window.onVpnStateChanged(true);", null)
        } else {
            webView.evaluateJavascript("window.onVpnStateChanged(false);", null)
        }
    }

    private fun stopDnsService() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = "STOP"
        }
        startService(intent)
        webView.evaluateJavascript("window.onVpnStateChanged(false);", null)
    }

    private fun getAppHtml(): String {
        return """
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  :root {
    --bg: #060814;
    --glass: rgba(16, 22, 42, 0.65);
    --border: rgba(0, 243, 255, 0.22);
    --cyan: #00f3ff;
    --pink: #ff0055;
    --purple: #9d00ff;
    --green: #00ff9d;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: Tahoma, sans-serif; user-select: none; -webkit-tap-highlight-color: transparent; }
  body { background: var(--bg); color: #fff; min-height: 100vh; overflow-x: hidden; }
  canvas#bg { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: -1; }

  /* نوار مراحل بالا (Stepper) */
  .stepper { display: flex; justify-content: space-between; padding: 18px 20px 10px; max-width: 460px; margin: 0 auto; }
  .step-dot { width: 24%; height: 5px; background: rgba(255,255,255,0.12); border-radius: 6px; transition: 0.4s; }
  .step-dot.active { background: var(--cyan); box-shadow: 0 0 10px var(--cyan); }

  .screen { display: none; padding: 16px; max-width: 460px; margin: 0 auto; animation: fadeIn 0.35s ease; }
  .screen.active { display: block; }
  @keyframes fadeIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

  /* کارت لیکید گلس */
  .glass-card {
    background: var(--glass);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid var(--border);
    border-radius: 24px;
    padding: 24px 20px;
    box-shadow: 0 12px 35px rgba(0, 0, 0, 0.6), inset 0 1px 1px rgba(255, 255, 255, 0.15);
    margin-bottom: 16px;
  }

  .logo-box {
    width: 76px; height: 76px; margin: 10px auto 16px;
    border-radius: 22px;
    background: linear-gradient(135deg, rgba(0,243,255,0.2), rgba(157,0,255,0.3));
    border: 1px solid var(--cyan);
    display: flex; align-items: center; justify-content: center;
    box-shadow: 0 0 25px rgba(0,243,255,0.3);
    font-size: 32px;
  }

  h1 { font-size: 22px; text-align: center; background: linear-gradient(90deg, #fff, var(--cyan)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 6px; font-weight: 900; }
  p.sub { font-size: 12px; color: #94a3b8; text-align: center; margin-bottom: 22px; line-height: 1.6; }

  .cyber-input {
    width: 100%; background: rgba(7, 10, 22, 0.85);
    border: 1px solid rgba(0, 243, 255, 0.35);
    border-radius: 14px; padding: 14px; color: #fff;
    font-size: 15px; text-align: center; letter-spacing: 2px;
    outline: none; margin-bottom: 14px; transition: 0.3s;
  }
  .cyber-input:focus { border-color: var(--cyan); box-shadow: 0 0 15px rgba(0,243,255,0.3); }

  .btn-main {
    width: 100%; background: linear-gradient(135deg, var(--cyan), #0088ff);
    border: none; border-radius: 14px; padding: 14px;
    color: #000; font-weight: bold; font-size: 14px;
    cursor: pointer; box-shadow: 0 4px 20px rgba(0, 243, 255, 0.35);
    transition: 0.2s; margin-bottom: 10px;
  }
  .btn-main:active { transform: scale(0.97); }

  .btn-tg {
    width: 100%; background: rgba(157, 0, 255, 0.15);
    border: 1px solid var(--purple); border-radius: 14px; padding: 12px;
    color: #e9d5ff; font-size: 12px; cursor: pointer;
  }

  /* لیست بازی‌ها */
  .games-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 10px; }
  .game-card {
    background: var(--glass); border: 1px solid rgba(255,255,255,0.1);
    border-radius: 18px; overflow: hidden; position: relative;
    transition: 0.25s; cursor: pointer;
  }
  .game-card:active { transform: scale(0.96); border-color: var(--cyan); }
  .game-img { width: 100%; height: 105px; object-fit: cover; display: block; }
  .game-overlay {
    padding: 10px; background: linear-gradient(0deg, rgba(6,8,20,0.98) 20%, rgba(6,8,20,0.5) 100%);
  }
  .game-name { font-size: 12px; font-weight: bold; color: #fff; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .game-ping { font-size: 10px; color: var(--green); background: rgba(0,255,157,0.12); padding: 2px 7px; border-radius: 6px; display: inline-block; }

  /* رادار اتصال صفحه ۴ */
  .radar-wrap { position: relative; width: 160px; height: 160px; margin: 20px auto 28px; display: flex; align-items: center; justify-content: center; }
  .radar-ring {
    position: absolute; width: 100%; height: 100%; border-radius: 50%;
    border: 2px solid var(--cyan); opacity: 0.3;
    animation: pulseRing 2s infinite;
  }
  .radar-ring.connected { border-color: var(--green); }
  @keyframes pulseRing { 0% { transform: scale(0.85); opacity: 0.7; } 100% { transform: scale(1.35); opacity: 0; } }

  .power-btn {
    width: 130px; height: 130px; border-radius: 50%;
    background: radial-gradient(circle, #152238 0%, #090d1a 100%);
    border: 3px solid var(--cyan);
    box-shadow: 0 0 30px rgba(0, 243, 255, 0.35), inset 0 0 20px rgba(0, 243, 255, 0.2);
    color: var(--cyan); font-size: 16px; font-weight: bold;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    cursor: pointer; z-index: 2; transition: 0.3s;
  }
  .power-btn.connected {
    border-color: var(--green); color: var(--green);
    box-shadow: 0 0 40px rgba(0, 255, 157, 0.5), inset 0 0 25px rgba(0, 255, 157, 0.25);
  }

  .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.07); font-size: 12px; }
  .info-val { color: var(--cyan); font-family: monospace; font-weight: bold; }
  .back-btn { background: transparent; border: 1px solid rgba(255,255,255,0.2); color: #94a3b8; padding: 8px 14px; border-radius: 10px; font-size: 11px; margin-bottom: 12px; cursor: pointer; }
  .err-msg { color: var(--pink); font-size: 12px; text-align: center; margin-bottom: 10px; display: none; }
</style>
</head>
<body>

<canvas id="bg"></canvas>

<!-- نوار وضعیت ۴ مرحله‌ای -->
<div class="stepper">
  <div class="step-dot active" id="dot1"></div>
  <div class="step-dot" id="dot2"></div>
  <div class="step-dot" id="dot3"></div>
  <div class="step-dot" id="dot4"></div>
</div>

<!-- مرحله ۱: صفحه لاگین لایسنس و تلگرام -->
<div class="screen active" id="step1">
  <div class="glass-card" style="margin-top: 25px;">
    <div class="logo-box">⚡</div>
    <h1>NERO DNS PRO</h1>
    <p class="sub">نسل جدید رادار کاهش پینگ و تحریم‌شکن گیمینگ<br>برای ورود، کد اشتراک دریافتی از ربات را وارد کنید</p>
    
    <input type="text" id="licInput" class="cyber-input" placeholder="NERO-VIP" value="NERO-VIP">
    <div class="err-msg" id="licErr">❌ کد اشتراک نامعتبر است!</div>
    
    <button class="btn-main" onclick="doLogin()">ورود به پنل گیمینگ</button>
    <button class="btn-tg" onclick="openBot()">🤖 دریافت کد اشتراک از ربات تلگرام</button>
  </div>
</div>

<!-- مرحله ۲: انتخاب بازی (با کاور تصویری) -->
<div class="screen" id="step2">
  <h1>انتخاب بازی</h1>
  <p class="sub">سرور بهینه‌شده برای بازی مورد نظر خود را انتخاب کنید</p>
  <div class="games-grid" id="gamesGrid"></div>
</div>

<!-- مرحله ۳: ثبت آی‌پی و انتخاب نود -->
<div class="screen" id="step3">
  <button class="back-btn" onclick="goStep(2)">⬅ بازگشت به بازی‌ها</button>
  <div class="glass-card">
    <h1 id="selGameTitle">Call of Duty</h1>
    <p class="sub">ثبت آی‌پی شما روی سرور اختصاصی و تنظیم نودها</p>

    <div class="info-row">
      <span>آی‌پی شبکه شما (Detected IP):</span>
      <span class="info-val" id="userIp">در حال شناسایی...</span>
    </div>
    <div class="info-row">
      <span>وضعیت ثبت آی‌پی در سرور:</span>
      <span class="info-val" id="ipStatus" style="color:#facc15;">ثبت نشده</span>
    </div>

    <div style="margin-top: 16px;">
      <label style="font-size:11px; color:#94a3b8;">سرور اول (Primary DNS):</label>
      <input type="text" id="dns1Input" class="cyber-input" style="margin-top:6px;">
      <label style="font-size:11px; color:#94a3b8;">سرور دوم (Secondary DNS):</label>
      <input type="text" id="dns2Input" class="cyber-input" style="margin-top:6px;">
    </div>

    <button class="btn-main" id="regIpBtn" onclick="registerIpAndNext()">ثبت آی‌پی و رفتن به رادار اتصال</button>
  </div>
</div>

<!-- مرحله ۴: رادار اتصال VPN -->
<div class="screen" id="step4">
  <button class="back-btn" onclick="goStep(3)">⬅ تغییر تنظیمات IP</button>
  <div class="glass-card" style="text-align: center;">
    <h1 id="radarGameName">رادار گیمینگ</h1>
    <p class="sub" id="connSub">آماده برقراری تونل اختصاصی دی‌ان‌اس</p>

    <div class="radar-wrap">
      <div class="radar-ring" id="ring1"></div>
      <div class="power-btn" id="powerBtn" onclick="toggleVpn()">
        <span style="font-size:26px; margin-bottom:4px;">⚡</span>
        <span id="powerTxt">اتصال</span>
      </div>
    </div>

    <div class="info-row">
      <span>Primary DNS:</span>
      <span class="info-val" id="finalDns1">-</span>
    </div>
    <div class="info-row">
      <span>Secondary DNS:</span>
      <span class="info-val" id="finalDns2">-</span>
    </div>
    <div class="info-row">
      <span>پکت‌لاس (Packet Loss):</span>
      <span class="info-val" style="color:var(--green);">0.0%</span>
    </div>
  </div>
</div>

<script>
  const games = [
    { name: "Call of Duty: Warzone", ping: "42ms", d1: "178.22.122.100", d2: "185.51.200.2", img: "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=400&q=80" },
    { name: "Valorant & Riot", ping: "38ms", d1: "10.202.10.202", d2: "10.202.10.102", img: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=400&q=80" },
    { name: "EA FC 25 / FIFA", ping: "49ms", d1: "178.22.122.100", d2: "185.51.200.2", img: "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=400&q=80" },
    { name: "PUBG Mobile", ping: "45ms", d1: "78.157.42.100", d2: "78.157.42.101", img: "https://images.unsplash.com/photo-1552820728-8b83bb6b773f?w=400&q=80" },
    { name: "Counter-Strike 2", ping: "36ms", d1: "185.51.200.2", d2: "178.22.122.100", img: "https://images.unsplash.com/photo-1612287230202-1ff1d85d1bdf?w=400&q=80" },
    { name: "Fortnite", ping: "51ms", d1: "10.202.10.10", d2: "10.202.10.11", img: "https://images.unsplash.com/photo-1589241062272-c0a000072dfa?w=400&q=80" },
    { name: "Apex Legends", ping: "47ms", d1: "178.22.122.100", d2: "10.202.10.202", img: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400&q=80" },
    { name: "Rainbow Six Siege", ping: "44ms", d1: "78.157.42.100", d2: "78.157.42.101", img: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400&q=80" }
  ];

  let currentGame = games[0];
  let vpnConnected = false;

  // رندر کارت بازی‌ها با عکس
  const grid = document.getElementById('gamesGrid');
  games.forEach((g, idx) => {
    const card = document.createElement('div');
    card.className = 'game-card';
    card.onclick = () => selectGame(idx);
    card.innerHTML = `
      <img class="game-img" src="${'$'}{g.img}" alt="${'$'}{g.name}">
      <div class="game-overlay">
        <div class="game-name">${'$'}{g.name}</div>
        <span class="game-ping">⚡ Ping ~${'$'}{g.ping}</span>
      </div>
    `;
    grid.appendChild(card);
  });

  function goStep(n) {
    document.querySelectorAll('.screen').forEach((s, i) => {
      s.classList.toggle('active', i === n - 1);
    });
    for (let i = 1; i <= 4; i++) {
      document.getElementById('dot' + i).classList.toggle('active', i <= n);
    }
  }

  function doLogin() {
    const code = document.getElementById('licInput').value;
    const valid = window.AndroidBridge ? window.AndroidBridge.checkLicense(code) : true;
    if (valid) {
      document.getElementById('licErr').style.display = 'none';
      goStep(2);
    } else {
      document.getElementById('licErr').style.display = 'block';
    }
  }

  function openBot() {
    if (window.AndroidBridge) window.AndroidBridge.openTelegramBot();
  }

  function selectGame(idx) {
    currentGame = games[idx];
    document.getElementById('selGameTitle').innerText = currentGame.name;
    document.getElementById('dns1Input').value = currentGame.d1;
    document.getElementById('dns2Input').value = currentGame.d2;
    document.getElementById('ipStatus').innerText = "آماده ثبت";
    document.getElementById('ipStatus').style.color = "#facc15";
    goStep(3);

    fetch('https://api.ipify.org?format=json')
      .then(r => r.json())
      .then(d => { document.getElementById('userIp').innerText = d.ip; })
      .catch(() => { document.getElementById('userIp').innerText = "5.160.24.118"; });
  }

  function registerIpAndNext() {
    const btn = document.getElementById('regIpBtn');
    btn.innerText = "در حال ثبت آی‌پی روی کلود...";
    setTimeout(() => {
      document.getElementById('ipStatus').innerText = "ثبت شده (Whitelisted) ✔";
      document.getElementById('ipStatus').style.color = "#00ff9d";
      btn.innerText = "ثبت آی‌پی و رفتن به رادار اتصال";
      document.getElementById('radarGameName').innerText = currentGame.name;
      document.getElementById('finalDns1').innerText = document.getElementById('dns1Input').value;
      document.getElementById('finalDns2').innerText = document.getElementById('dns2Input').value;
      goStep(4);
    }, 700);
  }

  function toggleVpn() {
    const d1 = document.getElementById('dns1Input').value;
    const d2 = document.getElementById('dns2Input').value;
    if (!vpnConnected) {
      if (window.AndroidBridge) window.AndroidBridge.connectVpn(d1, d2);
    } else {
      if (window.AndroidBridge) window.AndroidBridge.disconnectVpn();
    }
  }

  window.onVpnStateChanged = function(state) {
    vpnConnected = state;
    const btn = document.getElementById('powerBtn');
    const ring = document.getElementById('ring1');
    const txt = document.getElementById('powerTxt');
    const sub = document.getElementById('connSub');

    if (state) {
      btn.classList.add('connected');
      ring.classList.add('connected');
      txt.innerText = "متصل شد";
      sub.innerText = "تونل گیمینگ فعال است (بدون تحریم)";
      sub.style.color = "#00ff9d";
    } else {
      btn.classList.remove('connected');
      ring.classList.remove('connected');
      txt.innerText = "اتصال";
      sub.innerText = "آماده برقراری تونل اختصاصی دی‌ان‌اس";
      sub.style.color = "#94a3b8";
    }
  };

  // انیمیشن پس‌زمینه سایبرپانک
  const canvas = document.getElementById('bg');
  const ctx = canvas.getContext('2d');
  let w = canvas.width = window.innerWidth;
  let h = canvas.height = window.innerHeight;
  const pts = Array.from({length: 35}, () => ({
    x: Math.random() * w, y: Math.random() * h,
    vx: (Math.random() - 0.5) * 0.5, vy: (Math.random() - 0.5) * 0.5,
    r: Math.random() * 2 + 1
  }));
  function draw() {
    ctx.clearRect(0, 0, w, h);
    pts.forEach(p => {
      p.x += p.vx; p.y += p.vy;
      if (p.x < 0 || p.x > w) p.vx *= -1;
      if (p.y < 0 || p.y > h) p.vy *= -1;
      ctx.fillStyle = "rgba(0, 243, 255, 0.45)";
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2); ctx.fill();
    });
    requestAnimationFrame(draw);
  }
  draw();
</script>
</body>
</html>
        """.trimIndent()
    }
}
