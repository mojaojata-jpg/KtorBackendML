package application.usecase.inventory

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import domain.repository.AggregateRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

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

        // Parallel Execution: Fetch product, snapshot, and record event concurrently!
        return kotlinx.coroutines.coroutineScope {
            val productDeferred = async { productRepository.findById(tag.productId.toString()) }
            val latestSnapshotDeferred = async { inventoryRepository.getLatestSnapshot(tag.productId) }
            val recordedEventDeferred = async {
                val event = InventoryEvent(
                    productId = tag.productId,
                    tagId = tag.id,
                    adminId = adminId,
                    eventType = eventType,
                    quantity = 1,
                    note = note,
                    recordedAt = LocalDateTime.now()
                )
                inventoryRepository.recordEvent(event)
            }

            val product = productDeferred.await()
                ?: throw IllegalArgumentException("Product not found for tag: $tagUid")
            
            val latestSnapshot = latestSnapshotDeferred.await()
            val recordedEvent = recordedEventDeferred.await()

            val previousStock = latestSnapshot?.currentStock ?: 0
            val newStock = if (eventType == "IN") previousStock + 1 else previousStock - 1
            
            // Ensure stock doesn't go below zero
            val finalStock = if (newStock < 0) 0 else newStock

            // Determine Status
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

            // Real-time Aggregation: Sync aggregate stats in background (async) to cut down response time
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    aggregateRepository.calculateAndUpsertDaily(LocalDate.now())
                } catch (e: Exception) {
                    println("Failed to run real-time aggregation in background: ${e.message}")
                }
            }

            Pair(recordedEvent, savedSnapshot)
        }
    }
}

