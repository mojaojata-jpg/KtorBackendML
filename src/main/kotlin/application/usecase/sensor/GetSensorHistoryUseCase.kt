package application.usecase.sensor

import domain.model.SensorReading
import domain.repository.SensorRepository

class GetSensorHistoryUseCase(private val sensorRepository: SensorRepository) {
    suspend operator fun invoke(productId: String, limit: Int = 20): List<SensorReading> {
        return sensorRepository.getHistory(productId, limit)
    }
}
