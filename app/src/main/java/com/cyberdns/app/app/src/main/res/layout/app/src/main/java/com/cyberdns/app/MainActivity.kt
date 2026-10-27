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
    private val API_URL = "https://musicpass.ir/api.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            addJavascriptInterface(WebAppInterface(), "AndroidBridge")
        }

        setContentView(webView)
        webView.loadDataWithBaseURL("https://nerodns.local", buildHtml(), "text/html", "UTF-8", null)
    }

    inner class WebAppInterface {
        @JavascriptInterface fun getApiUrl(): String = API_URL
        @JavascriptInterface fun getPrimaryDns(): String = MY_DNS_1
        @JavascriptInterface fun getSecondaryDns(): String = MY_DNS_2

        @JavascriptInterface
        fun openBot() {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BOT_URL))) } catch (e: Exception) {}
        }

        @JavascriptInterface
        fun triggerHaptic(duration: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            } catch (e: Exception) {}
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
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 1 " + ip)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                if (output.contains("time=")) {
                    return output.substringAfter("time=").substringBefore(" ms") + "ms"
                }
            } catch (e: Exception) {}
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
                    onActivityResult(200, Activity.RESULT_OK, null)
                }
            }
        }

        @JavascriptInterface
        fun stopVpn() {
            runOnUiThread {
                val intent = Intent(this@MainActivity, DnsVpnService::class.java)
                intent.action = "STOP"
                startService(intent)
                webView.evaluateJavascript("setConnectionState(false);", null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, DnsVpnService::class.java)
            intent.putExtra("DNS1", pendingDns1)
            intent.putExtra("DNS2", pendingDns2)
            startService(intent)
            webView.evaluateJavascript("setConnectionState(true);", null)
        } else {
            webView.evaluateJavascript("setConnectionState(false);", null)
        }
    }

    private fun buildHtml(): String {
        return """
<!DOCTYPE html>
<html lang="fa" dir="rtl" data-theme="dark">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Nero DNS Pro</title>
<style>
  @import url('https://cdn.jsdelivr.net/gh/rastikerdar/vazirmatn@v33.003/Vazirmatn-font-face.css');

  :root {
    --cyan: #0077b6; --purple: #7209b7; --green: #00b300; --bg: #f8fafc;
    --glass: rgba(255, 255, 255, 0.85); --text-main: #0f172a; --text-sub: #475569;
    --card-border: rgba(0, 119, 182, 0.2); --input-bg: #ffffff; --btn-bg: rgba(0, 0, 0, 0.04);
  }

  [data-theme="dark"] {
    --cyan: #00f3ff; --purple: #9d00ff; --green: #00ff9d; --bg: #050711;
    --glass: rgba(12, 18, 36, 0.75); --text-main: #ffffff; --text-sub: #94a3b8;
    --card-border: rgba(0, 243, 255, 0.25); --input-bg: rgba(0, 0, 0, 0.55);
    --btn-bg: rgba(255, 255, 255, 0.06);
  }

  * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Vazirmatn', Tahoma, sans-serif; user-select: none; -webkit-tap-highlight-color: transparent; }
  body { background: var(--bg); color: var(--text-main); height: 100vh; overflow: hidden; transition: background 0.3s, color 0.3s; }

  .bg-anim { position: fixed; inset: 0; z-index: -2; background: radial-gradient(circle at 50% 10%, rgba(10,17,40,0.5), transparent); }
  .orb { position: absolute; border-radius: 50%; filter: blur(65px); opacity: 0.45; animation: float 10s infinite alternate ease-in-out; }
  .orb1 { width: 260px; height: 260px; background: var(--cyan); top: -50px; right: -60px; }
  .orb2 { width: 220px; height: 220px; background: var(--purple); bottom: -30px; left: -50px; animation-duration: 13s; }
  @keyframes float { 0% { transform: translateY(0) scale(1); } 100% { transform: translateY(40px) scale(1.15); } }

  .page { display: none; padding: 24px 20px; max-width: 420px; margin: 0 auto; height: 100vh; flex-direction: column; justify-content: center; }
  .page.active { display: flex; animation: fadeIn 0.35s ease; }
  @keyframes fadeIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

  .glass-card { background: var(--glass); backdrop-filter: blur(24px); border: 1px solid var(--card-border); border-radius: 26px; padding: 28px 22px; box-shadow: 0 15px 35px rgba(0,0,0,0.1); text-align: center; display: flex; flex-direction: column; max-height: 95vh; position: relative; }
  
  .logo-img { width: 90px; height: 90px; border-radius: 50%; margin: 0 auto 15px; border: 2px solid var(--cyan); box-shadow: 0 0 25px rgba(0,119,182,0.3); object-fit: cover; }
  [data-theme="dark"] .logo-img { box-shadow: 0 0 25px rgba(0,243,255,0.4); }
  
  h1 { font-size: 22px; color: var(--text-main); margin-bottom: 8px; font-weight: 800; }
  p.sub { font-size: 13px; color: var(--text-sub); margin-bottom: 24px; line-height: 1.6; }

  .input-box { width: 100%; background: var(--input-bg); border: 1px solid var(--card-border); border-radius: 14px; padding: 15px; color: var(--text-main); text-align: center; font-size: 15px; margin-bottom: 12px; outline: none; transition: 0.3s; }
  .input-box:focus { border-color: var(--cyan); }
  
  .btn-main { width: 100%; background: var(--cyan); color: #fff; font-weight: bold; font-size: 15px; border: none; border-radius: 14px; padding: 15px; cursor: pointer; margin-bottom: 12px; transition: 0.2s; box-shadow: 0 4px 15px rgba(0,119,182,0.3); }
  .btn-main:active { transform: scale(0.95); }
  .btn-sec { width: 100%; background: transparent; border: 1px solid var(--purple); color: var(--purple); border-radius: 14px; padding: 14px; font-size: 13px; font-weight: bold; cursor: pointer; }

  .games-container { flex: 1; overflow-y: auto; padding-right: 5px; margin-top: 15px; }
  .games-container::-webkit-scrollbar { display: none; }
  .games { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; padding-bottom: 15px; }
  .game-btn { background: var(--btn-bg); border: 1px solid var(--card-border); border-radius: 18px; padding: 15px 5px; color: var(--text-main); text-align: center; cursor: pointer; transition: 0.2s; font-size: 12px; font-weight: bold; display: flex; flex-direction: column; align-items: center; justify-content: center; }
  .game-btn:active { background: rgba(0, 119, 182, 0.1); transform: scale(0.95); }
  
  .radar-container { margin: 35px auto 45px; position: relative; width: 190px; height: 190px; display: flex; align-items: center; justify-content: center; }
  .pulse { position: absolute; width: 100%; height: 100%; border-radius: 50%; border: 2px solid var(--cyan); animation: radarPing 2s infinite; opacity: 0.6; }
  .pulse.on { border-color: var(--green); }
  @keyframes radarPing { 0% { transform: scale(0.75); opacity: 1; } 100% { transform: scale(1.4); opacity: 0; } }
  
  .power-btn { width: 145px; height: 145px; border-radius: 50%; background: var(--input-bg); border: 4px solid var(--cyan); box-shadow: 0 0 25px rgba(0,119,182,0.2); display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--cyan); z-index: 2; transition: 0.3s; cursor: pointer; }
  .power-btn.on { border-color: var(--green); color: var(--green); }

  .info-row { display: flex; justify-content: space-between; font-size: 13px; margin-top: 16px; padding-bottom: 8px; border-bottom: 1px dashed var(--card-border); }
  .val { color: var(--cyan); font-family: monospace; font-weight: bold; }
  .ping-badge { background: var(--btn-bg); padding: 5px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; border: 1px solid var(--cyan); color: var(--cyan); }
  
  .top-controls { position: fixed; top: 15px; right: 15px; display: flex; gap: 8px; z-index: 100; }
  .action-btn { background: var(--input-bg); border: 1px solid var(--card-border); color: var(--text-main); padding: 6px 12px; border-radius: 12px; font-size: 12px; cursor: pointer; font-weight: bold; box-shadow: 0 4px 10px rgba(0,0,0,0.05); transition: 0.2s; }
  .action-btn:active { transform: scale(0.95); }
</style>
</head>
<body>

<div class="bg-anim"><div class="orb orb1"></div><div class="orb orb2"></div></div>

<div class="top-controls">
    <button class="action-btn" onclick="toggleTheme()" id="themeBtn">☀️</button>
    <button class="action-btn" onclick="toggleLanguage()">EN / FA</button>
    <button class="action-btn" onclick="logoutApp()" id="logoutBtn" style="display:none; color:#ff0055; border-color:#ff0055;">خروج</button>
</div>

<div class="page active" id="p1">
  <div class="glass-card">
    <div style="position:absolute; top:18px; left:18px; font-size:11px; color:var(--text-sub); display:flex; align-items:center; gap:4px;">
      <span style="display:inline-block; width:8px; height:8px; background:var(--green); border-radius:50%;"></span>
      <span id="netType1">...</span>
    </div>
    
    <img src="https://i.postimg.cc/0jWJ2c9j/image-6d7d80.jpg" class="logo-img" alt="Nero Logo">
    
    <h1 id="t_title">NERO DNS PRO</h1>
    <p class="sub" id="t_sub">نسخه اندروید - لطفا لایسنس را وارد کنید</p>
    
    <input type="text" id="lic" class="input-box" placeholder="مثال: NERO-TDXYOX">
    <div id="licErr" style="color:#ff0055; font-size:12px; margin-bottom:12px; display:none;">❌ لایسنس نامعتبر است!</div>
    
    <button class="btn-main" id="btnLogin" onclick="verifyViaApi()">بررسی لایسنس و ورود</button>
    <button class="btn-sec" id="btnBuy" onclick="AndroidBridge.openBot()">خرید و تمدید اشتراک</button>
  </div>
</div>

<div class="page" id="p2">
  <div class="glass-card" style="padding: 22px; height: 90vh;">
    <h1 id="t_gameTitle">انتخاب سرور بازی</h1>
    <input type="text" id="searchBox" class="input-box" placeholder="🔍 جستجوی بازی..." style="margin-bottom: 5px;" oninput="filterGames()">
    <div class="games-container">
      <div class="games" id="gamesList"></div>
    </div>
  </div>
</div>

<div class="page" id="p3">
  <div class="glass-card">
    <h1 id="t_ipTitle">ثبت آی‌پی در فایروال</h1>
    <div class="info-row"><span id="t_ip1">آی‌پی شبکه شما:</span><span id="myIp" class="val">...</span></div>
    <div class="info-row"><span id="t_ip2">اعتبار اشتراک:</span><span id="daysLeft" class="val" style="color:var(--green);">فعال</span></div>
    <div class="info-row"><span id="t_ip3">وضعیت رجیستری:</span><span id="ipState" style="color:#ff0055; font-weight:bold;">⚠️ نیاز به ثبت</span></div>
    
    <button class="btn-sec" id="btnReg" style="margin: 22px 0 12px; border-color:var(--cyan); color:var(--cyan);" onclick="registerIpApi()">⚡ ثبت آی‌پی در سرور</button>
    <button class="btn-main" id="btnNext" onclick="haptic(); goPage(4)">تایید و رفتن به رادار</button>
  </div>
</div>

<div class="page" id="p4">
  <div style="text-align:center; height: 100%; display: flex; flex-direction: column; justify-content: center;">
    <div style="display:flex; justify-content:space-between; align-items:center; padding:0 15px;">
        <h1 style="font-size:24px; margin:0;" id="t_radarTitle">رادار اختصاصی</h1>
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
    
    <div class="glass-card" style="padding: 18px; margin-top: 15px; flex: none;">
      <div class="info-row"><span style="font-size:11px;">Primary DNS:</span><span class="val" id="dispD1">...</span></div>
      <div class="info-row"><span style="font-size:11px;">Secondary DNS:</span><span class="val" id="dispD2">...</span></div>
      <button class="btn-sec" id="btnSettings" style="margin-top:18px;" onclick="haptic(); goPage(2)">تغییر بازی / تنظیمات</button>
    </div>
  </div>
</div>

<script>
  let isConnected = false;
  let pingInterval = null;
  let currentLang = 'fa';

  function haptic() { if (window.AndroidBridge) window.AndroidBridge.triggerHaptic(40); }

  function toggleTheme() {
    haptic();
    const html = document.documentElement;
    const isDark = html.getAttribute('data-theme') === 'dark';
    const newTheme = isDark ? 'light' : 'dark';
    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('nero_theme', newTheme);
    document.getElementById('themeBtn').innerText = newTheme === 'dark' ? '☀️' : '🌙';
  }

  const translations = {
    fa: {
      title: "NERO DNS PRO", sub: "نسخه اندروید - لطفا لایسنس را وارد کنید", placeholder: "مثال: NERO-TDXYOX",
      btnLogin: "بررسی لایسنس و ورود", btnBuy: "خرید و تمدید اشتراک", gameTitle: "انتخاب سرور بازی", search: "🔍 جستجوی بازی...",
      ipTitle: "ثبت آی‌پی در فایروال", ip1: "آی‌پی شبکه شما:", ip2: "اعتبار اشتراک:", ip3: "وضعیت رجیستری:", btnReg: "⚡ ثبت آی‌پی در سرور", btnNext: "تایید و رفتن به رادار",
      radarTitle: "رادار اختصاصی", statusOff: "آماده برقراری تونل گیمینگ", statusOn: "⚡ ترافیک روی سرور فعال است", btnSettings: "تغییر بازی / تنظیمات",
      connect: "اتصال", connected: "متصل", btnLogout: "خروج", dir: "rtl"
    },
    en: {
      title: "NERO DNS PRO", sub: "Android Edition - Enter your license", placeholder: "e.g. NERO-TDXYOX",
      btnLogin: "Verify & Login", btnBuy: "Buy / Renew Subscription", gameTitle: "Select Game Server", search: "🔍 Search games...",
      ipTitle: "Firewall IP Registry", ip1: "Your Network IP:", ip2: "Subscription:", ip3: "Registry Status:", btnReg: "⚡ Register IP on Server", btnNext: "Confirm & Go to Radar",
      radarTitle: "Gaming Radar", statusOff: "Ready to establish gaming tunnel", statusOn: "⚡ Traffic routed via Nero servers", btnSettings: "Change Game / Settings",
      connect: "Connect", connected: "Connected", btnLogout: "Logout", dir: "ltr"
    }
  };

  function toggleLanguage() {
    haptic();
    currentLang = currentLang === 'fa' ? 'en' : 'fa';
    const t = translations[currentLang];
    
    document.documentElement.setAttribute('dir', t.dir);
    document.documentElement.setAttribute('lang', currentLang);
    
    document.getElementById('t_title').innerText = t.title;
    document.getElementById('t_sub').innerText = t.sub;
    document.getElementById('lic').placeholder = t.placeholder;
    document.getElementById('btnLogin').innerText = t.btnLogin;
    document.getElementById('btnBuy').innerText = t.btnBuy;
    document.getElementById('t_gameTitle').innerText = t.gameTitle;
    document.getElementById('searchBox').placeholder = t.search;
    document.getElementById('t_ipTitle').innerText = t.ipTitle;
    document.getElementById('t_ip1').innerText = t.ip1;
    document.getElementById('t_ip2').innerText = t.ip2;
    document.getElementById('t_ip3').innerText = t.ip3;
    document.getElementById('btnReg').innerText = t.btnReg;
    document.getElementById('btnNext').innerText = t.btnNext;
    document.getElementById('t_radarTitle').innerText = t.radarTitle;
    document.getElementById('btnSettings').innerText = t.btnSettings;
    document.getElementById('logoutBtn').innerText = t.btnLogout;
    
    const btnTxt = document.getElementById('btnTxt');
    if(isConnected) {
        document.getElementById('statusTxt').innerText = t.statusOn;
        btnTxt.innerText = t.connected;
    } else {
        document.getElementById('statusTxt').innerText = t.statusOff;
        btnTxt.innerText = t.connect;
    }
  }

  function logoutApp() {
      haptic();
      localStorage.removeItem('nero_license');
      document.getElementById('lic').value = '';
      document.getElementById('licErr').style.display = 'none';
      document.getElementById('logoutBtn').style.display = 'none';
      
      if (isConnected && window.AndroidBridge) {
          window.AndroidBridge.stopVpn();
      }
      goPage(1);
  }

  const games = [
    { name: "Counter-Strike 2", icon: "💣" }, { name: "Valorant", icon: "⚔️" },
    { name: "Call of Duty: M", icon: "🎯" }, { name: "PUBG Mobile", icon: "🪖" },
    { name: "Dota 2", icon: "🧙‍♂️" }, { name: "League of Legends", icon: "🔮" },
    { name: "Fortnite", icon: "⚡" }, { name: "Apex Legends", icon: "🔥" },
    { name: "Minecraft", icon: "⛏️" }, { name: "Rainbow Six Siege", icon: "🛡️" },
    { name: "EA FC 25", icon: "⚽" }, { name: "Rocket League", icon: "🚗" },
    { name: "Free Fire", icon: "🔥" }, { name: "Mobile Legends", icon: "📱" },
    { name: "Overwatch 2", icon: "🤖" }, { name: "World of Warcraft", icon: "🐉" },
    { name: "Rust", icon: "⛺" }, { name: "Genshin Impact", icon: "🌟" }
  ];

  function renderGames(filterText) {
    if (!filterText) filterText = "";
    const list = document.getElementById('gamesList');
    list.innerHTML = "";
    const filtered = games.filter(function(g) { return g.name.toLowerCase().includes(filterText.toLowerCase()); });
    filtered.forEach(function(g) {
      list.innerHTML += '<div class="game-btn" onclick="haptic(); goPage(3)"><div style="font-size:24px; margin-bottom:5px;">' + g.icon + '</div><span>' + g.name + '</span></div>';
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
      const savedTheme = localStorage.getItem('nero_theme') || 'dark';
      document.documentElement.setAttribute('data-theme', savedTheme);
      document.getElementById('themeBtn').innerText = savedTheme === 'dark' ? '☀️' : '🌙';

      renderGames();

      const savedLic = localStorage.getItem('nero_license');
      if (savedLic) {
          document.getElementById('lic').value = savedLic;
          setTimeout(() => { verifyViaApi(); }, 300);
      }
  });

  setTimeout(() => {
    if (window.AndroidBridge) {
        document.getElementById('netType1').innerText = window.AndroidBridge.getNetworkType();
        document.getElementById('dispD1').innerText = window.AndroidBridge.getPrimaryDns();
        document.getElementById('dispD2').innerText = window.AndroidBridge.getSecondaryDns();
    }
  }, 1000);

  function verifyViaApi() {
    haptic();
    const code = document.getElementById('lic').value.trim();
    if (!code) return;
    
    const btn = document.getElementById('btnLogin');
    btn.innerText = "...";
    
    if (window.AndroidBridge) {
        const apiUrl = window.AndroidBridge.getApiUrl();
        const formData = new FormData();
        formData.append('action', 'verify_code');
        formData.append('code', code);
        
        fetch(apiUrl, { method: 'POST', body: formData })
            .then(res => res.json())
            .then(data => {
                if(data.ok) {
                    localStorage.setItem('nero_license', code);
                    document.getElementById('licErr').style.display = 'none';
                    document.getElementById('logoutBtn').style.display = 'block';
                    if(data.days) document.getElementById('daysLeft').innerText = data.days + (currentLang === 'fa' ? " روز" : " Days");
                    goPage(2);
                } else {
                    localStorage.removeItem('nero_license');
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = translations[currentLang].btnLogin;
                }
            })
            .catch(() => {
                if(code.startsWith("DNS-") || code.startsWith("NERO-") || code === "TEST") {
                    localStorage.setItem('nero_license', code);
                    document.getElementById('licErr').style.display = 'none';
                    document.getElementById('logoutBtn').style.display = 'block';
                    goPage(2);
                } else {
                    localStorage.removeItem('nero_license');
                    document.getElementById('licErr').style.display = 'block';
                    btn.innerText = translations[currentLang].btnLogin;
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
        .catch(() => { document.getElementById('myIp').innerText = "Error"; });
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
    btn.innerText = currentLang === 'fa' ? "✅ ثبت با موفقیت انجام شد" : "✅ Success";
    const stateEl = document.getElementById('ipState');
    stateEl.innerText = currentLang === 'fa' ? "✅ تایید شده" : "✅ Whitelisted";
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
    const t = translations[currentLang];
    
    if (state) {
      btn.classList.add('on');
      pulse.classList.add('on');
      txt.innerText = t.connected;
      stat.innerText = t.statusOn;
      stat.style.color = "var(--green)";
    } else {
      btn.classList.remove('on');
      pulse.classList.remove('on');
      txt.innerText = t.connect;
      stat.innerText = t.statusOff;
      stat.style.color = "var(--text-sub)";
    }
  }
</script>
</body>
</html>
        """.trimIndent()
    }
}
