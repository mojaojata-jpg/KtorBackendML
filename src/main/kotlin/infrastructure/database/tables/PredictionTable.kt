package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PredictionTable : Table("prediction_results") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val modelName = varchar("model_name", 100)
    val modelVersion = varchar("model_version", 50)
    val predictedDaysRemaining = integer("predicted_days_remaining")
    val predictedStockOutDate = date("predicted_stock_out_date")
    val confidenceScore = decimal("confidence_score", 5, 2).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
