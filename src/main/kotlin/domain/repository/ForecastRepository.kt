package domain.repository

import domain.model.ForecastingResult
import java.time.LocalDate
import java.util.UUID

interface ForecastRepository {
    suspend fun getForecastsByProduct(productId: UUID, fromDate: LocalDate): List<ForecastingResult>
}
