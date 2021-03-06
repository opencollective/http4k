package org.http4k.server

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import io.undertow.util.HttpString
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters

/**
 * Exposed to allow for insertion into a customised Undertow server instance
 */
class HttpUndertowHandler(handler: HttpHandler) : io.undertow.server.HttpHandler {
    private val safeHandler = ServerFilters.CatchAll().then(handler)

    private fun Response.into(exchange: HttpServerExchange) {
        exchange.statusCode = status.code
        headers.forEach {
            exchange.responseHeaders.put(HttpString(it.first), it.second)
        }
        body.stream.copyTo(exchange.outputStream)
    }

    private fun HttpServerExchange.asRequest(): Request {
        val uri = Uri.of(relativePath + "?" + queryString)
        return requestHeaders
            .flatMap { header -> header.map { header.headerName to it } }
            .fold(Request(Method.valueOf(requestMethod.toString()), uri)) {
                memo, (first, second) ->
                memo.header(first.toString(), second)
            }.body(inputStream)
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        if (exchange.isInIoThread) exchange.dispatch(this) else safeHandler(exchange.asRequest()).into(exchange)
    }
}

data class Undertow(val port: Int = 8000) : ServerConfig {
    override fun toServer(handler: HttpHandler): Http4kServer {
        return object : Http4kServer {
            val server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(BlockingHandler(HttpUndertowHandler(handler))).build()

            override fun start(): Http4kServer {
                server.start()
                return this
            }

            override fun stop() = server.stop()
        }
    }
}