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
        val existing = ProductRfidTagTable
            .selectAll()
            .where { ProductRfidTagTable.id eq (tag.id ?: UUID.randomUUID()) }
            .singleOrNull()

        if (existing != null) {
            ProductRfidTagTable.update({ ProductRfidTagTable.id eq existing[ProductRfidTagTable.id] }) {
                it[productId] = tag.productId
                it[tagUid] = tag.tagUid
                it[tagLabel] = tag.tagLabel
                it[status] = tag.status
                it[registeredByAdminId] = tag.registeredByAdminId
                it[updatedAt] = java.time.LocalDateTime.now()
            }
            tag
        } else {
            val id = ProductRfidTagTable.insert {
                it[ProductRfidTagTable.id] = tag.id ?: UUID.randomUUID()
                it[productId] = tag.productId
                it[tagUid] = tag.tagUid
                it[tagLabel] = tag.tagLabel
                it[status] = tag.status
                it[registeredByAdminId] = tag.registeredByAdminId
            } get ProductRfidTagTable.id
            tag.copy(id = id)
        }
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
        // UPSERT LOGIC: Jika sudah ada untuk product_id ini, UPDATE. Jika tidak ada, INSERT.
        val existingId = InventorySnapshotTable
            .select(InventorySnapshotTable.id)
            .where { InventorySnapshotTable.productId eq snapshot.productId }
            .limit(1)
            .map { it[InventorySnapshotTable.id] }
            .singleOrNull()

        if (existingId != null) {
            InventorySnapshotTable.update({ InventorySnapshotTable.id eq existingId }) {
                it[currentStock] = snapshot.currentStock
                it[status] = snapshot.status
                it[snapshotTime] = java.time.LocalDateTime.now()
                it[sourceEventId] = snapshot.sourceEventId
            }
            snapshot.copy(id = existingId)
        } else {
            val id = InventorySnapshotTable.insert {
                it[InventorySnapshotTable.id] = UUID.randomUUID()
                it[productId] = snapshot.productId
                it[currentStock] = snapshot.currentStock
                it[status] = snapshot.status
                it[snapshotTime] = java.time.LocalDateTime.now()
                it[sourceEventId] = snapshot.sourceEventId
            } get InventorySnapshotTable.id
            snapshot.copy(id = id)
        }
    }

    override suspend fun getLatestSnapshot(productId: UUID): InventorySnapshot? = newSuspendedTransaction {
        InventorySnapshotTable.selectAll().where { InventorySnapshotTable.productId eq productId }
            .orderBy(InventorySnapshotTable.snapshotTime to SortOrder.DESC)
            .limit(1)
            .map { rowToSnapshot(it) }
            .singleOrNull()
    }

    override suspend fun getProductHistory(productId: UUID, limit: Int, startDate: java.time.LocalDate?): List<InventoryEvent> = newSuspendedTransaction {
        val startDateTime = startDate?.atStartOfDay()
        
        InventoryEventTable
            .selectAll()
            .where { 
                val base = InventoryEventTable.productId eq productId
                if (startDateTime != null) base and (InventoryEventTable.recordedAt greaterEq startDateTime)
                else base
            }
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

    override suspend fun getProductStats(productId: UUID, startDate: java.time.LocalDate?, endDate: java.time.LocalDate?): Pair<Int, Int> = newSuspendedTransaction {
        val startDateTime = startDate?.atStartOfDay()
        val endDateTime = endDate?.atTime(23, 59, 59)

        val incoming = InventoryEventTable.select(InventoryEventTable.quantity.sum())
            .where { (InventoryEventTable.productId eq productId) and (InventoryEventTable.eventType inList listOf("REGISTER", "IN")) }
            .apply {
                if (startDateTime != null) andWhere { InventoryEventTable.recordedAt greaterEq startDateTime }
                if (endDateTime != null) andWhere { InventoryEventTable.recordedAt lessEq endDateTime }
            }
            .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
            .singleOrNull() ?: 0

        val outgoing = InventoryEventTable.select(InventoryEventTable.quantity.sum())
            .where { (InventoryEventTable.productId eq productId) and (InventoryEventTable.eventType eq "OUT") }
            .apply {
                if (startDateTime != null) andWhere { InventoryEventTable.recordedAt greaterEq startDateTime }
                if (endDateTime != null) andWhere { InventoryEventTable.recordedAt lessEq endDateTime }
            }
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

    override suspend fun getAllEventsForDate(date: java.time.LocalDate, limit: Int): List<Pair<InventoryEvent, domain.model.Product>> = newSuspendedTransaction {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(23, 59, 59)

        (InventoryEventTable innerJoin ProductTable)
            .selectAll()
            .where {
                (InventoryEventTable.recordedAt.between(startOfDay, endOfDay)) and
                (InventoryEventTable.productId eq ProductTable.id)
            }
            .orderBy(InventoryEventTable.recordedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                Pair(rowToEvent(row), rowToProduct(row))
            }
    }
}
