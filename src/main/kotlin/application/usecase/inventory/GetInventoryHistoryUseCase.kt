package application.usecase.inventory

import domain.model.InventoryEvent
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import java.util.UUID

class GetInventoryHistoryUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, limit: Int = 50): List<InventoryEvent> {
        // Validasi apakah produk ada
        productRepository.findById(productId) 
            ?: throw IllegalArgumentException("Product with ID $productId not found")

        return inventoryRepository.getProductHistory(UUID.fromString(productId), limit)
    }
}
