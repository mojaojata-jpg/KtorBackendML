package data.repository

import domain.model.IOTDevice
import domain.repository.IOTDeviceRepository
import infrastructure.database.tables.IOTDeviceTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

class IOTDeviceRepositoryImpl(private val database: Database) : IOTDeviceRepository {

    override suspend fun create(device: IOTDevice): IOTDevice = dbQuery {
        val insertStatement = IOTDeviceTable.insert {
            it[id] = UUID.randomUUID()
            it[deviceCode] = device.deviceCode
            it[deviceName] = device.deviceName
            it[productId] = UUID.fromString(device.productId)
            it[status] = "ACTIVE"
            it[lastSeenAt] = LocalDateTime.now()
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }

        insertStatement.resultedValues?.singleOrNull()?.let(::toIOTDevice)
            ?: throw Exception("Failed to create IoT device")
    }

    override suspend fun findByCode(deviceCode: String): IOTDevice? = dbQuery {
        IOTDeviceTable.selectAll().where { IOTDeviceTable.deviceCode eq deviceCode }
            .map(::toIOTDevice)
            .singleOrNull()
    }

    override suspend fun findById(id: String): IOTDevice? = dbQuery {
        IOTDeviceTable.selectAll().where { IOTDeviceTable.id eq UUID.fromString(id) }
            .map(::toIOTDevice)
            .singleOrNull()
    }

    override suspend fun getAll(): List<IOTDevice> = dbQuery {
        IOTDeviceTable.selectAll().map(::toIOTDevice)
    }

    override suspend fun update(id: String, device: IOTDevice): Boolean = dbQuery {
        IOTDeviceTable.update({ IOTDeviceTable.id eq UUID.fromString(id) }) {
            it[deviceName] = device.deviceName
            it[productId] = UUID.fromString(device.productId)
            it[status] = device.status
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        IOTDeviceTable.deleteWhere { IOTDeviceTable.id eq UUID.fromString(id) } > 0
    }

    override suspend fun updateLastSeen(id: String): Boolean = dbQuery {
        // FIX: Update lastSeenAt DAN paksa status jadi ACTIVE
        IOTDeviceTable.update({ IOTDeviceTable.id eq UUID.fromString(id) }) {
            it[lastSeenAt] = LocalDateTime.now()
            it[status] = "ACTIVE"
        } > 0
    }

    private fun toIOTDevice(row: ResultRow) = IOTDevice(
        id = row[IOTDeviceTable.id].toString(),
        deviceCode = row[IOTDeviceTable.deviceCode],
        deviceName = row[IOTDeviceTable.deviceName],
        productId = row[IOTDeviceTable.productId].toString(),
        status = row[IOTDeviceTable.status],
        lastSeenAt = row[IOTDeviceTable.lastSeenAt]?.toString()
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
