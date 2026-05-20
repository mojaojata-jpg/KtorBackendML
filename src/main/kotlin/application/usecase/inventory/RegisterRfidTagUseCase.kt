package application.usecase.inventory

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.model.ProductRfidTag
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import domain.repository.AggregateRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

class RegisterRfidTagUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val aggregateRepository: AggregateRepository
) {
    suspend operator fun invoke(
        productId: String,
        tagUid: String,
        tagLabel: String? = null,
        adminId: String? = null
    ): Pair<ProductRfidTag, InventorySnapshot> {
        // Parallel Execution: Fetch product, tag, and snapshot concurrently to reduce physical latency
        return kotlinx.coroutines.coroutineScope {
            val uuidProductId = UUID.fromString(productId)
            
            val productDeferred = async { productRepository.findById(productId) }
            val existingTagDeferred = async { inventoryRepository.findTagByUid(tagUid) }
            val latestSnapshotDeferred = async { inventoryRepository.getLatestSnapshot(uuidProductId) }

            val product = productDeferred.await()
                ?: throw IllegalArgumentException("Product not found: $productId")
            val existingTag = existingTagDeferred.await()
            val latestSnapshot = latestSnapshotDeferred.await()

            val savedTag = if (existingTag != null) {
                if (existingTag.status == "INACTIVE") {
                    inventoryRepository.updateTagStatus(existingTag.id!!, "ACTIVE")
                    val updatedTag = existingTag.copy(
                        productId = uuidProductId,
                        tagLabel = tagLabel ?: existingTag.tagLabel,
                        status = "ACTIVE"
                    )
                    inventoryRepository.saveTag(updatedTag) 
                } else {
                    throw IllegalArgumentException("RFID Tag is still ACTIVE in system: $tagUid (Product: ${existingTag.productId})")
                }
            } else {
                val newTag = ProductRfidTag(
                    productId = uuidProductId,
                    tagUid = tagUid,
                    tagLabel = tagLabel,
                    registeredByAdminId = adminId?.let { UUID.fromString(it) },
                    status = "ACTIVE"
                )
                inventoryRepository.saveTag(newTag)
            }

            val event = InventoryEvent(
                productId = savedTag.productId,
                tagId = savedTag.id,
                adminId = adminId?.let { UUID.fromString(it) },
                eventType = "REGISTER",
                quantity = 1,
                note = "Initial registration of tag $tagUid",
                recordedAt = LocalDateTime.now()
            )
            val recordedEvent = inventoryRepository.recordEvent(event)

            val newStock = (latestSnapshot?.currentStock ?: 0) + 1
            
            val status = when {
                newStock == 0 -> "OUT_OF_STOCK"
                newStock <= product.minStockThreshold -> "LOW_STOCK"
                else -> "SUFFICIENT"
            }

            val snapshot = InventorySnapshot(
                productId = savedTag.productId,
                currentStock = newStock,
                status = status,
                sourceEventId = recordedEvent.id
            )
            val savedSnapshot = inventoryRepository.saveSnapshot(snapshot)

            // Real-time Aggregation: Sync aggregate stats in background (async)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    aggregateRepository.calculateAndUpsertDaily(LocalDate.now())
                } catch (e: Exception) {
                    println("Failed to run real-time aggregation on register in background: ${e.message}")
                }
            }

            Pair(savedTag, savedSnapshot)
        }
    }
}

