package domain.repository

import domain.model.StockSnapshot
import infrastructure.util.Quadruple

interface StockRepository {
    suspend fun updateSnapshot(snapshot: StockSnapshot): Boolean
    suspend fun getCurrentStocks(): List<StockSnapshot>
    suspend fun getCurrentStocksWithProductInfo(): List<Quadruple<String, String, Int, StockSnapshot>> // <ProductName, ProductCode, MinThreshold, Snapshot>
    suspend fun getByProduct(productId: String): StockSnapshot?
}
