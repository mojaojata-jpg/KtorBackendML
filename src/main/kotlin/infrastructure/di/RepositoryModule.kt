package infrastructure.di

import domain.repository.*
import infrastructure.repository.*
import org.jetbrains.exposed.sql.Database

class RepositoryModule(private val database: Database) {
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl() }
    val productRepository: ProductRepository by lazy { ProductRepositoryImpl() }
    val inventoryRepository: InventoryRepository by lazy { InventoryRepositoryImpl() }
    val aggregateRepository: AggregateRepository by lazy { AggregateRepositoryImpl() }
    val forecastRepository: ForecastRepository by lazy { ForecastRepositoryImpl() }
    val iotModeService: infrastructure.service.IotModeService by lazy { infrastructure.service.IotModeService() }
}

