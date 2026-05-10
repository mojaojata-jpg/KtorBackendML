package presentation.controller

import application.usecase.GetDailyReportUseCase
import application.usecase.GetMonthlySummaryUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import presentation.dto.response.BaseResponse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

class ReportController(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getDailyReportUseCase: GetDailyReportUseCase
) {
    suspend fun getMonthlySummary(call: ApplicationCall) {
        val productId = call.parameters["productId"]
        if (productId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(
                success = false, message = "Product ID is required"
            ))
            return
        }

        val monthParam = call.request.queryParameters["month"]
        if (monthParam.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(
                success = false, message = "Query parameter 'month' is required (format: YYYY-MM, e.g., 2026-04)"
            ))
            return
        }

        // Validate month format
        try {
            YearMonth.parse(monthParam)
        } catch (e: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(
                success = false, message = "Invalid month format. Use YYYY-MM (e.g., 2026-04)"
            ))
            return
        }

        try {
            val result = getMonthlySummaryUseCase.execute(productId, monthParam)
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(
                    success = false, message = "Product not found: $productId"
                ))
                return
            }
            call.respond(HttpStatusCode.OK, BaseResponse(
                success = true, data = result, message = "Monthly summary for $monthParam retrieved successfully"
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, BaseResponse<Unit>(
                success = false, message = "Failed to get monthly summary: ${e.message}"
            ))
        }
    }

    suspend fun getDailyReport(call: ApplicationCall) {
        val dateParam = call.request.queryParameters["date"]
        val targetDate = try {
            if (dateParam != null) LocalDate.parse(dateParam) else LocalDate.now()
        } catch (e: DateTimeParseException) {
            call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(
                success = false, message = "Invalid date format. Use YYYY-MM-DD (e.g., 2026-05-10)"
            ))
            return
        }

        try {
            val report = getDailyReportUseCase.execute(targetDate)
            call.respond(HttpStatusCode.OK, BaseResponse(
                success = true, data = report, message = "Daily report for $targetDate generated successfully"
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, BaseResponse<Unit>(
                success = false, message = "Failed to generate daily report: ${e.message}"
            ))
        }
    }
}
