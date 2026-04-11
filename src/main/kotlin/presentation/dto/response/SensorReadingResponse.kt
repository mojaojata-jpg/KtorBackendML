package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class SensorReadingResponse(
    val estimated_stock: Int,
    val validation_status: String,
    val is_anomaly: Boolean
)
