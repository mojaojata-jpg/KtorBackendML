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
        register(RateLimitName("sensor-limit")) {
            rateLimiter(limit = 10, refillPeriod = 1.seconds)
        }
    }

    routing {
        // 2. Auth Routes (Public)
        rateLimit(RateLimitName("auth-limit")) {
            authRoutes(appComponent.controllerModule.authController)
        }

        // 3. Product Routes (Protected)
        productRoutes(appComponent.controllerModule.productController)

        // 4. IoT Device Routes (Public & Protected)
        iotDeviceRoutes(appComponent.controllerModule.iotDeviceController)

        // 5. Sensor Data Ingestion (Public with Rate Limit)
        rateLimit(RateLimitName("sensor-limit")) {
            sensorRoutes(appComponent.controllerModule.sensorController)
        }

        // 6. Stock Monitoring Routes (Protected)
        stockRoutes(appComponent.controllerModule.stockController)

        // 7. Prediction Monitoring Routes (Protected)
        predictionRoutes(appComponent.controllerModule.predictionController)
    }
}
