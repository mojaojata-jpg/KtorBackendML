package application.usecase

import domain.repository.AggregateRepository
import java.time.LocalDate

class SyncAggregationUseCase(
    private val aggregateRepository: AggregateRepository
) {
    suspend fun execute(date: LocalDate = LocalDate.now()): Int {
        return aggregateRepository.syncAllProductsForDate(date)
    }
}
