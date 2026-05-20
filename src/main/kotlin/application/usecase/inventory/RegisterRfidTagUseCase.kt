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
        // 1. Validate Product
        val product = productRepository.findById(productId)
            ?: throw IllegalArgumentException("Product not found: $productId")

        // 2. Check if tag already exists
        val existingTag = inventoryRepository.findTagByUid(tagUid)
        
        val savedTag = if (existingTag != null) {
            // Jika tag sudah ada tapi statusnya INACTIVE, kita BOLEH daftarin ulang (Reusable)
            if (existingTag.status == "INACTIVE") {
                // Update tag lama jadi ACTIVE dan ganti produknya (jika beda)
                inventoryRepository.updateTagStatus(existingTag.id!!, "ACTIVE")
                
                // Jika produknya berubah, kita update juga product_id-nya
                val updatedTag = existingTag.copy(
                    productId = UUID.fromString(productId),
                    tagLabel = tagLabel ?: existingTag.tagLabel,
                    status = "ACTIVE"
                )
                // Kita asumsikan repository punya fungsi update, jika tidak kita pakai saveTag (UPSERT)
                inventoryRepository.saveTag(updatedTag) 
            } else {
                // Kalau masih ACTIVE, baru kita kasih error (biar gak double input)
                throw IllegalArgumentException("RFID Tag is still ACTIVE in system: $tagUid (Product: ${existingTag.productId})")
            }
        } else {
            // 3. Register Tag Baru
            val newTag = ProductRfidTag(
                productId = UUID.fromString(productId),
                tagUid = tagUid,
                tagLabel = tagLabel,
                registeredByAdminId = adminId?.let { UUID.fromString(it) },
                status = "ACTIVE"
            )
            inventoryRepository.saveTag(newTag)
        }

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

        // Real-time Aggregation: Sync aggregate stats in background (async) to cut down response time
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                aggregateRepository.calculateAndUpsertDaily(LocalDate.now())
            } catch (e: Exception) {
                println("Failed to run real-time aggregation on register in background: ${e.message}")
            }
        }

        return Pair(savedTag, savedSnapshot)
    }
}

