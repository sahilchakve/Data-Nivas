package com.friday.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

/** Data Nivas: the whole family runs inside this app. The page is bundled in assets/index.html. */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var loader: WebViewAssetLoader
    private lateinit var speech: SpeechBridge
    private var pending: PermissionRequest? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pending?.let { it.grant(it.resources) }
        pending = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        webView.setBackgroundColor(0xFF02060B.toInt())

        loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        speech = SpeechBridge(this)
        webView.addJavascriptInterface(speech, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                request?.url?.let { loader.shouldInterceptRequest(it) }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                val req = request ?: return
                val need = mutableListOf<String>()
                if (req.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) need += Manifest.permission.RECORD_AUDIO
                if (req.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) need += Manifest.permission.CAMERA
                val missing = need.filter {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    req.grant(req.resources)
                } else {
                    pending = req
                    permLauncher.launch(missing.toTypedArray())
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onDestroy() {
        speech.shutdown()
        super.onDestroy()
    }
}
