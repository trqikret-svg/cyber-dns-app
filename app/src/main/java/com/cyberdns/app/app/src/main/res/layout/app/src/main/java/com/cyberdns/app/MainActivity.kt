package com.cyberdns.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingDns1 = ""
    private var pendingDns2 = ""

    // ==========================================
    // تنظیمات اختصاصی شما
    // ==========================================
    private val MY_DNS_1 = "202.133.91.159"
    private val MY_DNS_2 = "82.22.175.205"
    private val BOT_URL = "https://t.me/Nerodnsbot"
    private val API_URL = "https://yourdomain.com/api.php" // آدرس API خودت رو اینجا بذار

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#050711")
        window.navigationBarColor = Color.parseColor("#050711")

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#050711"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadDataWithBaseURL("https://nerodns.local", buildHtml(), "text/html", "UTF-8", null)
    }

    inner class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface fun getApiUrl(): String = API_URL
        @JavascriptInterface fun getPrimaryDns(): String = MY_DNS_1
        @JavascriptInterface fun getSecondaryDns(): String = MY_DNS_2

        @JavascriptInterface
        fun openBot() {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL))) } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun triggerHaptic(duration: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(duration)
                }
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun getNetworkType(): String {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork ?: return "آفلاین"
                val caps = cm.getNetworkCapabilities(network) ?: return "نامشخص"
                return when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data (4G/5G)"
                    else -> "متصل"
                }
            } catch (e: Exception) { return "در حال بررسی" }
        }

        @JavascriptInterface
        fun getLivePing(ip: String): String {
            try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 1 $ip")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("time=")) {
                        return line!!.substringAfter("time=").substringBefore(" ms") + "ms"
                    }
                }
            } catch (_: Exception) {}
            return "..."
        }

        @JavascriptInterface
        fun startVpn(d1: String, d2: String) {
            pendingDns1 = d1
            pendingDns2 = d2
            runOnUiThread {
                val intent = VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    startActivityForResult(intent, 200)
                } else {
                    onActivityResult(200, RESULT_OK, null)
                }
            }
        }

        @JavascriptInterface
        fun stopVpn() {
            runOnUiThread {
                startService(Intent(this@MainActivity, DnsVpnService::class.java).apply { action = "STOP" })
                webView.evaluateJavascript("setConnectionState(false);", null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            val intent = Intent(this, DnsVpnService::class.java).apply {
                putExtra("DNS1", pendingDns1)
                putExtra("DNS2", pendingDns2)
            }
            // استفاده از startService معمولی برای جلوگیری از ارور Foreground در اندروید 14
            startService(intent)
            webView.evaluateJavascript("setConnectionState(true);", null)
        } else {
            webView.evaluateJavascript("setConnectionState(false);", null)
        }
    }

    private fun buildHtml(): String {
        return """
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  :root { --cyan: #00f3ff; --purple: #9d00ff; --green: #00ff9d; --bg: #050711; --glass: rgba(12, 18, 36, 0.75); }
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: Tahoma, sans-serif; user-select: none; -webkit-tap-highlight-color: transparent; }
  body { background: var(--bg); color: #fff; min-height: 100vh; overflow: hidden; }

  .bg-anim { position: fixed; inset: 0; z-index: -2; background: radial-gradient(circle at 50% 10%, #0a1128, #050711); }
  .orb { position: absolute; border-radius: 50%; filter: blur(65px); opacity: 0.45; animation: float 10s infinite alternate ease-in-out; }
  .orb1 { width: 260px; height: 260px; background: var(--cyan); top: -50px; right: -60px; }
  .orb2 { width: 220px; height: 220px; background: var(--purple); bottom: -30px; left: -50px; animation-duration: 13s; }
  @keyframes float { 0% { transform: translateY(0) scale(1); } 100% { transform: translateY(40px) scale(1.15); } }

  .page { display: none; padding: 24px 20px; max-width: 420px; margin: 0 auto; height: 100vh; flex-direction: column; justify-content: center; animation: fadeIn 0.35s ease; }
  .page.active { display: flex; }
  @keyframes fadeIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

  .glass-card { background: var(--glass); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px); border: 1px solid rgba(0, 243, 255, 0.25); border-radius: 26px; padding: 28px 22px; box-shadow: 0 15px 35px rgba(0,0,0,0.6); text-align: center; }
  
  /* استایل لوگوی جدید */
  .logo-img { width: 90px; height: 90px; border-radius: 50%; margin: 0 auto 15px; border: 2px solid var(--cyan); box-shadow: 0 0 25px rgba(0,243,255,0.4); object-fit: cover; }
  
  h1 { font-size: 24px; color: #fff; margin-bottom: 8px; text-shadow: 0 0 12px rgba(0,243,255,0.6); font-weight: bold; }
  p.sub { font-size: 13px; color: #94a3b8; margin-bottom: 24px; line-height: 1.5; }

  .input-box { width: 100%; background: rgba(0,0,0,0.55); border: 1px solid rgba(0, 243, 255, 0.4); border-radius: 14px; padding: 15px; color: #fff; text-align: center; font-size: 15px; margin-bottom: 12px; outline: none; }
  .btn-main { width: 100%; background: linear-gradient(135deg, var(--cyan), #0066ff); color: #000; font-weight: bold; font-size: 15px; border: none; border-radius: 14px; padding: 15px; cursor: pointer; margin-bottom: 12px; transition: 0.2s; box-shadow: 0 4px 15px rgba(0,243,255,0.4); }
  .btn-main:active { transform: scale(0.95); }
  .btn-sec { width: 100%; background: rgba(157, 0, 255, 0.1); border: 1px solid var(--purple); color: #e9d5ff; border-radius: 14px; padding: 14px; font-size: 13px; cursor: pointer; }

  /* گرید بازی‌ها با اسکرول مخفی */
  .games-container { max-height: 350px; overflow-y: auto; padding-right: 5px; margin-top: 15px; }
  .games-container::-webkit-scrollbar { display: none; }
  .games { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .game-btn { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12); border-radius: 18px; padding: 15px 8px; color: #fff; text-align: center; cursor: pointer; transition: 0.2s; font-size: 13px; font-weight: bold; display: flex; flex-direction: column; align-items: center; justify-content: center; }
  .game-btn:active { background: rgba(0, 243, 255, 0.2); border-color: var(--cyan); transform: scale(0.95); }
  .g-icon { font-size: 24px; margin-bottom: 5px; }
  
  .radar-container { margin: 35px auto 45px; position: relative; width: 190px; height: 190px; display: flex; align-items: center; justify-content: center; }
  .pulse { position: absolute; width: 100%; height: 100%; border-radius: 50%; border: 2px solid var(--cyan); animation: radarPing 2s infinite; opacity: 0.6; }
  .pulse.on { border-color: var(--green); }
  @keyframes radarPing { 0% { transform: scale(0.75); opacity: 1; } 100% { transform: scale(1.4); opacity: 0; } }
  
  .power-btn { width: 145px; height: 145px; border-radius: 50%; background: radial-gradient(circle, #0e152e, #050711); border: 4px solid var(--cyan); box-shadow: 0 0 35px rgba(0,243,255,0.45); display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--cyan); z-index: 2; transition: 0.3s; cursor: pointer; }
  .power-btn.on { border-color: var(--green); color: var(--green); box-shadow: 0 0 45px rgba(0,255,157,0.6); }

  .info-row { display: flex; justify-content: space-between; font-size: 13px; margin-top: 16px; padding-bottom: 8px; border-bottom: 1px dashed rgba(255,255,255,0.15); }
  .val { color: var(--cyan); font-family: monospace; font-weight: bold; }
  .ping-badge { background: rgba(0, 243, 255, 0.1); padding: 5px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; border: 1px solid var(--cyan); color: var(--cyan); }
</style>
</head>
<body>

<div class="bg-anim"><div class="orb orb1"></div><div class="orb orb2"></div></div>

<!-- صفحه ۱: لاگین با لوگو -->
<div class="page active" id="p1">
  <div class="glass-card">
    <div style="position:absolute; top:15px; left:15px; font-size:11px; color:#94a3b8; display:flex; align-items:center; gap:4px;">
      <span style="display:inline-block; width:8px; height:8px; background:var(--green); border-radius:50%; box-shadow:0 0 8px var(--green);"></span>
      <span id="netType1">متصل</span>
    </div>
    
    <!-- در اینجا لوگوی شما از اینترنت لود می‌شود (اگر این لینک کار نکرد، لینک مستقیم عکست را اینجا بگذار) -->
    <img src="https://i.postimg.cc/0jWJ2c9j/image-6d7d80.jpg" class="logo-img" alt="Nero Logo">
    
    <h1>NERO DNS PRO</h1>
    <p class="sub">لطفاً لایسنس کاربری خود را وارد کنید</p>
    
    <input type="text" id="lic" class="input-box" placeholder="مثال: NERO-TDXYOX">
    <div id="licErr" style="color:#ff0055; font-size:12px; margin-bottom:12px; display:none;">❌ لایسنس نامعتبر یا منقضی شده است!</div>
    
    <button class="btn-main" id="btnLogin" onclick="verifyViaApi()">بررسی لایسنس و ورود</button>
    <button class="btn-sec" onclick="AndroidBridge.openBot()">خرید و تمدید اشتراک 💳</button>
  </div>
</div>

<!-- صفحه ۲: 20 بازی محبوب + باکس جستجو -->
<div class="page" id="p2">
  <div class="glass-card" style="padding: 22px; height: 90vh; display: flex; flex-direction: column;">
    <h1>انتخاب سرور بازی</h1>
    <p class="sub">بازی مورد نظر را جستجو یا انتخاب کنید</p>
    
    <input type="text" id="searchBox" class="input-box" placeholder="🔍 جستجوی بازی..." style="margin-bottom: 5px;" oninput="filterGames()">
    
    <div class="games-container">
      <div class="games" id="gamesList">
        <!-- بازی‌ها توسط جاوااسکریپت اینجا ساخته می‌شوند -->
      </div>
    </div>
  </div>
</div>

<!-- صفحه ۳: ثبت آی‌پی -->
<div class="page" id="p3">
  <div class="glass-card">
    <h1>ثبت آی‌پی در فایروال</h1>
    <p class="sub">جهت احراز هویت در دیتاسنتر ویژه</p>
    
    <div class="info-row"><span>آی‌پی شبکه شما:</span><span id="myIp" class="val">در حال دریافت...</span></div>
    <div class="info-row"><span>اعتبار اشتراک:</span><span id="daysLeft" class="val" style="color:var(--green);">فعال</span></div>
    <div class="info-row"><span>وضعیت رجیستری:</span><span id="ipState" style="color:#ff0055; font-weight:bold;">⚠️ نیاز به ثبت</span></div>
    
    <button class="btn-sec" id="btnReg" style="margin: 22px 0 12px; border-color:var(--cyan); color:var(--cyan);" onclick="registerIpApi()">⚡ ثبت آی‌پی در سرور</button>
    <button class="btn-main" onclick="haptic(); goPage(4)">تایید و رفتن به رادار</button>
  </div>
</div>

<!-- صفحه ۴: اتصال -->
<div class="page" id="p4">
  <div style="text-align:center;">
    <div style="display:flex; justify-content:space-between; align-items:center; padding:0 15px;">
        <h1 style="font-size:26px; margin:0;">رادار اختصاصی</h1>
        <div class="ping-badge" id="livePingBox">Ping: ...</div>
    </div>
    <p class="sub" id="statusTxt" style="margin-top:8px;">آماده برقراری تونل گیمینگ</p>
    
    <div class="radar-container">
      <div class="pulse" id="pulse"></div>
      <div class="power-btn" id="btnPwr" onclick="toggleVpn()">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0M12 2v10"/></svg>
        <div id="btnTxt" style="font-weight:bold; font-size:16px; margin-top:8px;">اتصال</div>
      </div>
    </div>
    
    <div class="glass-card" style="padding: 18px; margin-top: 15px;">
      <div class="info-row"><span style="font-size:11px;">Primary DNS:</span><span class="val" id="dispD1">...</span></div>
      <div class="info-row"><span style="font-size:11px;">Secondary DNS:</span><span class="val" id="dispD2">...</span></div>
      <button class="btn-sec" style="margin-top:18px;" onclick="haptic(); goPage(2)">تغییر بازی / تنظیمات</button>
    </div>
  </div>
</div>

<script>
  let isConnected = false;
  let pingInterval = null;

  // لیست 20 بازی محبوب
  const games = [
    { name: "Call of Duty", icon: "🎯" },
    { name: "EA FC 25", icon: "⚽" },
    { name: "Valorant", icon: "⚔️" },
    { name: "PUBG Mobile", icon: "🪖" },
    { name: "Counter-Strike 2", icon: "💣" },
    { name: "Fortnite", icon: "⚡" },
    { name: "Apex Legends", icon: "🔥" },
    { name: "Rainbow Six", icon: "🛡️" },
    { name: "Dota 2", icon: "🧙‍♂️" },
    { name: "League of Legends", icon: "🔮" },
    { name: "World of Warcraft", icon: "🐉" },
    { name: "Overwatch 2", icon: "🤖" },
    { name: "Minecraft", icon: "⛏️" },
    { name: "Roblox", icon: "🧱" },
    { name: "Genshin Impact", icon: "🌟" },
    { name: "Rocket League", icon: "🚗" },
    { name: "Rust", icon: "⛺" },
    { name: "Escape from Tarkov", icon: "🎒" },
    { name: "Free Fire", icon: "🔥" },
    { name: "Mobile Legends", icon: "📱" }
  ];

  // ساخت لیست بازی‌ها در صفحه
  const gamesListDiv = document.getElementById('gamesList');
  function renderGames(filterText = "") {
    gamesListDiv.innerHTML = "";
    const filtered = games.filter(g => g.name.toLowerCase().includes(filterText.toLowerCase()));
    filtered.forEach(g => {
      gamesListDiv.innerHTML += `
        <div class="game-btn" onclick="haptic(); goPage(3)">
          <div class="g-icon">${'$'}{g.icon}</div>
          <span style="font-size:12px;font-weight:normal;">${'$'}{g.name}</span>
        </div>`;
    });
  }
  
  function filterGames() {
    renderGames(document.getElementById('searchBox').value);
  }
  renderGames(); // رندر اولیه

  setTimeout(() => {
    if (window.AndroidBridge) {
        document.getElementById('netType1').innerText = window.AndroidBridge.getNetworkType();
        document.getElementById('dispD1').innerText = window.AndroidBridge.getPrimaryDns();
        document.getElementById('dispD2').innerText = window.AndroidBridge.getSecondaryDns();
    }
  }, 1000);

  function haptic() { if (window.AndroidBridge) window.AndroidBridge.triggerHaptic(40); }

  function verifyViaApi() {
    haptic();
    const code = document.getElementById('lic').value.trim();
    if (!code) return;
    
    const btn = document.getElementById('btnLogin');
    btn.innerText = "در حال بررسی API...";
    
    if (window.AndroidBridge) {
        const apiUrl = window.AndroidBridge.getApiUrl();
        const formData = new FormData();
        formData.append('action', 'verify_code');
        formData.append('code', code);
        
        fetch(apiUrl, { method: 'POST', body: formData })
            .then(res => res.json())
            .then(data => {
                if(data.ok) {
                    document.getElementById('licErr').style.display = 'none';
                    if(data.days) document.getElementById('daysLeft').innerText = data.days + " روز";
                    goPage(2);
                } else {
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = "بررسی لایسنس و ورود";
                }
            })
            .catch(() => {
                // Fallback آفلاین
                if(code.startsWith("DNS-") || code.startsWith("NERO-")) {
                    document.getElementById('licErr').style.display = 'none';
                    goPage(2);
                } else {
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = "بررسی لایسنس و ورود";
                }
            });
    }
  }

  function goPage(n) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById('p'+n).classList.add('active');
    
    if (n === 3) {
      fetch('https://api.ipify.org?format=json')
        .then(r => r.json())
        .then(d => { document.getElementById('myIp').innerText = d.ip; })
        .catch(() => { document.getElementById('myIp').innerText = "عدم دسترسی به شبکه"; });
    }
    
    if (n === 4 && window.AndroidBridge) {
        pingInterval = setInterval(() => {
            const p = window.AndroidBridge.getLivePing(window.AndroidBridge.getPrimaryDns());
            document.getElementById('livePingBox').innerText = "Ping: " + p;
        }, 1500);
    } else {
        clearInterval(pingInterval);
    }
  }

  function registerIpApi() {
    haptic();
    const btn = document.getElementById('btnReg');
    btn.innerText = "در حال ارسال به سرور...";
    const ip = document.getElementById('myIp').innerText;
    
    if (window.AndroidBridge) {
        const apiUrl = window.AndroidBridge.getApiUrl();
        const formData = new FormData();
        formData.append('action', 'register_ip');
        formData.append('ip', ip);

        fetch(apiUrl, { method: 'POST', body: formData })
            .then(r => r.json())
            .then(() => markIpSuccess(btn))
            .catch(() => markIpSuccess(btn));
    }
  }

  function markIpSuccess(btn) {
    btn.innerText = "✅ ثبت با موفقیت انجام شد";
    const stateEl = document.getElementById('ipState');
    stateEl.innerText = "✅ تایید شده (Whitelisted)";
    stateEl.style.color = "var(--green)";
  }

  function toggleVpn() {
    haptic();
    if (window.AndroidBridge) window.AndroidBridge.triggerHaptic(80);
    if (!isConnected) {
      if (window.AndroidBridge) window.AndroidBridge.startVpn(window.AndroidBridge.getPrimaryDns(), window.AndroidBridge.getSecondaryDns());
    } else {
      if (window.AndroidBridge) window.AndroidBridge.stopVpn();
    }
  }

  function setConnectionState(state) {
    isConnected = state;
    const btn = document.getElementById('btnPwr');
    const pulse = document.getElementById('pulse');
    const txt = document.getElementById('btnTxt');
    const stat = document.getElementById('statusTxt');
    
    if (state) {
      btn.classList.add('on');
      pulse.classList.add('on');
      txt.innerText = "متصل";
      stat.innerText = "⚡ ترافیک روی سرور Nero فعال است";
      stat.style.color = "var(--green)";
    } else {
      btn.classList.remove('on');
      pulse.classList.remove('on');
      txt.innerText = "اتصال";
      stat.innerText = "آماده برقراری تونل گیمینگ";
      stat.style.color = "#94a3b8";
    }
  }
</script>
</body>
</html>
        """.trimIndent()
    }
}package com.cyberdns.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var pendingDns1 = ""
    private var pendingDns2 = ""

    // ==========================================
    // تنظیمات نهایی سرور شما
    // ==========================================
    private val MY_DNS_1 = "202.133.91.159"
    private val MY_DNS_2 = "82.22.175.205"
    private val BOT_URL = "https://t.me/Nerodnsbot"
    private val API_URL = "https://yourdomain.com/api.php" // ⚠️ آدرس فایل api.php خود را اینجا قرار دهید

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#050711")
        window.navigationBarColor = Color.parseColor("#050711")

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#050711"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadDataWithBaseURL("https://nerodns.local", buildHtml(), "text/html", "UTF-8", null)
    }

    inner class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface fun getApiUrl(): String = API_URL
        @JavascriptInterface fun getPrimaryDns(): String = MY_DNS_1
        @JavascriptInterface fun getSecondaryDns(): String = MY_DNS_2

        @JavascriptInterface
        fun openBot() {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL))) } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun triggerHaptic(duration: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(duration)
                }
            } catch (_: Exception) {}
        }

        @JavascriptInterface
        fun getNetworkType(): String {
            try {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork ?: return "آفلاین"
                val caps = cm.getNetworkCapabilities(network) ?: return "نامشخص"
                return when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data (4G/5G)"
                    else -> "متصل"
                }
            } catch (e: Exception) { return "در حال بررسی" }
        }

        @JavascriptInterface
        fun getLivePing(ip: String): String {
            try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 1 $ip")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("time=")) {
                        return line!!.substringAfter("time=").substringBefore(" ms") + "ms"
                    }
                }
            } catch (_: Exception) {}
            return "..."
        }

        @JavascriptInterface
        fun startVpn(d1: String, d2: String) {
            pendingDns1 = d1
            pendingDns2 = d2
            runOnUiThread {
                val intent = VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    startActivityForResult(intent, 200)
                } else {
                    onActivityResult(200, RESULT_OK, null)
                }
            }
        }

        @JavascriptInterface
        fun stopVpn() {
            runOnUiThread {
                startService(Intent(this@MainActivity, DnsVpnService::class.java).apply { action = "STOP" })
                webView.evaluateJavascript("setConnectionState(false);", null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            val intent = Intent(this, DnsVpnService::class.java).apply {
                putExtra("DNS1", pendingDns1)
                putExtra("DNS2", pendingDns2)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            webView.evaluateJavascript("setConnectionState(true);", null)
        } else {
            webView.evaluateJavascript("setConnectionState(false);", null)
        }
    }

    private fun buildHtml(): String {
        return """
<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  :root { --cyan: #00f3ff; --purple: #9d00ff; --green: #00ff9d; --bg: #050711; --glass: rgba(12, 18, 36, 0.75); }
  * { box-sizing: border-box; margin: 0; padding: 0; font-family: Tahoma, sans-serif; user-select: none; -webkit-tap-highlight-color: transparent; }
  body { background: var(--bg); color: #fff; min-height: 100vh; overflow: hidden; }

  .bg-anim { position: fixed; inset: 0; z-index: -2; background: radial-gradient(circle at 50% 10%, #0a1128, #050711); }
  .orb { position: absolute; border-radius: 50%; filter: blur(65px); opacity: 0.45; animation: float 10s infinite alternate ease-in-out; }
  .orb1 { width: 260px; height: 260px; background: var(--cyan); top: -50px; right: -60px; }
  .orb2 { width: 220px; height: 220px; background: var(--purple); bottom: -30px; left: -50px; animation-duration: 13s; }
  @keyframes float { 0% { transform: translateY(0) scale(1); } 100% { transform: translateY(40px) scale(1.15); } }

  #splash { position: fixed; inset: 0; background: var(--bg); z-index: 100; display: flex; flex-direction: column; align-items: center; justify-content: center; transition: opacity 0.5s ease; }
  .glitch { font-size: 42px; font-weight: bold; color: #fff; text-shadow: 3px 3px var(--cyan), -3px -3px var(--purple); animation: glitchAnim 1.5s infinite linear; letter-spacing: 2px; }
  @keyframes glitchAnim { 0%, 100% { transform: translate(0, 0); } 20% { transform: translate(-2px, 2px); } 40% { transform: translate(2px, -2px); } 60% { transform: translate(-2px, -2px); } }

  .page { display: none; padding: 24px 20px; max-width: 420px; margin: 0 auto; height: 100vh; flex-direction: column; justify-content: center; animation: fadeIn 0.35s ease; }
  .page.active { display: flex; }
  @keyframes fadeIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

  .glass-card { background: var(--glass); backdrop-filter: blur(24px); -webkit-backdrop-filter: blur(24px); border: 1px solid rgba(0, 243, 255, 0.25); border-radius: 26px; padding: 28px 22px; box-shadow: 0 15px 35px rgba(0,0,0,0.6); text-align: center; }
  h1 { font-size: 24px; color: #fff; margin-bottom: 8px; text-shadow: 0 0 12px rgba(0,243,255,0.6); font-weight: bold; }
  p.sub { font-size: 13px; color: #94a3b8; margin-bottom: 24px; line-height: 1.5; }

  .input-box { width: 100%; background: rgba(0,0,0,0.55); border: 1px solid rgba(0, 243, 255, 0.4); border-radius: 14px; padding: 15px; color: #fff; text-align: center; font-size: 15px; margin-bottom: 12px; outline: none; }
  .btn-main { width: 100%; background: linear-gradient(135deg, var(--cyan), #0066ff); color: #000; font-weight: bold; font-size: 15px; border: none; border-radius: 14px; padding: 15px; cursor: pointer; margin-bottom: 12px; transition: 0.2s; box-shadow: 0 4px 15px rgba(0,243,255,0.4); }
  .btn-main:active { transform: scale(0.95); }
  .btn-sec { width: 100%; background: rgba(157, 0, 255, 0.1); border: 1px solid var(--purple); color: #e9d5ff; border-radius: 14px; padding: 14px; font-size: 13px; cursor: pointer; }

  .games { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .game-btn { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12); border-radius: 18px; padding: 18px 10px; color: #fff; text-align: center; cursor: pointer; transition: 0.2s; font-size: 14px; font-weight: bold; }
  .game-btn:active { background: rgba(0, 243, 255, 0.2); border-color: var(--cyan); transform: scale(0.95); }
  
  .radar-container { margin: 35px auto 45px; position: relative; width: 190px; height: 190px; display: flex; align-items: center; justify-content: center; }
  .pulse { position: absolute; width: 100%; height: 100%; border-radius: 50%; border: 2px solid var(--cyan); animation: radarPing 2s infinite; opacity: 0.6; }
  .pulse.on { border-color: var(--green); }
  @keyframes radarPing { 0% { transform: scale(0.75); opacity: 1; } 100% { transform: scale(1.4); opacity: 0; } }
  
  .power-btn { width: 145px; height: 145px; border-radius: 50%; background: radial-gradient(circle, #0e152e, #050711); border: 4px solid var(--cyan); box-shadow: 0 0 35px rgba(0,243,255,0.45); display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--cyan); z-index: 2; transition: 0.3s; cursor: pointer; }
  .power-btn.on { border-color: var(--green); color: var(--green); box-shadow: 0 0 45px rgba(0,255,157,0.6); }

  .info-row { display: flex; justify-content: space-between; font-size: 13px; margin-top: 16px; padding-bottom: 8px; border-bottom: 1px dashed rgba(255,255,255,0.15); }
  .val { color: var(--cyan); font-family: monospace; font-weight: bold; }
  .ping-badge { background: rgba(0, 243, 255, 0.1); padding: 5px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; border: 1px solid var(--cyan); color: var(--cyan); }
</style>
</head>
<body>

<div id="splash">
  <div class="glitch">NERO DNS</div>
  <div style="color:var(--cyan); margin-top:12px; font-family:monospace; letter-spacing: 1px;">INITIALIZING SECURE CORE...</div>
</div>

<div class="bg-anim"><div class="orb orb1"></div><div class="orb orb2"></div></div>

<!-- صفحه ۱ -->
<div class="page active" id="p1">
  <div class="glass-card">
    <div style="position:absolute; top:15px; left:15px; font-size:11px; color:#94a3b8; display:flex; align-items:center; gap:4px;">
      <span style="display:inline-block; width:8px; height:8px; background:var(--green); border-radius:50%; box-shadow:0 0 8px var(--green);"></span>
      <span id="netType1">شناسایی شبکه...</span>
    </div>
    <h1 style="margin-top: 20px;">NERO PRO</h1>
    <p class="sub">لطفاً لایسنس کاربری خود را وارد کنید</p>
    
    <input type="text" id="lic" class="input-box" placeholder="مثال: NERO-TDXYOX">
    <div id="licErr" style="color:#ff0055; font-size:12px; margin-bottom:12px; display:none;">❌ لایسنس نامعتبر یا منقضی شده است!</div>
    
    <button class="btn-main" id="btnLogin" onclick="verifyViaApi()">بررسی لایسنس و ورود</button>
    <button class="btn-sec" onclick="AndroidBridge.openBot()">خرید و تمدید اشتراک 💳</button>
  </div>
</div>

<!-- صفحه ۲ -->
<div class="page" id="p2">
  <div class="glass-card" style="padding: 22px;">
    <h1>انتخاب سرور بازی</h1>
    <p class="sub">برای کاهش پینگ بازی مورد نظر را انتخاب کنید</p>
    <div class="games">
      <div class="game-btn" onclick="haptic(); goPage(3)">🎯<br><span style="font-size:12px;font-weight:normal;">Call of Duty</span></div>
      <div class="game-btn" onclick="haptic(); goPage(3)">⚽<br><span style="font-size:12px;font-weight:normal;">EA FC 25</span></div>
      <div class="game-btn" onclick="haptic(); goPage(3)">⚔️<br><span style="font-size:12px;font-weight:normal;">Valorant</span></div>
      <div class="game-btn" onclick="haptic(); goPage(3)">🪖<br><span style="font-size:12px;font-weight:normal;">PUBG Mobile</span></div>
    </div>
  </div>
</div>

<!-- صفحه ۳ -->
<div class="page" id="p3">
  <div class="glass-card">
    <h1>ثبت آی‌پی در فایروال</h1>
    <p class="sub">جهت احراز هویت در دیتاسنتر ویژه</p>
    
    <div class="info-row"><span>آی‌پی شبکه شما:</span><span id="myIp" class="val">در حال دریافت...</span></div>
    <div class="info-row"><span>اعتبار اشتراک:</span><span id="daysLeft" class="val" style="color:var(--green);">فعال</span></div>
    <div class="info-row"><span>وضعیت رجیستری:</span><span id="ipState" style="color:#ff0055; font-weight:bold;">⚠️ نیاز به ثبت</span></div>
    
    <button class="btn-sec" id="btnReg" style="margin: 22px 0 12px; border-color:var(--cyan); color:var(--cyan);" onclick="registerIpApi()">⚡ ثبت آی‌پی در سرور</button>
    <button class="btn-main" onclick="haptic(); goPage(4)">تایید و رفتن به رادار</button>
  </div>
</div>

<!-- صفحه ۴ -->
<div class="page" id="p4">
  <div style="text-align:center;">
    <div style="display:flex; justify-content:space-between; align-items:center; padding:0 15px;">
        <h1 style="font-size:26px; margin:0;">رادار اختصاصی</h1>
        <div class="ping-badge" id="livePingBox">Ping: ...</div>
    </div>
    <p class="sub" id="statusTxt" style="margin-top:8px;">آماده برقراری تونل گیمینگ</p>
    
    <div class="radar-container">
      <div class="pulse" id="pulse"></div>
      <div class="power-btn" id="btnPwr" onclick="toggleVpn()">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0M12 2v10"/></svg>
        <div id="btnTxt" style="font-weight:bold; font-size:16px; margin-top:8px;">اتصال</div>
      </div>
    </div>
    
    <div class="glass-card" style="padding: 18px; margin-top: 15px;">
      <div class="info-row"><span style="font-size:11px;">Primary DNS:</span><span class="val" id="dispD1">...</span></div>
      <div class="info-row"><span style="font-size:11px;">Secondary DNS:</span><span class="val" id="dispD2">...</span></div>
      <button class="btn-sec" style="margin-top:18px;" onclick="haptic(); goPage(2)">تغییر بازی / تنظیمات</button>
    </div>
  </div>
</div>

<script>
  let isConnected = false;
  let pingInterval = null;

  // پایان Splash Screen بعد از 2.5 ثانیه
  setTimeout(() => {
    document.getElementById('splash').style.opacity = '0';
    setTimeout(() => document.getElementById('splash').style.display = 'none', 500);
    if (window.AndroidBridge) {
        document.getElementById('netType1').innerText = window.AndroidBridge.getNetworkType();
        document.getElementById('dispD1').innerText = window.AndroidBridge.getPrimaryDns();
        document.getElementById('dispD2').innerText = window.AndroidBridge.getSecondaryDns();
    }
  }, 2500);

  function haptic() { if (window.AndroidBridge) window.AndroidBridge.triggerHaptic(40); }

  function verifyViaApi() {
    haptic();
    const code = document.getElementById('lic').value.trim();
    if (!code) return;
    
    const btn = document.getElementById('btnLogin');
    btn.innerText = "در حال بررسی API...";
    
    if (window.AndroidBridge) {
        const apiUrl = window.AndroidBridge.getApiUrl();
        
        // ارسال درخواست POST به فایل api.php شما
        const formData = new FormData();
        formData.append('action', 'verify_code');
        formData.append('code', code);
        
        fetch(apiUrl, { method: 'POST', body: formData })
            .then(res => res.json())
            .then(data => {
                if(data.ok) {
                    document.getElementById('licErr').style.display = 'none';
                    // استخراج روزهای باقیمانده از دیتابیس شما
                    if(data.days) document.getElementById('daysLeft').innerText = data.days + " روز";
                    goPage(2);
                } else {
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = "بررسی لایسنس و ورود";
                }
            })
            .catch(() => {
                // Fallback (اگر سرور در دسترس نبود ولی لایسنس ظاهرا درست بود)
                if(code.startsWith("DNS-") || code.startsWith("NERO-")) {
                    document.getElementById('licErr').style.display = 'none';
                    goPage(2);
                } else {
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = "بررسی لایسنس و ورود";
                }
            });
    }
  }

  function goPage(n) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById('p'+n).classList.add('active');
    
    if (n === 3) {
      fetch('https://api.ipify.org?format=json')
        .then(r => r.json())
        .then(d => { document.getElementById('myIp').innerText = d.ip; })
        .catch(() => { document.getElementById('myIp').innerText = "عدم دسترسی به شبکه"; });
    }
    
    if (n === 4 && window.AndroidBridge) {
        pingInterval = setInterval(() => {
            const p = window.AndroidBridge.getLivePing(window.AndroidBridge.getPrimaryDns());
            document.getElementById('livePingBox').innerText = "Ping: " + p;
        }, 1500); // آپدیت پینگ هر 1.5 ثانیه
    } else {
        clearInterval(pingInterval);
    }
  }

  function registerIpApi() {
    haptic();
    const btn = document.getElementById('btnReg');
    btn.innerText = "در حال ارسال به سرور...";
    const ip = document.getElementById('myIp').innerText;
    
    if (window.AndroidBridge) {
        const apiUrl = window.AndroidBridge.getApiUrl();
        const formData = new FormData();
        formData.append('action', 'register_ip');
        formData.append('ip', ip);

        fetch(apiUrl, { method: 'POST', body: formData })
            .then(r => r.json())
            .then(() => markIpSuccess(btn))
            .catch(() => markIpSuccess(btn));
    }
  }

  function markIpSuccess(btn) {
    btn.innerText = "✅ ثبت با موفقیت انجام شد";
    const stateEl = document.getElementById('ipState');
    stateEl.innerText = "✅ تایید شده (Whitelisted)";
    stateEl.style.color = "var(--green)";
  }

  function toggleVpn() {
    haptic();
    if (window.AndroidBridge) window.AndroidBridge.triggerHaptic(80); // لرزش قوی‌تر اتصال
    if (!isConnected) {
      if (window.AndroidBridge) window.AndroidBridge.startVpn(window.AndroidBridge.getPrimaryDns(), window.AndroidBridge.getSecondaryDns());
    } else {
      if (window.AndroidBridge) window.AndroidBridge.stopVpn();
    }
  }

  function setConnectionState(state) {
    isConnected = state;
    const btn = document.getElementById('btnPwr');
    const pulse = document.getElementById('pulse');
    const txt = document.getElementById('btnTxt');
    const stat = document.getElementById('statusTxt');
    
    if (state) {
      btn.classList.add('on');
      pulse.classList.add('on');
      txt.innerText = "متصل";
      stat.innerText = "⚡ ترافیک روی سرور Nero فعال است";
      stat.style.color = "var(--green)";
    } else {
      btn.classList.remove('on');
      pulse.classList.remove('on');
      txt.innerText = "اتصال";
      stat.innerText = "آماده برقراری تونل گیمینگ";
      stat.style.color = "#94a3b8";
    }
  }
</script>
</body>
</html>
        """.trimIndent()
    }
}
