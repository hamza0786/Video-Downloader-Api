

package com.paksoft.videodownloaderapi.extractor

import com.paksoft.videodownloaderapi.MimeType
import com.paksoft.videodownloaderapi.count
import com.paksoft.videodownloaderapi.dataholders.AudioResource
import com.paksoft.videodownloaderapi.dataholders.Formats
import com.paksoft.videodownloaderapi.dataholders.ImageResource
import com.paksoft.videodownloaderapi.dataholders.VideoResource
import com.paksoft.videodownloaderapi.toJSONObject
import org.json.JSONObject
import java.net.URI
import java.util.regex.Pattern

class Vimeo internal constructor(url: String) : Extractor(url) {

    private val formats: Formats = Formats()

    companion object {
        const val CONFIG_URL = "https://player.vimeo.com/video/%s/config"
    }

    private fun getVideoId() = Pattern.compile("(?:https|http):\\/\\/(?:www\\.|.*?)vimeo\\.com\\/((?=.*?#).*(?=#)|.*)")
        .matcher(inputUrl)
        .run {
            if (find()) group(1) else null
        }

    override suspend fun analyze(payload: Any?) {
        formats.src = "Vimeo"
        formats.url = inputUrl
        val id = getVideoId()
        id?.let {
            parseConfigRequest(
                httpRequestService.getResponse(CONFIG_URL.format(it)) ?: run {
                    clientRequestError()
                    return
                }
            )
            videoFormats.add(formats)
            finalize()
        }
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private suspend fun parseConfigRequest(response: String) {
        val json = response.toJSONObject()
        val hls = json.getJSONObject("request").getJSONObject("files").getJSONObject("dash")
        val defaultCdn = hls.getString("default_cdn")
        val cdnUrl = hls.getJSONObject("cdns").getJSONObject(defaultCdn).getString("url")
        extractFromCdns(
            httpRequestService.getResponse(cdnUrl) ?: run {
                clientRequestError()
                return
            },
            cdnUrl
        )
        val video = json.getJSONObject("video")
        extractMetaData(video)
    }

    private fun extractMetaData(video: JSONObject) {
        val thumbs = video.getJSONObject("thumbs")
        thumbs.keys().forEach {
            formats.imageData.add(
                ImageResource(
                    thumbs.getString(it),
                    resolution = it
                )
            )
        }

        formats.title = video.getString("title")
    }

    private fun extractFromCdns(response: String, cdnUrl: String) {
        val baseUrl = response.toJSONObject().getString("base_url")
        val videoArray = response.toJSONObject().getJSONArray("video")
        val audioArray = response.toJSONObject().getJSONArray("audio")

        val modified = goBackPossibly(baseUrl, cdnUrl).toMutableList()
        modified[1] += "/${modified[0]}"

        fun getUrlAndMimeFromObject(jsonObject: JSONObject) = run {
            val tempList = goBackPossibly(jsonObject.getString("base_url"), modified[1]).toMutableList()
            tempList[1] += "/${tempList[0]}"

            listOf(
                tempList[1] + jsonObject.get("id") + ".mp4",
                MimeType.fromCodecs(jsonObject.getString("codecs"), jsonObject.getString("mime_type"))
            )
        }

        fun extractVideoData() {
            for (i in 0 until videoArray.length()) {
                val it = videoArray.get(i)
                if (it !is JSONObject)
                    continue
                val (videoUrl, mime) = getUrlAndMimeFromObject(it)
                formats.videoData.add(
                    VideoResource(
                        videoUrl,
                        mime,
                        "${it.get("width")}x${it.get("height")}",
                        hasAudio = false
                    )
                )
            }
        }

        fun extractAudioData() {
            for (i in 0 until audioArray.length()) {
                val it = audioArray.get(i)
                if (it !is JSONObject)
                    continue
                val (audioUrl, mime) = getUrlAndMimeFromObject(it)
                formats.audioData.add(
                    AudioResource(
                        audioUrl,
                        mime,
                        bitrate = it.getLong("bitrate")
                    )
                )
            }
        }
        extractVideoData()
        extractAudioData()
    }

    private fun goBackPossibly(baseUrl: String, mainUrl: String): List<String> {
        var backCount = baseUrl.count("..")
        val tempBaseUrl = baseUrl.replace("../", "")
        var tempMainUrl = mainUrl.dropLastWhile { it == '/' }
        if (URI.create(tempMainUrl).path.contains(".") && backCount >= 1)
            backCount += 1
        repeat(backCount) {
            tempMainUrl = tempMainUrl.substring(0, tempMainUrl.lastIndexOf("/"))
        }
        return listOf(tempBaseUrl, tempMainUrl.dropLastWhile { it == '/' })
    }
}
