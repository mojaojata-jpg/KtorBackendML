package domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class PredictionResult(
    val id: String? = null,
    val productId: String,
    val modelName: String,
    val modelVersion: String,
    val predictedDaysRemaining: Int,
    val predictedStockOutDate: String, // Use String for Date to simplify serialization
    val confidenceScore: Double?,
    val createdAt: String? = null
)
