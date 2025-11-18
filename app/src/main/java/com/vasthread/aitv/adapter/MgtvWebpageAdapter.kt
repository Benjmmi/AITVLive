package com.vasthread.aitv.adapter

import android.view.KeyEvent
import com.vasthread.aitv.widget.WebpageAdapterWebView

class MgtvWebpageAdapter : CommonWebpageAdapter() {

    override fun isAdaptedUrl(url: String) = url.contains("live.mgtv.com")

    override suspend fun enterFullscreen(webView: WebpageAdapterWebView) {
        enterFullscreenByPressKey(webView, KeyEvent.KEYCODE_F)
    }
}