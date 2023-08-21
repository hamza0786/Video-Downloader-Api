

package com.paksoft.videodownloaderapi

import io.ktor.utils.io.errors.*
import org.ccil.cowan.tagsoup.HTMLSchema
import org.ccil.cowan.tagsoup.Parser
import org.xml.sax.*
import java.io.StringReader

private val schema = HTMLSchema()
actual object HtmlDecoderFactory {
    actual fun createDecoderFactory(): HtmlDecoder {
        return object : HtmlDecoder {
            override fun decodeHtml(string: String): String {
                val parser = Parser()
                try {
                    parser.setProperty(Parser.schemaProperty, schema)
                } catch (e: SAXNotRecognizedException) {
                    // Should not happen.
                    throw RuntimeException(e)
                } catch (e: SAXNotSupportedException) {
                    // Should not happen.
                    throw RuntimeException(e)
                }
                val converter = HtmlToSpannedConverter(string, parser)
                return converter.convert().toString()
            }
        }
    }
}

internal class HtmlToSpannedConverter(
    private val mSource: String,
    parser: Parser
) : ContentHandler {
    private val mReader: XMLReader
    private val mStringBuilder = StringBuilder()
    init {
        mReader = parser
    }
    fun convert(): StringBuilder {
        mReader.contentHandler = this
        try {
            mReader.parse(InputSource(StringReader(mSource)))
        } catch (e: IOException) {
            // We are reading from a string. There should not be IO problems.
            throw RuntimeException(e)
        } catch (e: SAXException) {
            // TagSoup doesn't throw parse exceptions.
            throw RuntimeException(e)
        }
        return mStringBuilder
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        val sb = StringBuilder()
        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */
        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */for (i in 0 until length) {
            val c = ch!![i + start]
            if (c == ' ' || c == '\n') {
                var pred: Char
                var len = sb.length
                if (len == 0) {
                    len = mStringBuilder.length
                    pred = if (len == 0) {
                        '\n'
                    } else {
                        mStringBuilder[len - 1]
                    }
                } else {
                    pred = sb[len - 1]
                }
                if (pred != ' ' && pred != '\n') {
                    sb.append(' ')
                }
            } else {
                sb.append(c)
            }
        }
        mStringBuilder.append(sb)
    }

    override fun setDocumentLocator(locator: Locator?) {}

    override fun startDocument() {}

    override fun endDocument() {}

    override fun startPrefixMapping(prefix: String?, uri: String?) {}

    override fun endPrefixMapping(prefix: String?) {}

    override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {}

    override fun endElement(uri: String?, localName: String?, qName: String?) {}

    override fun ignorableWhitespace(ch: CharArray?, start: Int, length: Int) {}

    override fun processingInstruction(target: String?, data: String?) {}

    override fun skippedEntity(name: String?) {}
}
