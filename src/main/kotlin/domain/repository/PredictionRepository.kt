package domain.repository

import domain.model.PredictionResult

interface PredictionRepository {
    suspend fun getLatestPredictions(): List<PredictionResult>
    suspend fun getLatestPredictionByProductId(productId: String): PredictionResult?
}
