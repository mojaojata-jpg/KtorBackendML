package presentation.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import presentation.controller.PredictionController

fun Route.predictionRoutes(predictionController: PredictionController) {
    authenticate("auth-jwt") {
        route("/api/v1/predictions") {
            get {
                predictionController.getAllPredictions(call)
            }
            get("/{productId}") {
                predictionController.getPredictionByProduct(call)
            }
        }
    }
}
