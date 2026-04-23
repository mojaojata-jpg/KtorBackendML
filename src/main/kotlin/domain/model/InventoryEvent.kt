package domain.model

import java.time.LocalDateTime
import java.util.UUID

data class InventoryEvent(
    val id: UUID? = null,
    val productId: UUID,
    val tagId: UUID? = null,
    val adminId: UUID? = null,
    val eventType: String, // REGISTER, IN, OUT, ADJUSTMENT
    val quantity: Int = 1,
    val note: String? = null,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)
