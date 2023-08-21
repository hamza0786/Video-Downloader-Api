

package com.paksoft.videodownloaderapi.extractor

import com.paksoft.videodownloaderapi.MimeType
import com.paksoft.videodownloaderapi.dataholders.*
import com.paksoft.videodownloaderapi.toHashtable
import com.paksoft.videodownloaderapi.toJSONObject
import org.json.JSONArray
import java.util.regex.Pattern

class Likee internal constructor(url: String) : Extractor(url) {

    companion object {
        private const val GET_VIDEO_INFO = "https://api.like-video.com/likee-activity-flow-micro/videoApi/getVideoInfo"
    }

    private val formats = Formats()

    override suspend fun analyze(payload: Any?) {
        formats.src = "Likee"
        formats.url = inputUrl
        val postId = getPostId() ?: run {
            clientRequestError()
            return
        }
        val response = httpRequestService.postRequest(GET_VIDEO_INFO, hashMapOf("postIds" to postId).toHashtable())
        val responseData =
            response?.toJSONObject()
                ?.getJSONObject("data") ?: run {
                clientRequestError()
                return
            }
        responseData.getJSONArray("videoList")?.let {
            extractVideoList(it)
        } ?: run {
            missingLogic()
        }
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private suspend fun extractVideoList(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val localFormats = formats.copy(title = "", videoData = mutableListOf(), imageData = mutableListOf())
            val currentObj = jsonArray.getJSONObject(i)
            localFormats.videoData.add(
                VideoResource(
                    currentObj.getString("videoUrl"),
                    MimeType.VIDEO_MP4,
                    "${currentObj.getInt("videoWidth")}x${currentObj.getInt("videoHeight")}"
                )
            )

            localFormats.title =
                currentObj.getString("title")?.ifEmpty { currentObj.getString("msgText") } ?: "Likee_Video"
            localFormats.imageData.add(
                ImageResource(
                    currentObj.getString("coverUrl")
                )
            )
            videoFormats.add(localFormats)
        }
        finalize()
    }

    private suspend fun getPostId(): String? {
        try {
            val page = httpRequestService.getResponse(inputUrl)
            Pattern.compile("<meta property=\"og:url\"\\W+content=\".*?postId=(.*?)\"").matcher(page).apply {
                return if (find()) group(1) else null
            }
        } catch (e: kotlin.Error) {
            return null
        }
    }
}
