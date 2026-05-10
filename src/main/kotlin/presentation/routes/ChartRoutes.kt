package presentation.routes

import infrastructure.di.ControllerModule
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.chartRoutes(controllerModule: ControllerModule) {
    // Public chart & aggregation routes
    route("/api/v1/inventory") {
        get("/chart-data/{productId}") {
            controllerModule.chartController.getChartData(call)
        }
        
        post("/aggregate/manual") {
            controllerModule.chartController.triggerAggregationManually(call)
        }
    }

    // Protected Admin-only sync route
    authenticate("auth-jwt") {
        route("/api/admin/inventory/aggregate") {
            post("/sync") {
                controllerModule.chartController.syncAggregation(call)
            }
        }
    }
}

