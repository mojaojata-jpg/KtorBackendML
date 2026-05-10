package application.usecase

import domain.repository.AggregateRepository
import java.time.LocalDate

class RunDailyAggregationUseCase(
    private val aggregateRepository: AggregateRepository
) {
    suspend fun execute(date: LocalDate = LocalDate.now()) {
        aggregateRepository.calculateAndUpsertDaily(date)
    }
}
