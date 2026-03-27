package presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val name: String,
    val code: String,
    val unitWeight: Double,
    val minStockThreshold: Int,
    val description: String? = null
)
