package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ScanResponse(
    val new_stock: Int,
    val status: String
)

@Serializable
data class RegisterTagResponse(
    val tag_id: String,
    val current_stock: Int
)

@Serializable
data class DashboardResponse(
    val product_id: String,
    val product_name: String,
    val product_code: String,
    val current_stock: Int,
    val total_incoming: Int,
    val total_outgoing: Int,
    val status: String,
    val unit: String
)

@Serializable
data class HistoryResponse(
    val event_type: String,
    val quantity: Int,
    val recorded_at: String,
    val note: String? = null
)
