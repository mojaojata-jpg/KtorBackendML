package infrastructure.di

import domain.repository.*
import infrastructure.repository.*
import org.jetbrains.exposed.sql.Database

class RepositoryModule(private val database: Database) {
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl() }
    val productRepository: ProductRepository by lazy { ProductRepositoryImpl() }
    val inventoryRepository: InventoryRepository by lazy { InventoryRepositoryImpl() }
    val predictionRepository: PredictionRepository by lazy { PredictionRepositoryImpl() }
}
