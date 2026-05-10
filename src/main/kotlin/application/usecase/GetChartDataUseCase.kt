package application.usecase

import domain.repository.AggregateRepository
import domain.repository.ForecastRepository
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import presentation.dto.response.ChartDataResponse
import presentation.dto.response.DailyAggregateDto
import presentation.dto.response.ForecastingResultDto
import presentation.dto.response.ProductResponse
import java.time.LocalDate
import java.util.UUID

class GetChartDataUseCase(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val aggregateRepository: AggregateRepository,
    private val forecastRepository: ForecastRepository
) {
    suspend fun execute(productIdStr: String, daysHistory: Int = 30): ChartDataResponse? {
        val product = productRepository.findById(productIdStr) ?: return null
        val productId = UUID.fromString(productIdStr)

        val today = LocalDate.now()
        val startDate = today.minusDays(daysHistory.toLong())

        val snapshot = inventoryRepository.getLatestSnapshot(productId)
        val currentStock = snapshot?.currentStock ?: 0

        val aggregates = aggregateRepository.getByProductAndDateRange(productId, startDate, today)
        val forecasts = forecastRepository.getForecastsByProduct(productId, today.plusDays(1))

        // Calculate Range Specific Stats (Sync Fix)
        val (rangeTotalIn, rangeTotalOut) = inventoryRepository.getProductStats(productId, startDate, today)

        // Logic to calculate estimated stock out date
        var tempStock = currentStock.toDouble()
        var estimatedStockOutDate: LocalDate? = null
        
        for (forecast in forecasts) {
            tempStock -= forecast.predictedValue
            if (tempStock <= 0) {
                estimatedStockOutDate = forecast.targetDate
                break
            }
        }

        val remainingDays = estimatedStockOutDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }

        return ChartDataResponse(
            product = ProductResponse(
                id = product.id.toString(),
                name = product.name,
                code = product.code,
                unitLabel = product.unitLabel,
                minStockThreshold = product.minStockThreshold,
                description = product.description,
                imageUrl = product.imageUrl,
                createdAt = product.createdAt.toString(),
                updatedAt = product.updatedAt.toString()
            ),
            currentStock = currentStock,
            estimatedStockOutDate = estimatedStockOutDate?.toString(),
            remainingDays = remainingDays,
            historicalData = aggregates.map { 
                DailyAggregateDto(
                    date = it.date.toString(),
                    totalIn = it.totalIn,
                    totalOut = it.totalOut,
                    netFlow = it.netFlow
                )
            },
            forecastingData = forecasts.map {
                ForecastingResultDto(
                    targetDate = it.targetDate.toString(),
                    predictedValue = it.predictedValue,
                    lowerBound = it.lowerBound,
                    upperBound = it.upperBound
                )
            },
            rangeTotalIn = rangeTotalIn,
            rangeTotalOut = rangeTotalOut
        )
    }
}
