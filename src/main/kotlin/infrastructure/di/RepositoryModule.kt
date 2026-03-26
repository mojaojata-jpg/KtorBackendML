package infrastructure.di

import data.repository.AdminRepositoryImpl
import domain.repository.AdminRepository
import org.jetbrains.exposed.sql.Database

class RepositoryModule(private val database: Database) {
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl(database) }
}
