package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object InventoryEventTable : Table("inventory_events") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val tagId = uuid("tag_id").references(ProductRfidTagTable.id).nullable()
    val adminId = uuid("admin_id").references(AdminTable.id).nullable()
    val eventType = varchar("event_type", 30) // REGISTER, IN, OUT, ADJUSTMENT
    val quantity = integer("quantity").default(1)
    val note = text("note").nullable()
    val recordedAt = datetime("recorded_at").default(LocalDateTime.now())
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
