package application.usecase

import domain.repository.AggregateRepository
import domain.repository.ProductRepository
import presentation.dto.response.DailyAggregateDto
import presentation.dto.response.MonthlySummaryResponse
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class GetMonthlySummaryUseCase(
    private val productRepository: ProductRepository,
    private val aggregateRepository: AggregateRepository
) {
    suspend fun execute(productIdStr: String, monthStr: String): MonthlySummaryResponse? {
        val product = productRepository.findById(productIdStr) ?: return null

        // Parse month string (format: YYYY-MM, e.g., "2026-04")
        val yearMonth = YearMonth.parse(monthStr)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        val aggregates = aggregateRepository.getByProductAndDateRange(
            UUID.fromString(productIdStr), startDate, endDate
        )

        val totalIn = aggregates.sumOf { it.totalIn }
        val totalOut = aggregates.sumOf { it.totalOut }

        return MonthlySummaryResponse(
            productId = product.id.toString(),
            productName = product.name,
            productCode = product.code,
            month = monthStr,
            totalIn = totalIn,
            totalOut = totalOut,
            netFlow = totalIn - totalOut,
            dailyBreakdown = aggregates.map {
                DailyAggregateDto(
                    date = it.date.toString(),
                    totalIn = it.totalIn,
                    totalOut = it.totalOut,
                    netFlow = it.netFlow
                )
            }
        )
    }
}
