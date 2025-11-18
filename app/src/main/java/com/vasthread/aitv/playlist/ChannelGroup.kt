package com.vasthread.aitv.playlist

data class ChannelGroup(
    var name: String,
    val channels: MutableList<Channel> = mutableListOf()
)