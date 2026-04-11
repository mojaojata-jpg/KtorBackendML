package application.usecase.prediction

import domain.model.PredictionResult
import domain.repository.PredictionRepository

class GetPredictionByProductIdUseCase(private val predictionRepository: PredictionRepository) {
    suspend operator fun invoke(productId: String): PredictionResult? {
        return predictionRepository.getLatestPredictionByProductId(productId)
    }
}
