package com.vasthread.aitv.adapter

import com.vasthread.aitv.widget.WebpageAdapterWebView

class ChromeUrlWebpageAdapter: WebpageAdapter() {
    override fun isAdaptedUrl(url: String) = url.startsWith("chrome:") || url.startsWith("about:")

    override fun javascript() = ""

    override suspend fun enterFullscreen(webView: WebpageAdapterWebView) {
    }

}