package application.usecase.inventory

import domain.model.InventoryEvent
import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import java.util.UUID

class GetInventoryHistoryUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, limit: Int = 50, days: Int? = null): List<InventoryEvent> {
        // Validasi apakah produk ada
        productRepository.findById(productId) 
            ?: throw IllegalArgumentException("Product with ID $productId not found")

        val startDate = days?.let { java.time.LocalDate.now().minusDays(it.toLong()) }

        return inventoryRepository.getProductHistory(
            productId = UUID.fromString(productId),
            limit = limit,
            startDate = startDate
        )
    }
}
