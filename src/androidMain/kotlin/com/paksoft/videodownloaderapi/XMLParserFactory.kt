

package com.paksoft.videodownloaderapi

import com.paksoft.json.XML
import org.json.JSONObject

actual object XMLParserFactory {
    actual fun createParserFactory(): XMLParser {
        return object : XMLParser {
            override fun xmlToJsonObject(xmlString: String): JSONObject {
                return XML.toJSONObject(xmlString)
            }
        }
    }
}
