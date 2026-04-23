package presentation.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import presentation.controller.InventoryController

fun Route.inventoryRoutes(inventoryController: InventoryController) {
    route("/api/v1/inventory") {
        // Public/IoT scan endpoint
        post("/scan") {
            inventoryController.processScan(call)
        }

        authenticate("auth-jwt") {
            // Admin only registration of tags
            post("/register-tag") {
                inventoryController.registerTag(call)
            }
            
            // Dashboard data
            get("/dashboard") {
                inventoryController.getDashboard(call)
            }
            
            // History per product
            get("/{productId}/history") {
                inventoryController.getHistory(call)
            }
        }
    }
}
