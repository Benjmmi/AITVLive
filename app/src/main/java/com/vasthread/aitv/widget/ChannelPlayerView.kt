package com.vasthread.aitv.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import com.vasthread.aitv.R
import com.vasthread.aitv.misc.application
import com.vasthread.aitv.playlist.Channel
import com.vasthread.aitv.settings.SettingsManager
import java.nio.file.Files

class ChannelPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ChannelPlayer"
    }

    private val webView: WebpageAdapterWebView
    private val waitingView: WaitingView
    private val channelBarView: ChannelBarView

    private fun html(url: String) : String{
        return """
<html style="width:100;height:100%">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="Cache-Control" content="no-siteapp">
    <meta name="renderer" content="webkit">
    <meta name="viewport" content="width=device-width,initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no">
</head>
<body style="width:100;height:100%">
<video class="dplayer-video dplayer-video-current"
       webkit-playsinline=""
       x-webkit-airplay="allow"
       playsinline=""
       preload="metadata"
       id="aitvlive"
       width="100%"
       height="100%"
       src="$url">
               您的浏览器不支持视频播放
</video>
</body>
</html>
        """.trimIndent()
    }

    var channel: Channel? = null
        set(value) {
            if (field == value) return
            field = value
            if (value == null) {
                webView.loadUrl(WebpageAdapterWebView.URL_BLANK)
                channelBarView.dismiss()
            } else if (!value.urls.isEmpty()){
                webView.loadUrl(value.url)
//                webView.loadUrl("http://106.53.99.30/php/cctv/cctvnews.php?id=cctv2&q=lg")

//                webView.loadDataWithBaseURL(
//                    null,
//                    html(value.url),
//                    "text/html",
//                    "UTF-8",
//                    null
//                )
                channelBarView.setCurrentChannelAndShow(value)
            }
        }
    var clickCallback: ((Float, Float) -> Unit)? = null
    var dismissAllViewCallback: (() -> Unit)? = null
    var onChannelReload: ((Channel) -> Unit)? = null
    var onVideoRatioChanged: ((Boolean) -> Unit)? = null

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent) = true

            override fun onShowPress(e: MotionEvent) = Unit

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                clickCallback?.invoke(e.x, e.y)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ) = false

            override fun onLongPress(e: MotionEvent) = Unit

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ) = false

        })

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultFocusHighlightEnabled = false
        }
        LayoutInflater.from(context).inflate(R.layout.widget_channel_player, this)
        setBackgroundColor(Color.BLACK)
        webView = findViewById(R.id.webView)
        channelBarView = findViewById(R.id.channelBarView)
        waitingView = findViewById(R.id.waitingView)
        waitingView.playerView = this
        webView.apply {
            fullscreenContainer = this@ChannelPlayerView.findViewById(R.id.fullscreenContainer)
            onPageFinished = {}
            onProgressChanged = { channelBarView.setProgress(it) }
            onFullscreenStateChanged = {}
            onWaitingStateChanged = { waitingView.visibility = if (it) VISIBLE else GONE }
            onVideoRatioChanged =
                { this@ChannelPlayerView.onVideoRatioChanged?.invoke(it == WebpageAdapterWebView.RATIO_16_9) }
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return webView.requestFocus(direction, previouslyFocusedRect)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return if (SettingsManager.isWebViewTouchable()) {
            dismissAllViewCallback?.invoke()
            super.dispatchTouchEvent(event)
        } else {
            gestureDetector.onTouchEvent(event)
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return false
    }

    fun refreshChannel() {
        webView.loadUrl(channel!!.url)
    }

    fun setVideoRatio(is_16_9: Boolean) {
        webView.setVideoRatio(if (is_16_9) WebpageAdapterWebView.RATIO_16_9 else WebpageAdapterWebView.RATIO_4_3)
    }

    fun getVideoSize() = webView.getVideoSize()
}