

package com.paksoft.videodownloaderapi



interface HtmlDecoder {
    fun decodeHtml(string: String): String
}

expect object HtmlDecoderFactory {
    fun createDecoderFactory(): HtmlDecoder
}
