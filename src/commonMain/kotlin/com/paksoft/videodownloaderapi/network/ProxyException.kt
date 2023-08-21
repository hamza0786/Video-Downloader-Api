

package com.paksoft.videodownloaderapi.network

/**
 * Sometimes there might be trouble connecting
 * you with the social media's server.
 * If you get this error kindly retry the request.
 */
class ProxyException(exception: Exception? = null) :
    IllegalStateException("Unable to process request because of connection problem", exception)
