package domain.model

import java.time.LocalDateTime
import java.util.UUID

data class InventorySnapshot(
    val id: UUID? = null,
    val productId: UUID,
    val currentStock: Int,
    val status: String, // OUT_OF_STOCK, LOW_STOCK, SUFFICIENT
    val snapshotTime: LocalDateTime = LocalDateTime.now(),
    val sourceEventId: UUID? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
