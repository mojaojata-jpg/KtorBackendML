package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class StockDashboardResponse(
    val productId: String,
    val productName: String,
    val productCode: String,
    val currentWeight: Double,
    val currentStock: Int,
    val minStockThreshold: Int,
    val stockStatus: String,
    val status: String,
    val lastUpdated: String
)
