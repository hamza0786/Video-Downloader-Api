

package com.paksoft.videodownloaderapi.extractor

import com.paksoft.videodownloaderapi.MimeType
import com.paksoft.videodownloaderapi.dataholders.*
import com.paksoft.videodownloaderapi.getNullableString
import org.json.JSONObject
import java.util.regex.Pattern


class ShareChat internal constructor(url: String) : Extractor(url) {
    private val formats = Formats()

    override suspend fun analyze(payload: Any?) {
        formats.url = inputUrl
        formats.src = "ShareChat"
        onProgress(Result.Progress(ProgressState.Start))
        scratchWebPage(
            httpRequestService.getResponse(inputUrl) ?: run {
                clientRequestError()
                return
            }
        )
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private suspend fun scratchWebPage(response: String) {
        val matcher =
            Pattern.compile("""<script data-rh="true" type="application\/ld\+json">(\{"@context":"http:\/\/schema\.org","@type":"(?:Image|Video)Object".*?\})<\/script>""")
                .matcher(response)
        if (!matcher.find()) {
            internalError("Unable detect the contentUrl for $inputUrl")
            return
        }
        onProgress(Result.Progress(ProgressState.Middle))
        val responseObject = JSONObject(matcher.group(1)!!)
        formats.title = responseObject.getString("name")
            ?: responseObject.getString("description")
            ?: "ShareChat_${
            responseObject.getJSONObject("author")
                .getNullableString("name")
            }"
        val contentUrl = responseObject.getString("contentUrl")
        try {
            // Try for video
            val resolution = responseObject.getString("width") + "x" + responseObject.getString("height")
            formats.videoData.add(
                VideoResource(
                    contentUrl,
                    MimeType.VIDEO_MP4,
                    resolution
                )
            )
            formats.imageData.add(ImageResource(responseObject.getString("thumbnail")))
        } catch (e: Exception) {
            formats.imageData.add(ImageResource(contentUrl))
        }
        videoFormats.add(formats)
        finalize()
    }
}
