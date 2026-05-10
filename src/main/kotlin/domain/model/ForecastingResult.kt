package domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ForecastingResult(
    val id: UUID? = null,
    val productId: UUID,
    val targetDate: LocalDate,
    val predictedValue: Double,
    val lowerBound: Double,
    val upperBound: Double,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
