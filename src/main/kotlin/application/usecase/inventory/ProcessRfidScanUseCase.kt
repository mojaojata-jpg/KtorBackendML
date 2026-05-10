package application.usecase.inventory

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import java.time.LocalDateTime
import java.util.UUID

class ProcessRfidScanUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
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

        // 2. State Validation (PENTING!)
        // Jika Continuous Mode (Scan Out), kita abaikan status INACTIVE supaya bisa scan berkali-kali buat testing/simulasi lebat
        if (!isContinuousMode) {
            if (eventType == "OUT" && tag.status != "ACTIVE") {
                throw IllegalArgumentException("Barang (Tag: $tagUid) sudah tidak ada di stok (Status: ${tag.status})")
            }
            if (eventType == "IN" && tag.status == "ACTIVE") {
                throw IllegalArgumentException("Barang (Tag: $tagUid) sudah ada di dalam stok (Status: ACTIVE)")
            }
        }

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

        // 5. Update Tag Status (Supaya tidak bisa double scan)
        val newTagStatus = if (eventType == "OUT") "INACTIVE" else "ACTIVE"
        inventoryRepository.updateTagStatus(tag.id!!, newTagStatus)

        // 6. Calculate New Stock (Snapshot)
        val latestSnapshot = inventoryRepository.getLatestSnapshot(tag.productId)
        val previousStock = latestSnapshot?.currentStock ?: 0
        val newStock = if (eventType == "IN") previousStock + 1 else previousStock - 1
        
        // Ensure stock doesn't go below zero
        val finalStock = if (newStock < 0) 0 else newStock

        // 5. Determine Status
        val status = when {
            finalStock == 0 -> "OUT_OF_STOCK"
            finalStock < product.minStockThreshold -> "LOW_STOCK"
            else -> "SUFFICIENT"
        }

        val snapshot = InventorySnapshot(
            productId = tag.productId,
            currentStock = finalStock,
            status = status,
            sourceEventId = recordedEvent.id
        )
        val savedSnapshot = inventoryRepository.saveSnapshot(snapshot)

        return Pair(recordedEvent, savedSnapshot)
    }
}
