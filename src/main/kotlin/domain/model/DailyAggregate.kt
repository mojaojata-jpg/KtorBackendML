package domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DailyAggregate(
    val id: UUID? = null,
    val productId: UUID,
    val date: LocalDate,
    val totalIn: Int,
    val totalOut: Int,
    val netFlow: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
