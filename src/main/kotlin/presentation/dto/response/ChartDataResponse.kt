package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class DailyAggregateDto(
    val date: String,
    val totalIn: Int,
    val totalOut: Int,
    val netFlow: Int
)

@Serializable
data class ForecastingResultDto(
    val targetDate: String,
    val predictedValue: Double,
    val lowerBound: Double,
    val upperBound: Double
)

@Serializable
data class ChartDataResponse(
    val product: ProductResponse,
    val currentStock: Int,
    val estimatedStockOutDate: String?,
    val remainingDays: Long?,
    val historicalData: List<DailyAggregateDto>,
    val forecastingData: List<ForecastingResultDto>,
    val rangeTotalIn: Int = 0,
    val rangeTotalOut: Int = 0
)
