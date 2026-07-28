/*
 * Module: r2-streamer-kotlin
 * Developers: Aferdita Muriqi, Clément Baumann
 *
 * Copyright (c) 2018. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.streamer.server.handler

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.router.RouterNanoHTTPD
import org.readium.r2.streamer.server.Ressources


class CSSHandler : RouterNanoHTTPD.DefaultHandler() {

    override fun getMimeType(): String? {
        return null
    }

    override fun getText(): String {
        return ResponseStatus.FAILURE_RESPONSE
    }

    override fun getStatus(): NanoHTTPD.Response.IStatus {
        return NanoHTTPD.Response.Status.OK
    }

    override fun get(uriResource: RouterNanoHTTPD.UriResource?, urlParams: Map<String, String>?, session: NanoHTTPD.IHTTPSession?): NanoHTTPD.Response {

        val method = session!!.method
        var uri = session.uri

        Log.v(TAG, "Method: $method, Uri: $uri")

        return try {
            val lastSlashIndex = uri.lastIndexOf('/')
            uri = uri.substring(lastSlashIndex + 1, uri.length)
            val resources = uriResource!!.initParameter(Ressources::class.java)
            val x = createResponse(NanoHTTPD.Response.Status.OK, "text/css", resources.get(uri))
            x
        } catch (e: Exception) {
            Log.e(TAG, "Exception in get", e)
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, mimeType, ResponseStatus.FAILURE_RESPONSE)
        }

    }

    private fun createResponse(status: NanoHTTPD.Response.Status, mimeType: String, message: String): NanoHTTPD.Response {
        val response = NanoHTTPD.newFixedLengthResponse(status, mimeType, message)
        response.addHeader("Accept-Ranges", "bytes")
        return response
    }

    companion object {
        private val TAG: String = CSSHandler::class.java.simpleName
    }
}