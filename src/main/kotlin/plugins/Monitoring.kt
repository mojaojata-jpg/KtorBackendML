package plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.event.Level
import presentation.dto.response.BaseResponse

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        // Tangkap IllegalArgumentException (biasanya dari UseCase validation)
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                BaseResponse<Unit>(success = false, message = cause.message ?: "Invalid request parameters")
            )
        }

        // Tangkap Error Umum
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error: ${cause.localizedMessage}")
            )
        }

        // Tangkap 404
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                BaseResponse<Unit>(success = false, message = "Endpoint or Resource not found")
            )
        }
        
        // Tangkap 429 Too Many Requests (Rate Limit)
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(
                status,
                BaseResponse<Unit>(success = false, message = "Too many requests. Please try again later.")
            )
        }
    }
}
