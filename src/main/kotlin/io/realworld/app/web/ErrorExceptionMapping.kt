package io.realworld.app.web

import com.fasterxml.jackson.core.JsonProcessingException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.realworld.app.domain.exceptions.NotFoundException
import io.realworld.app.domain.exceptions.UnauthorizedException

internal data class ErrorResponse(val errors: Map<String, List<String?>>)

private fun errorBody(message: String?) = ErrorResponse(mapOf("body" to listOf(message)))

// Maps domain exceptions to RealWorld status codes and error format: {"errors":{"body":["..."]}}.
// StatusPages picks the most specific handler for the thrown exception's class.
fun StatusPages.Configuration.errorMappings() {
    exception<IllegalArgumentException> { cause ->
        call.respond(HttpStatusCode.UnprocessableEntity, errorBody(cause.message))
    }
    exception<JsonProcessingException> {
        call.respond(HttpStatusCode.UnprocessableEntity, errorBody("Request body is missing or invalid."))
    }
    exception<UnauthorizedException> { cause ->
        call.respond(HttpStatusCode.Unauthorized, errorBody(cause.message))
    }
    exception<NotFoundException> { cause ->
        call.respond(HttpStatusCode.NotFound, errorBody(cause.message))
    }
    // Kotlin's TODO() throws NotImplementedError; stub endpoints use it to answer 501 instead of a misleading 404.
    exception<NotImplementedError> { cause ->
        call.respond(HttpStatusCode.NotImplemented, errorBody(cause.message?.removePrefix("An operation is not implemented: ")))
    }
    exception<Exception> { cause ->
        call.application.environment.log.error("Unhandled exception", cause)
        call.respond(HttpStatusCode.InternalServerError, errorBody("Internal server error."))
    }
}
