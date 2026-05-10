package application.usecase

import domain.repository.InventoryRepository
import domain.repository.ProductRepository
import presentation.dto.response.DailyReportResponse
import presentation.dto.response.ProductDailyStats
import presentation.dto.response.ScanLogEntry
import java.time.LocalDate

class GetDailyReportUseCase(
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository
) {
    suspend fun execute(date: LocalDate = LocalDate.now()): DailyReportResponse {
        // 1. Get all active products with their snapshots
        val dashboardData = inventoryRepository.getDashboardSnapshots()

        // 2. For each product, calculate IN/OUT for the specified date
        val productStats = dashboardData.map { (product, snapshot) ->
            val (totalIn, totalOut) = inventoryRepository.getProductStats(
                product.id!!, date, date
            )

            // Recalculate status based on current stock and threshold
            val currentStock = snapshot?.currentStock ?: 0
            val status = when {
                currentStock == 0 -> "OUT_OF_STOCK"
                currentStock <= product.minStockThreshold -> "LOW_STOCK"
                else -> "SUFFICIENT"
            }

            ProductDailyStats(
                productId = product.id.toString(),
                productName = product.name,
                productCode = product.code,
                unit = product.unitLabel,
                totalIn = totalIn,
                totalOut = totalOut,
                netFlow = totalIn - totalOut,
                currentStock = currentStock,
                status = status
            )
        }

        // 3. Get scan logs for the day
        val eventsWithProducts = inventoryRepository.getAllEventsForDate(date)
        val scanLogs = eventsWithProducts.map { (event, product) ->
            ScanLogEntry(
                eventType = event.eventType,
                productName = product.name,
                productCode = product.code,
                quantity = event.quantity,
                recordedAt = event.recordedAt.toString(),
                note = event.note
            )
        }

        return DailyReportResponse(
            date = date.toString(),
            products = productStats,
            grandTotalIn = productStats.sumOf { it.totalIn },
            grandTotalOut = productStats.sumOf { it.totalOut },
            grandNetFlow = productStats.sumOf { it.netFlow },
            scanLogs = scanLogs
        )
    }
}
