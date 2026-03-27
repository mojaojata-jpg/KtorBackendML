package application.usecase.product

import domain.model.Product
import domain.repository.ProductRepository

class GetProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(page: Int, limit: Int): List<Product> {
        // Validasi input pagination
        require(page >= 1) { "Page number must be at least 1" }
        require(limit >= 1) { "Limit must be at least 1" }

        return repository.getAll(page, limit)
    }
}
