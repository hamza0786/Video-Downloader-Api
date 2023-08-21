

package com.paksoft.videodownloaderapi.network

import com.paksoft.videodownloaderapi.toJsonString
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.regex.Pattern
import kotlin.math.min


class HttpRequestServiceImpl(private val client: HttpClient) : HttpRequestService {

    private val redirectionStatusCode = setOf(
        HttpStatusCode.MovedPermanently, HttpStatusCode.Found, HttpStatusCode.TemporaryRedirect
    )

    companion object {
        private val logger = LoggerFactory.getLogger(HttpRequestServiceImpl::class.java)
    }

    override suspend fun getResponse(
        url: String,
        headers: Hashtable<String, String>?,
    ): String? {
        return try {
            client.get {
                url(url)
                headers?.let {
                    if (it.isNotEmpty()) {
                        headers {
                            for ((key, value) in it) append(key, value)
                        }
                    }
                }
            }.run {
                if (status == HttpStatusCode.OK) {
                    bodyAsText()
                } else if (status in redirectionStatusCode) {
                    getLastPossibleRedirectedResponse(this, headers).bodyAsText()
                } else if (url.contains("instagram") && status == HttpStatusCode.InternalServerError) {
                    "{error:\"Invalid Cookies\"}"
                } else if (status == HttpStatusCode.TooManyRequests) {
                    logger.warn("Unhandled in getData() TooManyRequest for url=$url with headers=$headers & response=${bodyAsText()}")
                    "429"
                } else {
                    val body = bodyAsText()
                    logger.warn(
                        "Unhandled in getData() status code=$status for url=$url with headers=$headers &\n response=${
                        body.substring(
                            0,
                            min(body.length, 500)
                        )
                        }"
                    )
                    null
                }
            }
        } catch (e: ClientRequestException) {
            logger.error("getData() url=$url header=$headers ClientRequestException:\n", e)
            null
        } catch (e: IOException) {
            logger.error("getData() url=$url header=$headers IOException\n ${e.message}")
            throw ProxyException(e)
        } catch (e: SendCountExceedException) {
            if (url.contains("instagram") && headers?.containsKey("Cookie") == true) {
                "{error:\"Invalid Cookies\"}"
            } else {
                logger.error("getData() url=$url header=$headers SendCountExceedException:\n", e)
                throw e
            }
        } catch (e: Exception) {
            if (e is TimeoutCancellationException || e is CancellationException) {
                logger.error("getData() url=$url header=$headers Cancellation exception:\n ${e.message}")
                return null
            } else logger.error("getData() url=$url header=$headers Generic exception:\n", e)
            throw e
        }
    }

    override suspend fun getRawResponse(
        url: String,
        headers: Hashtable<String, String>?,
        followRedirect: Boolean,
    ): HttpResponse? = try {
        var cache = true
        client.config {
            cache = this.followRedirects
            this.followRedirects = followRedirect
        }
        val response = client.get {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
        }
        client.config {
            this.followRedirects = cache
        }
        response
    } catch (e: IOException) {
        logger.error("getRawResponse() url=$url header=$headers IOException\n ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        if (e is CancellationException) logger.error("getRawResponse() url=$url header=$headers Cancellation exception0\n: ${e.message}")
        else logger.error("getRawResponse() url=$url header=$headers Generic exception:\n", e)
        null
    }

    override suspend fun headRawResponse(
        url: String,
        headers: Hashtable<String, String>?,
        followRedirect: Boolean,
    ): HttpResponse? = try {
        var cache = true
        client.config {
            cache = this.followRedirects
            this.followRedirects = followRedirect
        }
        val response = client.head {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
        }
        client.config {
            this.followRedirects = cache
        }
        response
    } catch (e: IOException) {
        logger.error("headRawResponse() url=$url header=$headers IOException\n ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        if (e is CancellationException) logger.error("getRawResponse() url=$url header=$headers Cancellation exception0\n: ${e.message}")
        else logger.error("headRawResponse() url=$url header=$headers Generic exception:\n", e)
        null
    }

    override suspend fun getSize(url: String, headers: Hashtable<String, String>?) = try {
        client.head {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) headers {
                    for ((key, value) in it) append(key, value)
                }
            }
        }.run {
            if (status == HttpStatusCode.OK) {
                this.headers["content-length"]?.toLong() ?: Long.MIN_VALUE
            } else {
                Long.MIN_VALUE
            }
        }
    } catch (e: Exception) {
        when (e) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is IOException,
            -> {
                // handle the exception from caller
                1
            }

            else -> {
                logger.error("unhandled exception with calculating size" + e.message)
                throw e
            }
        }
    }

    override suspend fun postRequest(
        url: String,
        headers: Hashtable<String, String>?,
        postData: Hashtable<String, Any>?,
    ): String? {
        return try {
            client.post {
                url(url)
                headers?.let {
                    if (it.isNotEmpty()) headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
                postData?.let {
                    setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
                }
            }.bodyAsText()
        } catch (e: IOException) {
            logger.error("postRequest() url=$url header=$headers IOException\n ${e.message}")
            throw ProxyException(e)
        } catch (e: Exception) {
            logger.error("postRequest() url=$url header=$headers & postRequest=$postData\n Error:", e)
            return null
        }
    }

    override suspend fun postRawResponse(
        url: String,
        headers: Hashtable<String, String>?,
        postData: Hashtable<String, Any>?,
        followRedirect: Boolean,
    ): HttpResponse? = try {
        var cache = true
        client.config {
            cache = this.followRedirects
            this.followRedirects = followRedirect
        }
        val response = client.post {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
            postData?.let {
                setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
            }
        }
        client.config {
            this.followRedirects = cache
        }
        response
    } catch (e: IOException) {
        logger.error("getRawResponse() url=$url header=$headers IOException\n ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        if (e is CancellationException) logger.error("getRawResponse() url=$url header=$headers Cancellation exception:\n ${e.message}")
        else logger.error("getRawResponse() url=$url header=$headers Generic exception:\n", e)
        null
    }

    // Instagram Server crashes with 500 if we sent wrong cookies
    // So it is tackled by hardcoding and making it as true to prevent NonFatal Error
    override suspend fun checkPageAvailability(
        url: String,
        headers: Hashtable<String, String>?,
    ): Boolean {
        val acceptedStatusCode = setOf(
            HttpStatusCode.OK,
            HttpStatusCode.Accepted,
            HttpStatusCode.Created,
            HttpStatusCode.NonAuthoritativeInformation,
            HttpStatusCode.NoContent,
            HttpStatusCode.PartialContent,
            HttpStatusCode.ResetContent,
            HttpStatusCode.MultiStatus,
            if (url.contains("instagram")) HttpStatusCode.InternalServerError else HttpStatusCode.OK
        )
        return try {
            var cacheRedirect = true
            client.config {
                cacheRedirect = followRedirects
                followRedirects = false
            }
            client.head {
                url(url)
                method = HttpMethod.Head
                headers?.let {
                    if (it.isNotEmpty()) headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }.run {
                client.config { followRedirects = cacheRedirect }
                status in acceptedStatusCode || run {
                    if (status in redirectionStatusCode) {
                        val res = getLastPossibleRedirectedResponse(this, headers)
                        val isPageAvailable = res.status in acceptedStatusCode || res.status in redirectionStatusCode
                        logger.info("page availability = $isPageAvailable")
                        return isPageAvailable
                    }
                    logger.warn("Unhandled in checkWebPage() status code=$status for url=$url with headers=$headers}")
                    false
                }
            }
        } catch (e: ClientRequestException) {
            logger.error("checkWebPage() url=$url header=$headers ClientRequestException:\n", e)
            false
        } catch (e: IOException) {
            logger.error("getData() url=$url header=$headers IOException\n ${e.message}")
            throw ProxyException(e)
        } catch (e: Exception) {
            logger.error("checkWebPage() url=$url header=$headers GenericException:", e)
            false
        }
    }

    private suspend fun getLastPossibleRedirectedResponse(
        response: HttpResponse,
        headers: Hashtable<String, String>?,
    ): HttpResponse {
        var cacheFollowRedirection = true
        val nonRedirectingClient = client.config {
            cacheFollowRedirection = followRedirects
            followRedirects = false
        }
        var cnt = 0
        var cacheResponse = response
        do {
            var locationUrl = cacheResponse.headers[HttpHeaders.Location] ?: return cacheResponse

            val matcher = Pattern.compile("^(?:https?://)?(?:[^@\\n]+@)?(?:www\\.)?([^:/\\n?]+)").matcher(locationUrl)
            if (!matcher.find()) locationUrl = cacheResponse.request.url.protocolWithAuthority + locationUrl
            logger.info("redirection ${cacheResponse.request.url}->$locationUrl [${cacheResponse.status.value}]")
            val tempResponse = nonRedirectingClient.get(locationUrl) {
                this.headers {
                    headers?.let {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
            nonRedirectingClient.close()
            if (cacheResponse.request.url == tempResponse.request.url) break
            cacheResponse = tempResponse
            cnt++
        } while (cacheResponse.status in redirectionStatusCode && cnt < 20)
        client.config { followRedirects = cacheFollowRedirection }
        return if (cacheResponse.request.url.host == "localhost") response else cacheResponse
    }

    override fun close() {
        client.close()
    }
}
