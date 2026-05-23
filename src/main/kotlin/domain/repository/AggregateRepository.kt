package domain.repository

import domain.model.DailyAggregate
import java.time.LocalDate
import java.util.UUID

interface AggregateRepository {
    suspend fun calculateAndUpsertDaily(date: LocalDate)
    suspend fun syncAllProductsForDate(date: LocalDate): Int
    suspend fun getByProductAndDateRange(productId: UUID, startDate: LocalDate, endDate: LocalDate): List<DailyAggregate>
    suspend fun countByProduct(productId: UUID): Long
}
