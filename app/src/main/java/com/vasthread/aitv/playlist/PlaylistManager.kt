package com.vasthread.aitv.playlist

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.vasthread.aitv.misc.application
import com.vasthread.aitv.misc.preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

object PlaylistManager {

    private const val TAG = "PlaylistManager"
    private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L
    private const val KEY_PLAYLIST_URL = "playlist_url"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val UPDATE_RETRY_DELAY = 10 * 1000L

    private val client =
        OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .build()
    private val gson = GsonBuilder().setPrettyPrinting().create()!!
    private val jsonTypeToken = object : TypeToken<List<Channel>>() {}
    private val playlistFile = File(application.filesDir, "playlist.json")
    private val builtInPlaylists = listOf<Pair<String, String>>()

    var onPlaylistChange: ((Playlist) -> Unit)? = null
    var onUpdatePlaylistJobStateChange: ((Boolean) -> Unit)? = null
    private var updatePlaylistJob: Job? = null
    private var isUpdating = false
        set(value) {
            onUpdatePlaylistJobStateChange?.invoke(value)
        }

    fun getBuiltInPlaylists() = builtInPlaylists

    fun setPlaylistUrl(url: String) {
        preference.edit()
            .putString(KEY_PLAYLIST_URL, url)
            .putLong(KEY_LAST_UPDATE, 0)
            .apply()
        requestUpdatePlaylist()
    }

    fun getPlaylistUrl() =
        "https://hub.gitmirror.com/raw.githubusercontent.com/Benjmmi/iptv-api/refs/heads/master/output/user_result.txt"
//        preference.getString(KEY_PLAYLIST_URL, builtInPlaylists.firstOrNull()?.second ?: "")!!

    fun setLastUpdate(time: Long, requestUpdate: Boolean = false) {
        preference.edit().putLong(KEY_LAST_UPDATE, time).apply()
        if (requestUpdate) requestUpdatePlaylist()
    }

    private fun requestUpdatePlaylist() {
        val lastJobCompleted = updatePlaylistJob?.isCompleted
        if (lastJobCompleted != null && !lastJobCompleted) {
            Log.i(TAG, "A job is executing, ignore!")
            return
        }
//        val playlistText = """
//            [{"name":"CCTV-1 综合","group":"China","urls":["https://stream1.freetv.fun/a4f6e6163319cb6ad59892a54343f514a9ff20917af903f2ee1818e005f43202.ctv"]},{"name":"CCTV-2 财经","group":"China","urls":["https://stream1.freetv.fun/1847276d6bc5debba388a2d32acc6ac34427b06d901dedb2d9678954b03ad752.ctv"]}]
//        """.trimIndent()
//        playlistFile.writeText(playlistText)
        updatePlaylistJob = CoroutineScope(Dispatchers.IO).launch {
            var times = 0
            val needUpdate = {
                System.currentTimeMillis() - preference.getLong(
                    KEY_LAST_UPDATE,
                    0L
                ) > CACHE_EXPIRATION_MS
            }
            isUpdating = true
            while (needUpdate()) {
                ++times
                Log.i(TAG, "Updating playlist... times=${times}")
                try {
                    val url = getPlaylistUrl()
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("Response code ${response.code}")

                    var remote = response.body!!.string()
                    remote = parseM3UTextToJson(remote)
                    val local = runCatching { playlistFile.readText() }.getOrNull()
                    if (remote != local) {
                        playlistFile.writeText(remote)
                        onPlaylistChange?.invoke(createPlaylistFromJson(remote))
                    }

                    setLastUpdate(System.currentTimeMillis())
                    Log.i(TAG, "Update playlist successfully.")
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot update playlist, reason: ${e.message}")
                }
                if (needUpdate()) {
                    delay(UPDATE_RETRY_DELAY)
                }
            }
            isUpdating = false
        }
    }

    /**
     * 将自定义的M3U格式文本解析成目标JSON字符串
     * @param m3uText 原始的、以逗号分隔的文本
     * @return 符合 Channel 结构体定义的JSON字符串
     */
    private fun parseM3UTextToJson(m3uText: String): String {
        var channels = HashMap<String, Channel>()
        var currentGroup = "default" // 默认分组

        m3uText.lines().forEach { line ->
            // 跳过空行或无效行
            if (line.isBlank() || !line.contains(',')) {
                return@forEach
            }

            val parts = line.split(',')
            val name = parts[0].trim()
            val url = parts.getOrNull(1)?.trim() ?: ""

            if (url == "#genre#") {
                // 这是一个分组标记行
                currentGroup = name
            } else {
                // 这是一个频道行
                var value = channels[name]
                if (value == null) {
                    val channel = Channel(
                        name,
                        currentGroup,
                        listOf(url) // 将URL包装在列表中，以匹配Channel数据结构
                    )
                    channels.put(name, channel)
                } else {
                    var list = value.urls.toMutableList();
                    list.add(url)
                    value.urls = list
                    channels[name] = value
                }
//                channels.add(channelMap)
            }
        }
        // 使用Gson将处理好的List<Map>转换成JSON字符串
        var list = channels.values
        var iterator = list.iterator()
        while (iterator.hasNext()){
            val channel = iterator.next();
            if (channel.groupName.contains("更新时间")){
                iterator.remove()
            }
        }
        return gson.toJson(list)
    }

    private fun createPlaylistFromJson(json: String): Playlist {
        val channels = gson.fromJson(json, jsonTypeToken)
        return Playlist.createFromAllChannels("default", channels)
    }

    private fun loadBuiltInPlaylist() = createPlaylistFromJson("[]")

    fun loadPlaylist(): Playlist {
        return try {
            val json = playlistFile.readText()
//            json = parseM3UTextToJson(json)
            createPlaylistFromJson(json)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot load playlist, reason: ${e.message}")
            setLastUpdate(0L)
            loadBuiltInPlaylist()
        } finally {
            requestUpdatePlaylist()
        }
    }

}