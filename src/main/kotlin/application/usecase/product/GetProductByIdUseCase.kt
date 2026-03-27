package application.usecase.product

import domain.model.Product
import domain.repository.ProductRepository

class GetProductByIdUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: String): Product? {
        // Validasi ID format jika perlu (UUID)
        return repository.findById(id)
    }
}
