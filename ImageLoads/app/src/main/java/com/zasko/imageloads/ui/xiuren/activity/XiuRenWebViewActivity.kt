package com.zasko.imageloads.ui.xiuren.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import com.zasko.imageloads.base.BaseActivity
import com.zasko.imageloads.components.LogComponent

class XiuRenWebViewActivity : BaseActivity() {

    companion object {
        private const val TAG = "XiuRenWebViewActivity"
        private const val KEY_URL = "key_url"
        private const val DEFAULT_URL = "https://xiutaku.com/?start=0"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

        private val REQUEST_HEADERS = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language" to "zh-CN,zh;q=0.9,en;q=0.8,ko;q=0.7",
            "Referer" to DEFAULT_URL,
            "Sec-CH-UA" to "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"",
            "Sec-CH-UA-Mobile" to "?0",
            "Sec-CH-UA-Platform" to "\"macOS\"",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
        )

        fun start(context: Context, url: String = DEFAULT_URL) {
            context.startActivity(Intent(context, XiuRenWebViewActivity::class.java).apply {
                putExtra(KEY_URL, url)
            })
        }
    }

    private lateinit var webView: WebView
    private var hasOpenedExternalBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        LogComponent.printE(
                            tag = TAG,
                            message = "WebView load failed:${error?.description} url:${request.url}",
                        )
                        openExternalBrowser(url = request.url.toString())
                    }
                }
            }
            webChromeClient = WebChromeClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.userAgentString = DESKTOP_USER_AGENT
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        setContentView(webView)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        webView.loadUrl(intent.getStringExtra(KEY_URL) ?: DEFAULT_URL, REQUEST_HEADERS)
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        webView.destroy()
        super.onDestroy()
    }

    private fun openExternalBrowser(url: String) {
        if (hasOpenedExternalBrowser) {
            return
        }
        hasOpenedExternalBrowser = true
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            finish()
        }.onFailure { throwable ->
            LogComponent.printE(tag = TAG, message = throwable.toString())
        }
    }
}
