package infrastructure.di

import data.repository.AdminRepositoryImpl
import data.repository.ProductRepositoryImpl
import data.repository.IOTDeviceRepositoryImpl
import data.repository.SensorReadingRepositoryImpl
import data.repository.StockRepositoryImpl
import domain.repository.AdminRepository
import domain.repository.ProductRepository
import domain.repository.IOTDeviceRepository
import domain.repository.SensorRepository
import domain.repository.StockRepository
import domain.repository.PredictionRepository
import data.repository.PredictionRepositoryImpl
import org.jetbrains.exposed.sql.Database

class RepositoryModule(private val database: Database) {
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl(database) }
    val productRepository: ProductRepository by lazy { ProductRepositoryImpl(database) }
    val iotDeviceRepository: IOTDeviceRepository by lazy { IOTDeviceRepositoryImpl(database) }
    val sensorRepository: SensorRepository by lazy { SensorReadingRepositoryImpl(database) } // Use ReadingImpl
    val stockRepository: StockRepository by lazy { StockRepositoryImpl(database) }
    val predictionRepository: PredictionRepository by lazy { PredictionRepositoryImpl(database) }
}
