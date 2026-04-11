package data.repository

import domain.model.StockSnapshot
import domain.repository.StockRepository
import infrastructure.database.tables.ProductTable
import infrastructure.database.tables.StockSnapshotTable
import infrastructure.util.Quadruple
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.util.*

class StockRepositoryImpl(private val database: Database) : StockRepository {

    override suspend fun updateSnapshot(snapshot: StockSnapshot): Boolean = dbQuery {
        val existing = StockSnapshotTable.selectAll()
            .where { StockSnapshotTable.productId eq UUID.fromString(snapshot.productId) }
            .singleOrNull()

        val snapshotDateTime = parseAnyDateTime(snapshot.snapshotTime)

        if (existing == null) {
            StockSnapshotTable.insert {
                it[id] = UUID.randomUUID()
                it[productId] = UUID.fromString(snapshot.productId)
                it[deviceId] = UUID.fromString(snapshot.deviceId)
                it[currentWeight] = snapshot.currentWeight.toBigDecimal()
                it[currentStock] = snapshot.currentStock
                it[status] = snapshot.status
                it[snapshotTime] = snapshotDateTime
            }
            true
        } else {
            StockSnapshotTable.update({ StockSnapshotTable.productId eq UUID.fromString(snapshot.productId) }) {
                it[deviceId] = UUID.fromString(snapshot.deviceId)
                it[currentWeight] = snapshot.currentWeight.toBigDecimal()
                it[currentStock] = snapshot.currentStock
                it[status] = snapshot.status
                it[snapshotTime] = snapshotDateTime
            } > 0
        }
    }

    override suspend fun getCurrentStocks(): List<StockSnapshot> = dbQuery {
        StockSnapshotTable.selectAll().map(::toStockSnapshot)
    }

    override suspend fun getCurrentStocksWithProductInfo(): List<Quadruple<String, String, Int, StockSnapshot>> = dbQuery {
        // SQL JOIN: stock_snapshots JOIN products
        (StockSnapshotTable innerJoin ProductTable)
            .selectAll()
            .map { 
                Quadruple(
                    it[ProductTable.name],
                    it[ProductTable.code],
                    it[ProductTable.minStockThreshold],
                    toStockSnapshot(it)
                )
            }
    }

    override suspend fun getByProduct(productId: String): StockSnapshot? = dbQuery {
        StockSnapshotTable.selectAll()
            .where { StockSnapshotTable.productId eq UUID.fromString(productId) }
            .map(::toStockSnapshot)
            .singleOrNull()
    }

    private fun parseAnyDateTime(dateString: String): LocalDateTime {
        return try {
            val instant = Instant.parse(dateString)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(dateString)
            } catch (e2: Exception) {
                LocalDateTime.now()
            }
        }
    }

    private fun toStockSnapshot(row: ResultRow) = StockSnapshot(
        id = row[StockSnapshotTable.id].toString(),
        productId = row[StockSnapshotTable.productId].toString(),
        deviceId = row[StockSnapshotTable.deviceId].toString(),
        currentWeight = row[StockSnapshotTable.currentWeight].toDouble(),
        currentStock = row[StockSnapshotTable.currentStock],
        status = row[StockSnapshotTable.status],
        snapshotTime = row[StockSnapshotTable.snapshotTime].toString()
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
