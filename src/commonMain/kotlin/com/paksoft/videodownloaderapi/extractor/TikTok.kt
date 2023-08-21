

package com.paksoft.videodownloaderapi.extractor

import com.paksoft.videodownloaderapi.MimeType
import com.paksoft.videodownloaderapi.dataholders.Formats
import com.paksoft.videodownloaderapi.dataholders.ImageResource
import com.paksoft.videodownloaderapi.dataholders.VideoResource
import com.paksoft.videodownloaderapi.toJSONObject
import java.io.File
import java.util.*
import java.util.regex.Pattern



class TikTok internal constructor(url: String) : Extractor(url) {

    private val localFormats = Formats()

    override suspend fun analyze(payload: Any?) {
        localFormats.src = "TikTok"
        localFormats.url = inputUrl
        headers["Referer"] = "https://www.tiktok.com/"
        headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        val response = httpRequestService.getResponse(inputUrl, headers)
        response?.let {
            extractFromWebPage(response)
        } ?: run {
            missingLogic()
            return
        }
    }

    private suspend fun extractFromWebPage(webpage: String) {
        val matcher =
            Pattern.compile("<script id=\"SIGI_STATE\" type=\"application/json\">(\\{.*?\\})</script>").matcher(webpage)
        if (matcher.find()) {
            val json = matcher.group(1).toJSONObject()
            val itemList = json.getJSONObject("ItemList").getJSONObject("video").getJSONArray("list")
            val itemModule = json.getJSONObject("ItemModule")
            for (i in 0 until itemList.length()) {
                val videoId = itemModule.getJSONObject(itemList.getString(i))
                val formats = localFormats.copy(
                    title = "",
                    audioData = mutableListOf(),
                    videoData = mutableListOf(),
                    imageData = mutableListOf()
                )

                formats.title = videoId.getString("desc")
                val video = videoId.getJSONObject("video")
                formats.imageData.add(
                    ImageResource(
                        video.getString("cover")
                    )
                )
                val videos = video.getJSONArray("bitrateInfo")
                for (j in 0 until videos.length()) {
                    val bitrate = videos.getJSONObject(j)
                    val urls = bitrate.getJSONObject("PlayAddr").getJSONArray("UrlList")
                    for (k in 0 until urls.length()) {
                        val url = urls.getString(k)
                        formats.videoData.add(
                            VideoResource(
                                url,
                                MimeType.fromCodecs(bitrate.getString("CodecType")),
                                quality = bitrate.getString("GearName")
                            )
                        )
                    }
                }

                // TODO: needed to find flexible appropriate method and does extracting music is necessary
//                val music = videoId.getNullableJSONObject("music")
//                music?.let {
//                    if(formats.title.isEmpty()) formats.title = it.getString("title")
//
//                }
                videoFormats.add(formats)
            }
            finalize()
        } else {
            val uuid = "tt." + UUID.randomUUID().toString() + ".html"
            File(uuid).writeText(webpage)
            missingLogic()
        }
    }

    override suspend fun testWebpage(string: String) {
        onProgress = {
            println(it)
        }
        extractFromWebPage(string)
    }
}
