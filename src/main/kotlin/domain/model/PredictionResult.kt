package domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PredictionResult(
    val id: UUID? = null,
    val productId: UUID,
    val modelName: String,
    val modelVersion: String,
    val currentStock: Int,
    val predictedDaysRemaining: Int,
    val predictedStockOutDate: LocalDate,
    val confidenceScore: Double? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
