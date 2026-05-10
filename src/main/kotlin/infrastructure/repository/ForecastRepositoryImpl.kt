package infrastructure.repository

import domain.model.ForecastingResult
import domain.repository.ForecastRepository
import infrastructure.database.tables.ForecastingResultTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDate
import java.util.UUID

class ForecastRepositoryImpl : ForecastRepository {
    override suspend fun getForecastsByProduct(productId: UUID, fromDate: LocalDate): List<ForecastingResult> = newSuspendedTransaction {
        ForecastingResultTable
            .selectAll()
            .where { 
                (ForecastingResultTable.productId eq productId) and 
                (ForecastingResultTable.targetDate greaterEq fromDate) 
            }
            .orderBy(ForecastingResultTable.targetDate to SortOrder.ASC)
            .map { rowToForecast(it) }
    }

    private fun rowToForecast(row: ResultRow) = ForecastingResult(
        id = row[ForecastingResultTable.id],
        productId = row[ForecastingResultTable.productId],
        targetDate = row[ForecastingResultTable.targetDate],
        predictedValue = row[ForecastingResultTable.predictedValue],
        lowerBound = row[ForecastingResultTable.lowerBound],
        upperBound = row[ForecastingResultTable.upperBound],
        createdAt = row[ForecastingResultTable.createdAt]
    )
}
