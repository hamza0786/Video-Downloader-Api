

package com.paksoft.videodownloaderapi.network

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.statement.*
import java.util.*

interface HttpRequestService {
    suspend fun getResponse(
        url: String,
        headers:
            Hashtable<String, String>? = null,
    ): String?

    /**
     * Makes Http request
     *
     * @return HttpResponse
     */
    suspend fun getRawResponse(
        url: String,
        headers: Hashtable<String, String>? = null,
        followRedirect: Boolean = true,
    ): HttpResponse?

    /**
     * Makes Http request
     *
     * @return HttpResponse
     */
    suspend fun headRawResponse(
        url: String,
        headers: Hashtable<String, String>? = null,
        followRedirect: Boolean = true,
    ): HttpResponse?

    /**
     * Used to estimate size of given url in bytes
     *
     * @return bytes count of given [url]
     */
    suspend fun getSize(
        url: String,
        headers: Hashtable<String, String>? = null,
    ): Long

    suspend fun postRequest(
        url: String,
        headers: Hashtable<String, String>? = null,
        postData: Hashtable<String, Any>? = null,
    ): String?

    suspend fun postRawResponse(
        url: String,
        headers: Hashtable<String, String>? = null,
        postData: Hashtable<String, Any>? = null,
        followRedirect: Boolean = true,
    ): HttpResponse?

    suspend fun checkPageAvailability(
        url: String,
        headers: Hashtable<String, String>? = null,
    ): Boolean

    fun close()

    companion object {
        fun create(
            client: HttpClient = HttpClient(Android),
            storage: CookiesStorage? = null,
        ): HttpRequestService =
            HttpRequestServiceImpl(
                client.config {
                    if (storage != null) {
                        install(HttpCookies) {
                            this.storage = storage
                        }
                    }
                }
            )
    }
}
