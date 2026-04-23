package domain.repository

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.model.ProductRfidTag
import java.util.UUID

interface InventoryRepository {
    // Tag Management
    suspend fun findTagByUid(tagUid: String): ProductRfidTag?
    suspend fun saveTag(tag: ProductRfidTag): ProductRfidTag
    suspend fun updateTagStatus(tagId: UUID, status: String)

    // Event & Stock
    suspend fun recordEvent(event: InventoryEvent): InventoryEvent
    suspend fun saveSnapshot(snapshot: InventorySnapshot): InventorySnapshot
    suspend fun getLatestSnapshot(productId: UUID): InventorySnapshot?
    suspend fun getProductHistory(productId: UUID, limit: Int): List<InventoryEvent>
    suspend fun getDashboardSnapshots(): List<Pair<domain.model.Product, InventorySnapshot?>>
    suspend fun getProductStats(productId: UUID): Pair<Int, Int> // Returns Pair(Total Incoming, Total Outgoing)
}
