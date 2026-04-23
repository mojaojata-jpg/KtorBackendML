package domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Product(
    val id: UUID? = null,
    val name: String,
    val code: String,
    val unitLabel: String = "pcs",
    val minStockThreshold: Int = 0,
    val description: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
