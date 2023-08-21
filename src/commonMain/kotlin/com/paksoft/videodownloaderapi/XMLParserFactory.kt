

package com.paksoft.videodownloaderapi

import org.json.JSONObject

interface XMLParser {
    fun xmlToJsonObject(xmlString: String): JSONObject
}

expect object XMLParserFactory {
    fun createParserFactory(): XMLParser
}
