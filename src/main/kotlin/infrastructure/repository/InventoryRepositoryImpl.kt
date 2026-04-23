package infrastructure.repository

import domain.model.InventoryEvent
import domain.model.InventorySnapshot
import domain.model.ProductRfidTag
import domain.repository.InventoryRepository
import infrastructure.database.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class InventoryRepositoryImpl : InventoryRepository {

    override suspend fun findTagByUid(tagUid: String): ProductRfidTag? = newSuspendedTransaction {
        ProductRfidTagTable.selectAll().where { ProductRfidTagTable.tagUid eq tagUid }
            .map { rowToTag(it) }
            .singleOrNull()
    }

    override suspend fun saveTag(tag: ProductRfidTag): ProductRfidTag = newSuspendedTransaction {
        val id = ProductRfidTagTable.insert {
            it[ProductRfidTagTable.id] = UUID.randomUUID()
            it[productId] = tag.productId
            it[tagUid] = tag.tagUid
            it[tagLabel] = tag.tagLabel
            it[status] = tag.status
            it[registeredByAdminId] = tag.registeredByAdminId
        } get ProductRfidTagTable.id
        
        tag.copy(id = id)
    }

    override suspend fun updateTagStatus(tagId: UUID, status: String): Unit = newSuspendedTransaction {
        ProductRfidTagTable.update({ ProductRfidTagTable.id eq tagId }) {
            it[ProductRfidTagTable.status] = status
        }
    }.run { Unit }

    override suspend fun recordEvent(event: InventoryEvent): InventoryEvent = newSuspendedTransaction {
        val id = InventoryEventTable.insert {
            it[InventoryEventTable.id] = UUID.randomUUID()
            it[productId] = event.productId
            it[tagId] = event.tagId
            it[adminId] = event.adminId
            it[eventType] = event.eventType
            it[quantity] = event.quantity
            it[note] = event.note
            it[recordedAt] = event.recordedAt
        } get InventoryEventTable.id
        
        event.copy(id = id)
    }

    override suspend fun saveSnapshot(snapshot: InventorySnapshot): InventorySnapshot = newSuspendedTransaction {
        val id = InventorySnapshotTable.insert {
            it[InventorySnapshotTable.id] = UUID.randomUUID()
            it[productId] = snapshot.productId
            it[currentStock] = snapshot.currentStock
            it[status] = snapshot.status
            it[sourceEventId] = snapshot.sourceEventId
        } get InventorySnapshotTable.id
        
        snapshot.copy(id = id)
    }

    override suspend fun getLatestSnapshot(productId: UUID): InventorySnapshot? = newSuspendedTransaction {
        InventorySnapshotTable.selectAll().where { InventorySnapshotTable.productId eq productId }
            .orderBy(InventorySnapshotTable.snapshotTime to SortOrder.DESC)
            .limit(1)
            .map { rowToSnapshot(it) }
            .singleOrNull()
    }

    override suspend fun getProductHistory(productId: UUID, limit: Int): List<InventoryEvent> = newSuspendedTransaction {
        InventoryEventTable
            .selectAll()
            .where { InventoryEventTable.productId eq productId }
            .orderBy(InventoryEventTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                InventoryEvent(
                    id = row[InventoryEventTable.id],
                    productId = row[InventoryEventTable.productId],
                    tagId = row.getOrNull(InventoryEventTable.tagId),
                    adminId = row.getOrNull(InventoryEventTable.adminId),
                    eventType = row[InventoryEventTable.eventType],
                    quantity = row[InventoryEventTable.quantity],
                    note = row[InventoryEventTable.note],
                    recordedAt = row[InventoryEventTable.recordedAt],
                    createdAt = row[InventoryEventTable.createdAt]
                )
            }
    }

    override suspend fun getDashboardSnapshots(): List<Pair<domain.model.Product, InventorySnapshot?>> = newSuspendedTransaction {
        val products = ProductTable.selectAll().where { ProductTable.isActive eq true }.map { rowToProduct(it) }
        
        products.map { product ->
            val snapshot = getLatestSnapshotInTransaction(product.id!!)
            product to snapshot
        }
    }

    override suspend fun getProductStats(productId: UUID): Pair<Int, Int> = newSuspendedTransaction {
        val incoming = InventoryEventTable
            .select(InventoryEventTable.quantity.sum())
            .where { (InventoryEventTable.productId eq productId) and (InventoryEventTable.eventType inList listOf("REGISTER", "IN")) }
            .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
            .singleOrNull() ?: 0

        val outgoing = InventoryEventTable
            .select(InventoryEventTable.quantity.sum())
            .where { (InventoryEventTable.productId eq productId) and (InventoryEventTable.eventType eq "OUT") }
            .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
            .singleOrNull() ?: 0

        Pair(incoming, Math.abs(outgoing))
    }

    // Helper to call inside transaction
    private fun getLatestSnapshotInTransaction(productId: UUID): InventorySnapshot? {
        return InventorySnapshotTable.selectAll().where { InventorySnapshotTable.productId eq productId }
            .orderBy(InventorySnapshotTable.snapshotTime to SortOrder.DESC)
            .limit(1)
            .map { rowToSnapshot(it) }
            .singleOrNull()
    }

    private fun rowToTag(row: ResultRow) = ProductRfidTag(
        id = row[ProductRfidTagTable.id],
        productId = row[ProductRfidTagTable.productId],
        tagUid = row[ProductRfidTagTable.tagUid],
        tagLabel = row[ProductRfidTagTable.tagLabel],
        status = row[ProductRfidTagTable.status],
        registeredByAdminId = row[ProductRfidTagTable.registeredByAdminId],
        registeredAt = row[ProductRfidTagTable.registeredAt],
        updatedAt = row[ProductRfidTagTable.updatedAt]
    )

    private fun rowToSnapshot(row: ResultRow) = InventorySnapshot(
        id = row[InventorySnapshotTable.id],
        productId = row[InventorySnapshotTable.productId],
        currentStock = row[InventorySnapshotTable.currentStock],
        status = row[InventorySnapshotTable.status],
        snapshotTime = row[InventorySnapshotTable.snapshotTime],
        sourceEventId = row[InventorySnapshotTable.sourceEventId],
        createdAt = row[InventorySnapshotTable.createdAt]
    )

    private fun rowToEvent(row: ResultRow) = InventoryEvent(
        id = row[InventoryEventTable.id],
        productId = row[InventoryEventTable.productId],
        tagId = row.getOrNull(InventoryEventTable.tagId),
        adminId = row.getOrNull(InventoryEventTable.adminId),
        eventType = row[InventoryEventTable.eventType],
        quantity = row[InventoryEventTable.quantity],
        note = row[InventoryEventTable.note],
        recordedAt = row[InventoryEventTable.recordedAt],
        createdAt = row[InventoryEventTable.createdAt]
    )
    
    private fun rowToProduct(row: ResultRow) = domain.model.Product(
        id = row[ProductTable.id],
        name = row[ProductTable.name],
        code = row[ProductTable.code],
        unitLabel = row[ProductTable.unitLabel],
        minStockThreshold = row[ProductTable.minStockThreshold],
        description = row[ProductTable.description],
        imageUrl = row[ProductTable.imageUrl],
        isActive = row[ProductTable.isActive],
        createdAt = row[ProductTable.createdAt],
        updatedAt = row[ProductTable.updatedAt]
    )
}
