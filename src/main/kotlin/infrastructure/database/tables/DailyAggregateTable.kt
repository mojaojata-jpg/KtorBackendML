package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DailyAggregateTable : Table("daily_aggregates") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val date = date("date")
    val totalIn = integer("total_in").default(0)
    val totalOut = integer("total_out").default(0)
    val netFlow = integer("net_flow").default(0)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_product_date", productId, date)
    }
}
