package com.zcodemobile.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.zcodemobile.app.QRUrlParser
import com.zcodemobile.app.ZcodeConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(connection: ZcodeConnection, keepScreenOn: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var pageTitle by remember { mutableStateOf(QRUrlParser.displayName(connection)) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(100) }
    var isRefreshing by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }

    if (keepScreenOn) {
        DisposableEffect(Unit) {
            val window = (context as? Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "断开连接")
            }
            Text(
                pageTitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                webView?.reload()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportMultipleWindows(true)
                            ViewCompat.setNestedScrollingEnabled(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    // 子框架资源不做内外链分流
                                    if (!request.isForMainFrame) return false
                                    val url = request.url
                                    val host = url.host?.lowercase()
                                    val internal = host != null &&
                                        (host in QRUrlParser.KNOWN_HOSTS || host == connection.host)
                                    if (internal) return false
                                    return try {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, url))
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                    pageError = null
                                }

                                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                    canGoBack = view.canGoBack()
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    if (request.isForMainFrame) {
                                        pageError = error.description?.toString() ?: "网络错误"
                                    }
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    view.title?.takeIf { it.isNotBlank() }?.let { pageTitle = it }
                                    isRefreshing = false
                                    progress = 100
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }

                                // target="_blank"：官方域名在本页打开，外链交给系统浏览器
                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: android.os.Message,
                                ): Boolean {
                                    val host = view.url?.let { runCatching { Uri.parse(it).host?.lowercase() }.getOrNull() }
                                    val temp = WebView(view.context)
                                    temp.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                                            val url = request.url
                                            val newHost = url.host?.lowercase()
                                            val internal = newHost != null &&
                                                (newHost in QRUrlParser.KNOWN_HOSTS || newHost == connection.host || newHost == host)
                                            if (internal) {
                                                view.loadUrl(url.toString())
                                            } else {
                                                try {
                                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, url))
                                                } catch (_: Exception) {
                                                }
                                            }
                                            return true
                                        }
                                    }
                                    (resultMsg.obj as WebView.WebViewTransport).webView = temp
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            loadUrl(connection.url)
                            webView = this
                        }
                    },
                    // destroy 会断开页面内的中继连接并释放资源
                    onRelease = {
                        it.stopLoading()
                        it.onPause()
                        it.destroy()
                    },
                )

                if (pageError != null) {
                    ErrorOverlay(
                        message = pageError.orEmpty(),
                        url = connection.url,
                        onRetry = {
                            pageError = null
                            webView?.reload()
                        },
                    )
                }
            }
        }
    }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }
    BackHandler(enabled = !canGoBack) {
        onClose()
    }
}

@Composable
private fun ErrorOverlay(message: String, url: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("页面加载失败", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "请检查网络连接后重试。\n$message\n$url",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}
