package infrastructure.repository

import domain.model.DailyAggregate
import domain.repository.AggregateRepository
import infrastructure.database.tables.DailyAggregateTable
import infrastructure.database.tables.InventoryEventTable
import infrastructure.database.tables.ProductTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class AggregateRepositoryImpl : AggregateRepository {

    override suspend fun calculateAndUpsertDaily(date: LocalDate): Unit = newSuspendedTransaction {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)

        val productIdsWithEvents = InventoryEventTable
            .select(InventoryEventTable.productId)
            .where { InventoryEventTable.recordedAt.between(startOfDay, endOfDay) }
            .withDistinct()
            .map { it[InventoryEventTable.productId] }

        for (productId in productIdsWithEvents) {
            val incoming = InventoryEventTable
                .select(InventoryEventTable.quantity.sum())
                .where { 
                    (InventoryEventTable.productId eq productId) and 
                    (InventoryEventTable.recordedAt.between(startOfDay, endOfDay)) and 
                    (InventoryEventTable.eventType inList listOf("REGISTER", "IN")) 
                }
                .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
                .singleOrNull() ?: 0

            val outgoing = InventoryEventTable
                .select(InventoryEventTable.quantity.sum())
                .where { 
                    (InventoryEventTable.productId eq productId) and 
                    (InventoryEventTable.recordedAt.between(startOfDay, endOfDay)) and 
                    (InventoryEventTable.eventType eq "OUT") 
                }
                .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
                .singleOrNull() ?: 0

            val netFlow = incoming - Math.abs(outgoing)

            val existingId = DailyAggregateTable
                .select(DailyAggregateTable.id)
                .where { (DailyAggregateTable.productId eq productId) and (DailyAggregateTable.date eq date) }
                .limit(1)
                .map { it[DailyAggregateTable.id] }
                .singleOrNull()

            if (existingId != null) {
                DailyAggregateTable.update({ DailyAggregateTable.id eq existingId }) {
                    it[totalIn] = incoming
                    it[totalOut] = Math.abs(outgoing)
                    it[DailyAggregateTable.netFlow] = netFlow
                    it[updatedAt] = LocalDateTime.now()
                }
            } else {
                DailyAggregateTable.insert {
                    it[id] = UUID.randomUUID()
                    it[DailyAggregateTable.productId] = productId
                    it[DailyAggregateTable.date] = date
                    it[totalIn] = incoming
                    it[totalOut] = Math.abs(outgoing)
                    it[DailyAggregateTable.netFlow] = netFlow
                }
            }
        }
    }

    override suspend fun getByProductAndDateRange(productId: UUID, startDate: LocalDate, endDate: LocalDate): List<DailyAggregate> = newSuspendedTransaction {
        DailyAggregateTable
            .selectAll()
            .where { 
                (DailyAggregateTable.productId eq productId) and 
                (DailyAggregateTable.date.between(startDate, endDate)) 
            }
            .orderBy(DailyAggregateTable.date to SortOrder.ASC)
            .map { rowToAggregate(it) }
    }

    override suspend fun syncAllProductsForDate(date: LocalDate): Int = newSuspendedTransaction {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)

        // 1. Ambil SEMUA produk aktif (bukan hanya yang punya event)
        val activeProducts = ProductTable
            .selectAll()
            .where { ProductTable.isActive eq true }
            .map { it[ProductTable.id] }

        var syncedCount = 0

        for (productId in activeProducts) {
            // 2. Hitung Total IN (REGISTER + IN)
            val incoming = InventoryEventTable
                .select(InventoryEventTable.quantity.sum())
                .where {
                    (InventoryEventTable.productId eq productId) and
                    (InventoryEventTable.recordedAt.between(startOfDay, endOfDay)) and
                    (InventoryEventTable.eventType inList listOf("REGISTER", "IN"))
                }
                .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
                .singleOrNull() ?: 0

            // 3. Hitung Total OUT
            val outgoing = InventoryEventTable
                .select(InventoryEventTable.quantity.sum())
                .where {
                    (InventoryEventTable.productId eq productId) and
                    (InventoryEventTable.recordedAt.between(startOfDay, endOfDay)) and
                    (InventoryEventTable.eventType eq "OUT")
                }
                .map { it[InventoryEventTable.quantity.sum()] ?: 0 }
                .singleOrNull() ?: 0

            val absOutgoing = Math.abs(outgoing)
            val netFlow = incoming - absOutgoing

            // 4. UPSERT: Update jika sudah ada, Insert jika belum
            val existingId = DailyAggregateTable
                .select(DailyAggregateTable.id)
                .where { (DailyAggregateTable.productId eq productId) and (DailyAggregateTable.date eq date) }
                .limit(1)
                .map { it[DailyAggregateTable.id] }
                .singleOrNull()

            if (existingId != null) {
                DailyAggregateTable.update({ DailyAggregateTable.id eq existingId }) {
                    it[totalIn] = incoming
                    it[totalOut] = absOutgoing
                    it[DailyAggregateTable.netFlow] = netFlow
                    it[updatedAt] = LocalDateTime.now()
                }
            } else {
                DailyAggregateTable.insert {
                    it[id] = UUID.randomUUID()
                    it[DailyAggregateTable.productId] = productId
                    it[DailyAggregateTable.date] = date
                    it[totalIn] = incoming
                    it[totalOut] = absOutgoing
                    it[DailyAggregateTable.netFlow] = netFlow
                    it[createdAt] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }
            }

            syncedCount++
        }

        syncedCount
    }

    override suspend fun countByProduct(productId: UUID): Long = newSuspendedTransaction {
        DailyAggregateTable
            .selectAll()
            .where { DailyAggregateTable.productId eq productId }
            .count()
    }

    private fun rowToAggregate(row: ResultRow) = DailyAggregate(
        id = row[DailyAggregateTable.id],
        productId = row[DailyAggregateTable.productId],
        date = row[DailyAggregateTable.date],
        totalIn = row[DailyAggregateTable.totalIn],
        totalOut = row[DailyAggregateTable.totalOut],
        netFlow = row[DailyAggregateTable.netFlow],
        createdAt = row[DailyAggregateTable.createdAt],
        updatedAt = row[DailyAggregateTable.updatedAt]
    )
}
