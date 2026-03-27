package application.usecase.product

import domain.model.Product
import domain.repository.ProductRepository

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        id: String,
        name: String,
        code: String,
        unitWeight: Double,
        minStockThreshold: Int,
        description: String?
    ): Boolean {
        // 1. Validasi Input
        require(name.isNotBlank()) { "Product name cannot be empty" }
        require(code.isNotBlank()) { "Product code cannot be empty" }
        require(unitWeight > 0) { "Unit weight must be greater than zero" }
        require(minStockThreshold >= 0) { "Minimum stock threshold cannot be negative" }

        // 2. Business Logic: Cek kode produk duplikat (kalau diubah ke kode lain)
        val existingProductByCode = repository.findByCode(code)
        if (existingProductByCode != null && existingProductByCode.id != id) {
            throw IllegalArgumentException("Product code already exists with another product")
        }

        // 3. Create Entity for Update
        val product = Product(
            id = id, // ID tetap, ini untuk di update
            name = name,
            code = code,
            unitWeight = unitWeight,
            minStockThreshold = minStockThreshold,
            description = description
        )

        return repository.update(id, product)
    }
}
