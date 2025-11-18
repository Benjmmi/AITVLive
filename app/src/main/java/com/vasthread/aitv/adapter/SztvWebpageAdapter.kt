package com.vasthread.aitv.adapter

import com.vasthread.aitv.widget.WebpageAdapterWebView

class SztvWebpageAdapter : CommonWebpageAdapter() {

    override fun isAdaptedUrl(url: String) = url.contains("sztv.com.cn")

    override suspend fun enterFullscreen(webView: WebpageAdapterWebView) {
        enterFullscreenByPressKey(webView)
    }
}