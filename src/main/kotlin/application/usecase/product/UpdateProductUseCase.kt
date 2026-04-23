package application.usecase.product

import domain.model.Product
import domain.repository.ProductRepository

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        id: String,
        name: String,
        code: String,
        unitLabel: String,
        minStockThreshold: Int,
        description: String?,
        imageUrl: String? = null
    ): Boolean {
        // 1. Validasi Input
        require(name.isNotBlank()) { "Product name cannot be empty" }
        require(code.isNotBlank()) { "Product code cannot be empty" }
        require(unitLabel.isNotBlank()) { "Unit label cannot be empty" }
        require(minStockThreshold >= 0) { "Minimum stock threshold cannot be negative" }

        // 2. Business Logic: Cek kode produk duplikat (kalau diubah ke kode lain)
        val existingProductByCode = repository.findByCode(code)
        if (existingProductByCode != null && existingProductByCode.id.toString() != id) {
            throw IllegalArgumentException("Product code already exists with another product")
        }

        // 3. Create Entity for Update
        val product = Product(
            name = name,
            code = code,
            unitLabel = unitLabel,
            minStockThreshold = minStockThreshold,
            description = description,
            imageUrl = imageUrl
        )

        return repository.update(id, product)
    }
}
