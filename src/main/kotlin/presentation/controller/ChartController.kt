package presentation.controller

import application.usecase.GetChartDataUseCase
import application.usecase.RunDailyAggregationUseCase
import application.usecase.SyncAggregationUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import presentation.dto.response.BaseResponse
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Serializable
data class SyncResultData(
    val date: String,
    val productsSynced: Int
)

class ChartController(
    private val getChartDataUseCase: GetChartDataUseCase,
    private val runDailyAggregationUseCase: RunDailyAggregationUseCase,
    private val syncAggregationUseCase: SyncAggregationUseCase
) {
    suspend fun getChartData(call: ApplicationCall) {
        val productId = call.parameters["productId"]
        val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
        
        if (productId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Product ID is required"))
            return
        }

        try {
            val chartData = getChartDataUseCase.execute(productId, days)
            if (chartData == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
                return
            }
            call.respond(HttpStatusCode.OK, chartData)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch chart data: ${e.message}"))
        }
    }

    suspend fun triggerAggregationManually(call: ApplicationCall) {
        val dateParam = call.request.queryParameters["date"]
        val targetDate = try {
            if (dateParam != null) LocalDate.parse(dateParam) else LocalDate.now()
        } catch (e: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid date format. Use YYYY-MM-DD"))
            return
        }

        try {
            runDailyAggregationUseCase.execute(targetDate)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Aggregation for $targetDate triggered successfully"))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to run aggregation: ${e.message}"))
        }
    }

    suspend fun syncAggregation(call: ApplicationCall) {
        val dateParam = call.request.queryParameters["date"]
        val targetDate = try {
            if (dateParam != null) LocalDate.parse(dateParam) else LocalDate.now()
        } catch (e: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(
                success = false,
                message = "Invalid date format. Use YYYY-MM-DD (e.g., 2026-05-06)"
            ))
            return
        }

        try {
            val syncedCount = syncAggregationUseCase.execute(targetDate)
            call.respond(HttpStatusCode.OK, BaseResponse(
                success = true,
                data = SyncResultData(
                    date = targetDate.toString(),
                    productsSynced = syncedCount
                ),
                message = "Aggregation sync completed for $targetDate. $syncedCount product(s) processed."
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, BaseResponse<Unit>(
                success = false,
                message = "Failed to sync aggregation: ${e.message}"
            ))
        }
    }
}

