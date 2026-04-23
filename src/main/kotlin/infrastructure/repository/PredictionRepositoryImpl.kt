package infrastructure.repository

import domain.model.PredictionResult
import domain.repository.PredictionRepository
import infrastructure.database.tables.PredictionTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class PredictionRepositoryImpl : PredictionRepository {
    override suspend fun getLatestPredictionByProductId(productId: String): PredictionResult? = newSuspendedTransaction {
        try {
            PredictionTable.selectAll().where { PredictionTable.productId eq UUID.fromString(productId) }
                .orderBy(PredictionTable.createdAt to SortOrder.DESC)
                .limit(1)
                .map { rowToPrediction(it) }
                .singleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getLatestPredictions(): List<PredictionResult> = newSuspendedTransaction {
        PredictionTable.selectAll()
            .orderBy(PredictionTable.createdAt to SortOrder.DESC)
            .map { rowToPrediction(it) }
    }

    suspend fun create(prediction: PredictionResult): PredictionResult = newSuspendedTransaction {
        val id = PredictionTable.insert {
            it[PredictionTable.id] = UUID.randomUUID()
            it[productId] = prediction.productId
            it[modelName] = prediction.modelName
            it[modelVersion] = prediction.modelVersion
            it[currentStock] = prediction.currentStock
            it[predictedDaysRemaining] = prediction.predictedDaysRemaining
            it[predictedStockOutDate] = prediction.predictedStockOutDate
            it[confidenceScore] = prediction.confidenceScore?.toBigDecimal()
        } get PredictionTable.id
        
        prediction.copy(id = id)
    }

    private fun rowToPrediction(row: ResultRow) = PredictionResult(
        id = row[PredictionTable.id],
        productId = row[PredictionTable.productId],
        modelName = row[PredictionTable.modelName],
        modelVersion = row[PredictionTable.modelVersion],
        currentStock = row[PredictionTable.currentStock],
        predictedDaysRemaining = row[PredictionTable.predictedDaysRemaining],
        predictedStockOutDate = row[PredictionTable.predictedStockOutDate],
        confidenceScore = row[PredictionTable.confidenceScore]?.toDouble(),
        createdAt = row[PredictionTable.createdAt]
    )
}
