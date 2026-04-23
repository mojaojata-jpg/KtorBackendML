package presentation.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ProductRequest(
    val name: String,
    val code: String,
    val unitLabel: String = "pcs",
    val minStockThreshold: Int,
    val description: String? = null,
    val imageUrl: String? = null
)
