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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

val productScanMutexes = ConcurrentHashMap<UUID, Mutex>()

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
        isContinuousMode: Boolean = false
    ): Pair<InventoryEvent, InventorySnapshot> {
        // 1. Find the tag (safe outside mutex)
        val tag = inventoryRepository.findTagByUid(tagUid)
            ?: throw IllegalArgumentException("RFID Tag not registered: $tagUid")

        // 2. Get product info (safe outside mutex - product data doesn't change during scan)
        val product = productRepository.findById(tag.productId.toString())
            ?: throw IllegalArgumentException("Product not found for tag: $tagUid")

        // 3. Mutex lock: SEQUENTIAL read-modify-write to prevent stock skipping/staying
        val mutex = productScanMutexes.getOrPut(tag.productId) { Mutex() }
        
        return mutex.withLock {
            // Step A: Record event
            val event = InventoryEvent(
                productId = tag.productId,
                tagId = tag.id,
                adminId = adminId,
                eventType = eventType,
                quantity = 1,
                note = note,
                recordedAt = LocalDateTime.now()
            )
            val recordedEvent = inventoryRepository.recordEvent(event)

            // Step B: Read current stock AFTER event is recorded
            val latestSnapshot = inventoryRepository.getLatestSnapshot(tag.productId)
            val previousStock = latestSnapshot?.currentStock ?: 0
            val newStock = if (eventType == "IN") previousStock + 1 else previousStock - 1
            val finalStock = if (newStock < 0) 0 else newStock

            // Step C: Determine status
            val status = when {
                finalStock == 0 -> "OUT_OF_STOCK"
                finalStock <= product.minStockThreshold -> "LOW_STOCK"
                else -> "SUFFICIENT"
            }

            // Step D: Save new snapshot
            val snapshot = InventorySnapshot(
                productId = tag.productId,
                currentStock = finalStock,
                status = status,
                sourceEventId = recordedEvent.id
            )
            val savedSnapshot = inventoryRepository.saveSnapshot(snapshot)

            // Step E: Background aggregation (fire-and-forget, non-blocking)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    aggregateRepository.calculateAndUpsertDaily(LocalDate.now())
                } catch (e: Exception) {
                    println("Background aggregation failed: ${e.message}")
                }
            }

            Pair(recordedEvent, savedSnapshot)
        }
    }
}

