

package com.paksoft.videodownloaderapi

import org.json.JSONObject
import org.json.XML

actual object XMLParserFactory {
    actual fun createParserFactory(): XMLParser {
        return object : XMLParser {
            override fun xmlToJsonObject(xmlString: String): JSONObject {
                return XML.toJSONObject(xmlString)
            }
        }
    }
}
