package application.usecase.inventory

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.model.ProductRfidTag
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import java.time.LocalDateTime
import java.util.UUID

class RegisterRfidTagUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(
        productId: String,
        tagUid: String,
        tagLabel: String? = null,
        adminId: String? = null
    ): Pair<ProductRfidTag, InventorySnapshot> {
        // 1. Validate Product
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

        // 2. Check if tag already exists
        val existingTag = inventoryRepository.findTagByUid(tagUid)
        if (existingTag != null) {
            throw IllegalArgumentException("RFID Tag already registered: $tagUid (Product: ${existingTag.productId})")
        }

        // 3. Register Tag
        val newTag = ProductRfidTag(
            productId = UUID.fromString(productId),
            tagUid = tagUid,
            tagLabel = tagLabel,
            registeredByAdminId = adminId?.let { UUID.fromString(it) },
            status = "ACTIVE"
        )
        val savedTag = inventoryRepository.saveTag(newTag)

        // 4. Record REGISTER Event
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

        // 5. Update Stock Snapshot (Registering a tag usually means adding 1 item)
        val latestSnapshot = inventoryRepository.getLatestSnapshot(savedTag.productId)
        val newStock = (latestSnapshot?.currentStock ?: 0) + 1
        
        val status = when {
            newStock == 0 -> "OUT_OF_STOCK"
            newStock < product.minStockThreshold -> "LOW_STOCK"
            else -> "SUFFICIENT"
        }

        val snapshot = InventorySnapshot(
            productId = savedTag.productId,
            currentStock = newStock,
            status = status,
            sourceEventId = recordedEvent.id
        )
        val savedSnapshot = inventoryRepository.saveSnapshot(snapshot)

        return Pair(savedTag, savedSnapshot)
    }
}
