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
import kotlinx.coroutines.async

class GetChartDataUseCase(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val aggregateRepository: AggregateRepository,
    private val forecastRepository: ForecastRepository
) {
    suspend fun execute(productIdStr: String, daysHistory: Int = 30): ChartDataResponse? = kotlinx.coroutines.coroutineScope {
        val product = productRepository.findById(productIdStr) ?: return@coroutineScope null
        val productId = UUID.fromString(productIdStr)

        val today = LocalDate.now()
        val startDate = today.minusDays(daysHistory.toLong())

        // Parallel Query Optimization: Fetch independent datasets concurrently to cut down physical latency
        val snapshotDeferred = async { inventoryRepository.getLatestSnapshot(productId) }
        val aggregatesDeferred = async { aggregateRepository.getByProductAndDateRange(productId, startDate, today) }
        val forecastsDeferred = async { forecastRepository.getForecastsByProduct(productId, today.plusDays(1)) }
        val statsDeferred = async { inventoryRepository.getProductStats(productId, startDate, today) }
        val historyDaysCountDeferred = async { aggregateRepository.countByProduct(productId) }

        val snapshot = snapshotDeferred.await()
        val aggregates = aggregatesDeferred.await()
        val forecasts = forecastsDeferred.await()
        val (rangeTotalIn, rangeTotalOut) = statsDeferred.await()
        val historyDaysCount = historyDaysCountDeferred.await()

        val currentStock = snapshot?.currentStock ?: 0

        // LOGIC: Strict 60-day minimum requirement for prediction
        val hasEnoughData = historyDaysCount >= 60
        val validForecasts = if (hasEnoughData) forecasts else emptyList()

        // Logic to calculate estimated stock out date
        var tempStock = currentStock.toDouble()
        var estimatedStockOutDate: LocalDate? = null
        
        for (forecast in validForecasts) {
            tempStock -= forecast.predictedValue
            if (tempStock <= 0) {
                estimatedStockOutDate = forecast.targetDate
                break
            }
        }

        val remainingDays = estimatedStockOutDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }

        ChartDataResponse(
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
            remainingDays = remainingDays ?: 0, // Explicitly set to 0 if no valid prediction
            historicalData = aggregates.map { 
                DailyAggregateDto(
                    date = it.date.toString(),
                    totalIn = it.totalIn,
                    totalOut = it.totalOut,
                    netFlow = it.netFlow
                )
            },
            forecastingData = validForecasts.map {
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
