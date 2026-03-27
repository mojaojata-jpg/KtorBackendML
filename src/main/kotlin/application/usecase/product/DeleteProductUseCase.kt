package application.usecase.product

import domain.repository.ProductRepository

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: String): Boolean {
        // Validasi ID format jika perlu (UUID)
        return repository.delete(id)
    }
}
