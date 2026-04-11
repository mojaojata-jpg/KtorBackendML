package presentation.controller

import application.usecase.stock.GetStockDashboardUseCase
import application.usecase.sensor.GetSensorHistoryUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import presentation.dto.response.BaseResponse
import presentation.dto.response.StockHistoryResponse

class StockController(
    private val getStockDashboardUseCase: GetStockDashboardUseCase,
    private val getSensorHistoryUseCase: GetSensorHistoryUseCase
) {
    suspend fun getStockDashboard(call: ApplicationCall) {
        try {
            val dashboardData = getStockDashboardUseCase()
            val response = BaseResponse(
                success = true,
                data = dashboardData,
                message = "Stock dashboard data retrieved successfully"
            )
            call.respond(HttpStatusCode.OK, response)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error: ${e.message}")
            )
        }
    }

    suspend fun getHistory(call: ApplicationCall) {
        try {
            val productId = call.parameters["productId"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50

            if (productId == null) {
                call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(success = false, message = "Product ID is required"))
                return
            }

            val history = getSensorHistoryUseCase(productId, limit)
            
            // Map ke DTO yang lebih ramping (hanya field yang dibutuhkan Android)
            val responseData = history.map { 
                StockHistoryResponse(
                    estimatedStock = it.estimatedStock,
                    recordedAt = it.recordedAt
                )
            }

            call.respond(
                HttpStatusCode.OK,
                BaseResponse(
                    success = true,
                    data = responseData,
                    message = "Sensor history retrieved successfully"
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error: ${e.message}")
            )
        }
    }
}
