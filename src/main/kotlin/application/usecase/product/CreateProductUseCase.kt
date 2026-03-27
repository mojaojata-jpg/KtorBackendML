package application.usecase.product

import domain.model.Product
import domain.repository.ProductRepository

class CreateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        name: String,
        code: String,
        unitWeight: Double,
        minStockThreshold: Int,
        description: String?
    ): Product {
        // 1. Validasi Input
        require(name.isNotBlank()) { "Product name cannot be empty" }
        require(code.isNotBlank()) { "Product code cannot be empty" }
        require(unitWeight > 0) { "Unit weight must be greater than zero" }
        require(minStockThreshold >= 0) { "Minimum stock threshold cannot be negative" }

        // 2. Business Logic: Cek kode produk duplikat
        if (repository.findByCode(code) != null) {
            throw IllegalArgumentException("Product code already exists")
        }

        // 3. Create Entity & Save
        val product = Product(
            name = name,
            code = code,
            unitWeight = unitWeight,
            minStockThreshold = minStockThreshold,
            description = description
        )

        return repository.create(product)
    }
}
