package data.repository

import domain.model.SensorReading
import domain.repository.SensorRepository
import infrastructure.database.tables.SensorReadingTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.util.*

class SensorReadingRepositoryImpl(private val database: Database) : SensorRepository {

    override suspend fun saveReading(reading: SensorReading): SensorReading = dbQuery {
        val insertStatement = SensorReadingTable.insert {
            it[id] = UUID.randomUUID()
            it[deviceId] = UUID.fromString(reading.deviceId)
            it[productId] = UUID.fromString(reading.productId)
            it[rawWeight] = reading.rawWeight.toBigDecimal()
            it[filteredWeight] = reading.filteredWeight.toBigDecimal()
            it[estimatedStock] = reading.estimatedStock
            it[validationStatus] = reading.validationStatus
            it[noiseLevel] = reading.noiseLevel.toBigDecimal()
            it[isAnomaly] = reading.isAnomaly
            it[recordedAt] = parseFlexibleDateTime(reading.recordedAt)
        }

        insertStatement.resultedValues?.singleOrNull()?.let(::toSensorReading)
            ?: throw Exception("Failed to save sensor reading")
    }

    override suspend fun getHistory(productId: String, limit: Int): List<SensorReading> = dbQuery {
        SensorReadingTable.selectAll()
            .where { SensorReadingTable.productId eq UUID.fromString(productId) }
            .orderBy(SensorReadingTable.recordedAt, SortOrder.DESC)
            .limit(limit)
            .map(::toSensorReading)
    }

    override suspend fun getLatestReading(deviceId: String): SensorReading? = dbQuery {
        SensorReadingTable.selectAll()
            .where { SensorReadingTable.deviceId eq UUID.fromString(deviceId) }
            .orderBy(SensorReadingTable.createdAt, SortOrder.DESC)
            .limit(1)
            .map(::toSensorReading)
            .singleOrNull()
    }

    private fun parseFlexibleDateTime(dateString: String): LocalDateTime {
        return try {
            // Handle ISO 8601 with 'Z' (e.g. 2026-03-27T11:00:00Z)
            val instant = Instant.parse(dateString)
            LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        } catch (e: Exception) {
            try {
                // Fallback to local date time without 'Z'
                LocalDateTime.parse(dateString)
            } catch (e2: Exception) {
                // Last resort fallback
                LocalDateTime.now()
            }
        }
    }

    private fun toSensorReading(row: ResultRow) = SensorReading(
        id = row[SensorReadingTable.id].toString(),
        deviceId = row[SensorReadingTable.deviceId].toString(),
        productId = row[SensorReadingTable.productId].toString(),
        rawWeight = row[SensorReadingTable.rawWeight].toDouble(),
        filteredWeight = row[SensorReadingTable.filteredWeight].toDouble(),
        estimatedStock = row[SensorReadingTable.estimatedStock],
        validationStatus = row[SensorReadingTable.validationStatus],
        noiseLevel = row[SensorReadingTable.noiseLevel].toDouble(),
        isAnomaly = row[SensorReadingTable.isAnomaly],
        recordedAt = row[SensorReadingTable.recordedAt].toString()
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
