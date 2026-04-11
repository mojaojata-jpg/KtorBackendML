package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class StockHistoryResponse(
    val estimatedStock: Int,
    val recordedAt: String
)
