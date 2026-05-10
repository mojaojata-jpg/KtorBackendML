package infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ForecastingResultTable : Table("forecasting_results") {
    val id = uuid("id")
    val productId = uuid("product_id").references(ProductTable.id)
    val targetDate = date("target_date")
    val predictedValue = double("predicted_value")
    val lowerBound = double("lower_bound")
    val upperBound = double("upper_bound")
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_forecast_product_date", productId, targetDate)
    }
}
