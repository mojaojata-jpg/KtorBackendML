package presentation.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import presentation.controller.StockController

fun Route.stockRoutes(stockController: StockController) {
    authenticate("auth-jwt") {
        route("/api/v1/stocks") {
            get {
                stockController.getStockDashboard(call)
            }
            
            // GET /api/v1/stocks/{productId}/history
            get("/{productId}/history") {
                stockController.getHistory(call)
            }
        }
    }
}
