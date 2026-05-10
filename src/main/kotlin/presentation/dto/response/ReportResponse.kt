package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class MonthlySummaryResponse(
    val productId: String,
    val productName: String,
    val productCode: String,
    val month: String,
    val totalIn: Int,
    val totalOut: Int,
    val netFlow: Int,
    val dailyBreakdown: List<DailyAggregateDto>
)

@Serializable
data class DailyReportResponse(
    val date: String,
    val products: List<ProductDailyStats>,
    val grandTotalIn: Int,
    val grandTotalOut: Int,
    val grandNetFlow: Int,
    val scanLogs: List<ScanLogEntry>
)

@Serializable
data class ProductDailyStats(
    val productId: String,
    val productName: String,
    val productCode: String,
    val unit: String,
    val totalIn: Int,
    val totalOut: Int,
    val netFlow: Int,
    val currentStock: Int,
    val status: String
)

@Serializable
data class ScanLogEntry(
    val eventType: String,
    val productName: String,
    val productCode: String,
    val quantity: Int,
    val recordedAt: String,
    val note: String? = null
)
