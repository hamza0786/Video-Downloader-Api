

package com.paksoft.videodownloaderapi

import androidx.core.text.HtmlCompat

actual object HtmlDecoderFactory {
    actual fun createDecoderFactory(): HtmlDecoder {
        return object : HtmlDecoder {
            override fun decodeHtml(string: String): String {
                return HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            }
        }
    }
}
