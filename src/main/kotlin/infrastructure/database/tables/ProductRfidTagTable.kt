package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ProductRfidTagTable : Table("product_rfid_tags") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val tagUid = varchar("tag_uid", 100).uniqueIndex()
    val tagLabel = varchar("tag_label", 100).nullable()
    val status = varchar("status", 50).default("ACTIVE")
    val registeredByAdminId = uuid("registered_by_admin_id").references(AdminTable.id).nullable()
    val registeredAt = datetime("registered_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
