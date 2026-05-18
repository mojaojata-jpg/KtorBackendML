package application.usecase.inventory

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import domain.repository.AggregateRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ProcessRfidScanUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val aggregateRepository: AggregateRepository
) {
    suspend operator fun invoke(
        tagUid: String,
        eventType: String, // IN, OUT
        adminId: UUID? = null,
        note: String? = null,
        isContinuousMode: Boolean = false // Tambahkan flag
    ): Pair<InventoryEvent, InventorySnapshot> {
        // 1. Find the tag
        val tag = inventoryRepository.findTagByUid(tagUid)
            ?: throw IllegalArgumentException("RFID Tag not registered: $tagUid")

        // 2. State Validation (REMOVED: Tag is now stateless, just an identifier for the product)
        // A single tag can be used repeatedly for IN and OUT scans without becoming INACTIVE.

        // 3. Get Product Info
        val product = productRepository.findById(tag.productId.toString())
            ?: throw IllegalArgumentException("Product not found for tag: $tagUid")

        // 4. Record Event
        val event = InventoryEvent(
            productId = tag.productId,
            tagId = tag.id,
            adminId = adminId,
            eventType = eventType,
            quantity = 1, // Standardized: Always positive 1, movement direction defined by eventType
            note = note,
            recordedAt = LocalDateTime.now()
        )
        val recordedEvent = inventoryRepository.recordEvent(event)

        // 5. Update Tag Status (REMOVED)
        // Tag is stateless, so we no longer update it to INACTIVE or ACTIVE on scan.

        // 6. Calculate New Stock (Snapshot)
        val latestSnapshot = inventoryRepository.getLatestSnapshot(tag.productId)
        val previousStock = latestSnapshot?.currentStock ?: 0
        val newStock = if (eventType == "IN") previousStock + 1 else previousStock - 1
        
        // Ensure stock doesn't go below zero
        val finalStock = if (newStock < 0) 0 else newStock

        // 5. Determine Status
        val status = when {
            finalStock == 0 -> "OUT_OF_STOCK"
            finalStock <= product.minStockThreshold -> "LOW_STOCK"
            else -> "SUFFICIENT"
        }

        val snapshot = InventorySnapshot(
            productId = tag.productId,
            currentStock = finalStock,
            status = status,
            sourceEventId = recordedEvent.id
        )
        val savedSnapshot = inventoryRepository.saveSnapshot(snapshot)

        // Real-time Aggregation: Sync aggregate stats for this product today
        try {
            aggregateRepository.calculateAndUpsertDaily(LocalDate.now())
        } catch (e: Exception) {
            // Log or ignore to prevent scan failing if aggregation has an error
            println("Failed to run real-time aggregation: ${e.message}")
        }

        return Pair(recordedEvent, savedSnapshot)
    }
}

