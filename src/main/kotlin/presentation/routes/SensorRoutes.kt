package presentation.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import presentation.controller.SensorController

fun Route.sensorRoutes(sensorController: SensorController) {
    route("/api/v1/sensor-readings") {
        // IoT Device Ingestion (Public with Rate Limit in Routing.kt)
        post {
            sensorController.processSensorData(call)
        }
    }
}
