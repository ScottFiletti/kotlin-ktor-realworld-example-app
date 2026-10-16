package io.realworld.app.ext

import io.ktor.application.ApplicationCall
import io.ktor.request.receive

// Like receive(), but a body Ktor can't read at all (e.g. no body and no Content-Length) becomes
// a 422 validation error instead of an IllegalStateException that would surface as a 500.
// Malformed JSON already raises JsonProcessingException, which is mapped to 422 separately.
suspend inline fun <reified T : Any> ApplicationCall.receiveBody(): T =
    try {
        receive()
    } catch (e: IllegalStateException) {
        throw IllegalArgumentException("Request body is missing or invalid.", e)
    }
