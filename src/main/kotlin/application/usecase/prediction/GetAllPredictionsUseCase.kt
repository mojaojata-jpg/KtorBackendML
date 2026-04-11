package application.usecase.prediction

import domain.model.PredictionResult
import domain.repository.PredictionRepository

class GetAllPredictionsUseCase(private val predictionRepository: PredictionRepository) {
    suspend operator fun invoke(): List<PredictionResult> {
        return predictionRepository.getLatestPredictions()
    }
}
