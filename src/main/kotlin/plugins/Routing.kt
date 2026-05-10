package plugins

import infrastructure.di.AppComponent
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import presentation.routes.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting(appComponent: AppComponent) {
    // 1. Install Rate Limit Plugin
    install(RateLimit) {
        register(RateLimitName("auth-limit")) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
        }
    }

    routing {
        // 2. Auth Routes (Public)
        rateLimit(RateLimitName("auth-limit")) {
            authRoutes(appComponent.controllerModule.authController)
        }

        // 3. Product Routes (Protected)
        productRoutes(appComponent.controllerModule.productController)

        // 4. Inventory Routes (RFID Based)
        inventoryRoutes(appComponent.controllerModule.inventoryController)

        // 6. Chart & Aggregation Routes
        chartRoutes(appComponent.controllerModule)

        // 7. Report Routes (Monthly Summary, Daily Report)
        reportRoutes(appComponent.controllerModule)
    }
}

