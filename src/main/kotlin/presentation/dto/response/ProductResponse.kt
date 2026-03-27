package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val code: String,
    val unitWeight: Double,
    val minStockThreshold: Int,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
)
