package presentation.routes

import infrastructure.di.ControllerModule
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.reportRoutes(controllerModule: ControllerModule) {
    authenticate("auth-jwt") {
        route("/api/v1/reports") {
            // GET /api/v1/reports/daily?date=2026-05-10
            get("/daily") {
                controllerModule.reportController.getDailyReport(call)
            }
        }

        // Monthly summary per product (protected)
        route("/api/v1/inventory") {
            // GET /api/v1/inventory/{productId}/monthly-summary?month=2026-04
            get("/{productId}/monthly-summary") {
                controllerModule.reportController.getMonthlySummary(call)
            }
        }
    }
}
