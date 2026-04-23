package presentation.controller

import application.usecase.prediction.GetAllPredictionsUseCase
import application.usecase.prediction.GetPredictionByProductIdUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import presentation.dto.response.BaseResponse
import presentation.dto.response.PredictionResponse

class PredictionController(
    private val getAllPredictionsUseCase: GetAllPredictionsUseCase,
    private val getPredictionByProductIdUseCase: GetPredictionByProductIdUseCase
) {
    // GET /api/v1/predictions
    suspend fun getAllPredictions(call: ApplicationCall) {
        try {
            val predictions = getAllPredictionsUseCase()
            val responseData = predictions.map { 
                PredictionResponse(
                    productId = it.productId.toString(),
                    modelName = it.modelName,
                    modelVersion = it.modelVersion,
                    predictedDaysRemaining = it.predictedDaysRemaining,
                    predictedStockOutDate = it.predictedStockOutDate.toString(),
                    confidenceScore = it.confidenceScore,
                    createdAt = it.createdAt.toString()
                )
            }

            call.respond(
                HttpStatusCode.OK,
                BaseResponse(
                    success = true,
                    data = responseData,
                    message = "All prediction results retrieved successfully"
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error: ${e.message}")
            )
        }
    }

    // GET /api/v1/predictions/{productId}
    suspend fun getPredictionByProduct(call: ApplicationCall) {
        try {
            val productId = call.parameters["productId"]
            if (productId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, BaseResponse<Unit>(success = false, message = "Product ID is required"))
                return
            }
            
            val prediction = getPredictionByProductIdUseCase(productId)
            if (prediction == null) {
                call.respond(
                    HttpStatusCode.OK, 
                    BaseResponse<Unit?>(success = true, data = null, message = "No prediction data found for this product")
                )
                return
            }

            val responseData = PredictionResponse(
                productId = prediction.productId.toString(),
                modelName = prediction.modelName,
                modelVersion = prediction.modelVersion,
                predictedDaysRemaining = prediction.predictedDaysRemaining,
                predictedStockOutDate = prediction.predictedStockOutDate.toString(),
                confidenceScore = prediction.confidenceScore,
                createdAt = prediction.createdAt.toString()
            )

            call.respond(
                HttpStatusCode.OK,
                BaseResponse(
                    success = true,
                    data = responseData,
                    message = "Latest prediction result retrieved successfully"
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
