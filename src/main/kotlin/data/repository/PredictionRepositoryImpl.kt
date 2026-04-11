package data.repository

import domain.model.PredictionResult
import domain.repository.PredictionRepository
import infrastructure.database.tables.PredictionTable
import infrastructure.database.tables.ProductTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class PredictionRepositoryImpl(private val database: Database) : PredictionRepository {

    override suspend fun getLatestPredictions(): List<PredictionResult> = dbQuery {
        // Query to get the latest prediction for each product
        // In a simple way, we can get all and then filter or use a more complex SQL.
        // For now, let's get all results ordered by createdAt DESC and we can handle it in UseCase if needed,
        // or just return the list of all prediction records.
        // Usually, 'latest' means the most recent record for each product.
        
        PredictionTable.selectAll()
            .orderBy(PredictionTable.createdAt to SortOrder.DESC)
            .map(::toPredictionResult)
    }

    override suspend fun getLatestPredictionByProductId(productId: String): PredictionResult? = dbQuery {
        PredictionTable.selectAll()
            .where { PredictionTable.productId eq UUID.fromString(productId) }
            .orderBy(PredictionTable.createdAt to SortOrder.DESC)
            .limit(1)
            .map(::toPredictionResult)
            .singleOrNull()
    }

    private fun toPredictionResult(row: ResultRow) = PredictionResult(
        id = row[PredictionTable.id].toString(),
        productId = row[PredictionTable.productId].toString(),
        modelName = row[PredictionTable.modelName],
        modelVersion = row[PredictionTable.modelVersion],
        predictedDaysRemaining = row[PredictionTable.predictedDaysRemaining],
        predictedStockOutDate = row[PredictionTable.predictedStockOutDate].toString(),
        confidenceScore = row[PredictionTable.confidenceScore]?.toDouble(),
        createdAt = row[PredictionTable.createdAt].toString()
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
