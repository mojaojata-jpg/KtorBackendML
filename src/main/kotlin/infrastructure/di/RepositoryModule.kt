package infrastructure.di

import data.repository.AdminRepositoryImpl
import data.repository.ProductRepositoryImpl
import domain.repository.AdminRepository
import domain.repository.ProductRepository
import org.jetbrains.exposed.sql.Database

class RepositoryModule(private val database: Database) {
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl(database) }
    val productRepository: ProductRepository by lazy { ProductRepositoryImpl(database) }
}
