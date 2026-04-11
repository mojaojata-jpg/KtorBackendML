package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PredictionResponse(
    val productId: String,
    val modelName: String,
    val modelVersion: String,
    val predictedDaysRemaining: Int,
    val predictedStockOutDate: String,
    val confidenceScore: Double?,
    val createdAt: String?
)
