package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    val name: String,
    val code: String,
    val unitWeight: Double,
    val minStockThreshold: Int,
    val description: String? = null
)
