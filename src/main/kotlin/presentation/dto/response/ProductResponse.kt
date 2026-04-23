package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val code: String,
    val unitLabel: String,
    val minStockThreshold: Int,
    val description: String? = null,
    val imageUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)
