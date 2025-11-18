package com.vasthread.aitv.adapter

class FgtvWebpageAdapter :CommonWebpageAdapter(){

    override fun isAdaptedUrl(url: String) = url.contains("4gtv")

    override fun getFullscreenElementId(): String {
        return "#videoPlay_html5_api"
    }
}