

package com.paksoft.videodownloaderapi.extractor

import com.paksoft.videodownloaderapi.MimeType
import com.paksoft.videodownloaderapi.dataholders.Formats
import com.paksoft.videodownloaderapi.dataholders.ImageResource
import com.paksoft.videodownloaderapi.dataholders.VideoResource
import com.paksoft.videodownloaderapi.toJSONObject
import java.util.regex.Pattern


class Twitch internal constructor(url: String) : Extractor(url) {

    private val localFormats = Formats()
    override suspend fun analyze(payload: Any?) {
        TODO("Not yet implemented")
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private fun getId(s: String?): String? {
        return s?.run {
            val matcher =
                Pattern.compile("https?://(?:clips\\.twitch\\.tv/(?:embed\\?.*?\\bclip=|(?:[^/]+/)*)|(?:(?:www|go|m)\\.)?twitch\\.tv/[^/]+/clip/)([^/?#&]+)")
                    .matcher(this)
            if (matcher.find())
                matcher.group(1)
            else null
        }
    }

    suspend fun extractURLFromClips(): Formats? {
        var id: String?
        if (getId(inputUrl).also { id = it } == null) {
            clientRequestError("Error!! Id can't find for URL $inputUrl")
            return null
        }
        headers["Content-Type"] = "text/plain;charset=UTF-8"
        headers["Client-ID"] = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        val data = String.format(
            "{\"query\":\"{clip(slug:\\\"%s\\\"){broadcaster{displayName}createdAt curator{displayName id}durationSeconds id tiny:thumbnailURL(width:86,height:45)small:thumbnailURL(width:260,height:147)medium:thumbnailURL(width:480,height:272)title videoQualities{frameRate quality sourceURL}viewCount}}\"}",
            id
        )
        val response = httpRequestService.postRequest("https://gql.twitch.tv/gql", headers = headers) ?: run {
            clientRequestError()
            return localFormats
        }
        val clip = response.toJSONObject().getJSONObject("data").getJSONObject("clip")
        localFormats.imageData.add(ImageResource(clip.getString("medium"), "medium"))
        val videoQualities = clip.getJSONArray("videoQualities")
        localFormats.title = clip.getString("title")
        for (j in 0 until videoQualities.length()) {
            val video = videoQualities.getJSONObject(j)
            localFormats.videoData.add(
                VideoResource(
                    video.getString("sourceURL"),
                    MimeType.VIDEO_MP4,
                    video.getString("quality") + "p"
                )
            )
        }
        return localFormats
    }
}
