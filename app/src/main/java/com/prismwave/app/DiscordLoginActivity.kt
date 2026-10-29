package com.prismwave.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

private const val DISCORD_TOKEN_JS =
    "(function(){try{var f=document.createElement('iframe');document.body.appendChild(f);" +
        "var t=f.contentWindow.localStorage.getItem('token');f.remove();return t?t:'';}catch(e){return '';}})()"

/** Discord sign-in in its own plain Android screen (no Compose), so the web page can always draw. */
class DiscordLoginActivity : Activity() {
    private var web: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var done = false
    private lateinit var status: TextView

    private val poll = object : Runnable {
        override fun run() {
            val w = web ?: return
            if (done) return
            w.evaluateJavascript(DISCORD_TOKEN_JS) { raw ->
                val t = (raw ?: "").filter { it != '"' && it != '\\' && it != '\'' }
                if (!done && t.length > 40 && t != "null") {
                    done = true
                    DiscordRpc.onToken(applicationContext, t)
                    finish()
                }
            }
            handler.postDelayed(this, 1500)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#121214"))
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding((16 * dp).toInt(), (10 * dp).toInt(), (16 * dp).toInt(), (10 * dp).toInt())
        val close = TextView(this)
        close.text = "\u2715"
        close.textSize = 22f
        close.setTextColor(Color.WHITE)
        close.setPadding((8 * dp).toInt(), (4 * dp).toInt(), (20 * dp).toInt(), (4 * dp).toInt())
        close.setOnClickListener { finish() }
        val title = TextView(this)
        title.text = "Sign in to Discord"
        title.textSize = 20f
        title.setTextColor(Color.WHITE)
        bar.addView(close)
        bar.addView(title)
        root.addView(bar)

        status = TextView(this)
        status.textSize = 13f
        status.setTextColor(Color.parseColor("#B0B0B8"))
        status.setPadding((20 * dp).toInt(), 0, (20 * dp).toInt(), (6 * dp).toInt())
        status.text = "Starting..."
        root.addView(status)

        val w = WebView(this)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.javaScriptCanOpenWindowsAutomatically = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
        w.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                status.text = if (newProgress < 100) "Loading Discord... $newProgress%" else "Page loaded. Sign in below."
            }
        }
        w.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    status.text = "Could not load Discord: " + (error?.description ?: "error")
                }
            }
        }
        root.addView(w, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        web = w
        w.loadUrl("https://discord.com/login")
        handler.postDelayed(poll, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        web?.destroy()
        web = null
        super.onDestroy()
    }
}
