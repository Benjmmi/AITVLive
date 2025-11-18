package com.vasthread.aitv.adapter

class YangshipinWebpageAdapter : CommonWebpageAdapter() {

    override fun isAdaptedUrl(url: String) = url.contains("yangshipin.cn")

    override fun isPlayingCheckEnabled() = false
}