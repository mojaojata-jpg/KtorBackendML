package application.usecase.product

import domain.repository.ProductRepository
import java.util.UUID

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: String): Boolean {
        return try {
            UUID.fromString(id)
            // Pastikan produk ada sebelum hapus
            if (repository.findById(id) == null) return false
            
            // Eksekusi hapus (Database Cascade akan menangani Tags, Events, Snapshots, & Predictions)
            repository.delete(id)
        } catch (e: Exception) {
            false
        }
    }
}
