package presentation.controller

import application.usecase.sensor.ProcessSensorReadingUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import presentation.dto.request.SensorDataRequest
import presentation.dto.response.BaseResponse
import presentation.dto.response.SensorReadingResponse

class SensorController(
    private val processSensorReadingUseCase: ProcessSensorReadingUseCase
) {
    suspend fun processSensorData(call: ApplicationCall) {
        try {
            val request = call.receive<SensorDataRequest>()
            val reading = processSensorReadingUseCase(
                deviceCode = request.device_code,
                rawWeight = request.raw_weight,
                filteredWeight = request.filtered_weight,
                recordedAt = request.recorded_at
            )
            
            val responseData = SensorReadingResponse(
                estimated_stock = reading.estimatedStock,
                validation_status = reading.validationStatus,
                is_anomaly = reading.isAnomaly
            )

            val response = BaseResponse(
                success = true,
                data = responseData,
                message = "Sensor data processed successfully"
            )

            call.respond(HttpStatusCode.OK, response)
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                BaseResponse<Unit>(success = false, message = e.message ?: "Invalid request data")
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error: ${e.message}")
            )
        }
    }
}
