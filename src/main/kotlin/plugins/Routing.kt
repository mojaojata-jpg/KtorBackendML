package plugins

import infrastructure.di.AppComponent
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import presentation.routes.authRoutes
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting(appComponent: AppComponent) {
    install(RateLimit) {
        register(RateLimitName("auth-limit")) {
            rateLimiter(limit = 3, refillPeriod = 10.seconds)
        }
    }

    routing {
        rateLimit(RateLimitName("auth-limit")) {
            authRoutes(appComponent.controllerModule.authController)
        }
    }
}
