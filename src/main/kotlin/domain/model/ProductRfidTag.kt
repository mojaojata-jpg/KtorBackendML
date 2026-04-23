package domain.model

import java.time.LocalDateTime
import java.util.UUID

data class ProductRfidTag(
    val id: UUID? = null,
    val productId: UUID,
    val tagUid: String,
    val tagLabel: String? = null,
    val status: String = "ACTIVE",
    val registeredByAdminId: UUID? = null,
    val registeredAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
