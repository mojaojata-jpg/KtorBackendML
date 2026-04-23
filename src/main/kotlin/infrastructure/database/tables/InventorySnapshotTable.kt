package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object InventorySnapshotTable : Table("inventory_snapshots") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val currentStock = integer("current_stock")
    val status = varchar("status", 50) // OUT_OF_STOCK, LOW_STOCK, SUFFICIENT
    val snapshotTime = datetime("snapshot_time").default(LocalDateTime.now())
    val sourceEventId = uuid("source_event_id").references(InventoryEventTable.id).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
