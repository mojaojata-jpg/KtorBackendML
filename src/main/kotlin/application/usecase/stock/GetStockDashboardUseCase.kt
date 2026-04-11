package application.usecase.stock

import domain.repository.StockRepository
import presentation.dto.response.StockDashboardResponse

class GetStockDashboardUseCase(private val stockRepository: StockRepository) {
    suspend operator fun invoke(): List<StockDashboardResponse> {
        val rawData = stockRepository.getCurrentStocksWithProductInfo()
        
        return rawData.map { (productName, productCode, minThreshold, snapshot) ->
            val stockStatus = when {
                snapshot.currentStock == 0 -> "OUT_OF_STOCK"
                snapshot.currentStock < minThreshold -> "LOW_STOCK"
                else -> "SUFFICIENT"
            }

            StockDashboardResponse(
                productId = snapshot.productId,
                productName = productName,
                productCode = productCode,
                currentWeight = snapshot.currentWeight,
                currentStock = snapshot.currentStock,
                minStockThreshold = minThreshold,
                stockStatus = stockStatus,
                status = snapshot.status,
                lastUpdated = snapshot.snapshotTime
            )
        }
    }
}
