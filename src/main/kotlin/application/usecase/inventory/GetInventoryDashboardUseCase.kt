package application.usecase.inventory

import domain.model.InventorySnapshot
import domain.model.Product
import domain.repository.InventoryRepository

class GetInventoryDashboardUseCase(
    private val inventoryRepository: InventoryRepository
) {
    suspend operator fun invoke(): List<Pair<Product, InventorySnapshot?>> {
        return inventoryRepository.getDashboardSnapshots()
    }
}
