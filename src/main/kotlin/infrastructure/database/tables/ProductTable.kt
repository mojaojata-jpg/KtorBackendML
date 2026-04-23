package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ProductTable : Table("products") {
    val id = uuid("id")
    val name = varchar("name", 100)
    val code = varchar("code", 50).uniqueIndex()
    val unitLabel = varchar("unit_label", 30).default("pcs")
    val minStockThreshold = integer("min_stock_threshold").default(0)
    val description = text("description").nullable()
    val imageUrl = text("image_url").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
