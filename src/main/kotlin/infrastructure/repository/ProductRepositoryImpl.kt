package infrastructure.repository

import domain.model.Product
import domain.repository.ProductRepository
import infrastructure.database.tables.ProductTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class ProductRepositoryImpl : ProductRepository {
    override suspend fun getAll(page: Int, limit: Int): List<Product> = newSuspendedTransaction {
        val offset = ((page - 1).toLong() * limit)
        ProductTable.selectAll()
            .limit(limit)
            .offset(offset)
            .map { rowToProduct(it) }
    }

    override suspend fun findById(id: String): Product? = newSuspendedTransaction {
        try {
            ProductTable.selectAll().where { ProductTable.id eq UUID.fromString(id) }
                .map { rowToProduct(it) }
                .singleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByCode(code: String): Product? = newSuspendedTransaction {
        ProductTable.selectAll().where { ProductTable.code eq code }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    override suspend fun create(product: Product): Product = newSuspendedTransaction {
        val newId = UUID.randomUUID()
        ProductTable.insert {
            it[ProductTable.id] = newId
            it[name] = product.name
            it[code] = product.code
            it[unitLabel] = product.unitLabel
            it[minStockThreshold] = product.minStockThreshold
            it[description] = product.description
            it[imageUrl] = product.imageUrl
            it[isActive] = product.isActive
        }
        
        product.copy(id = newId)
    }

    override suspend fun update(id: String, product: Product): Boolean = newSuspendedTransaction {
        val updatedRows = ProductTable.update({ ProductTable.id eq UUID.fromString(id) }) {
            it[name] = product.name
            it[code] = product.code
            it[unitLabel] = product.unitLabel
            it[minStockThreshold] = product.minStockThreshold
            it[description] = product.description
            it[imageUrl] = product.imageUrl
            it[isActive] = product.isActive
            it[updatedAt] = java.time.LocalDateTime.now()
        }
        updatedRows > 0
    }

    override suspend fun delete(id: String): Boolean = newSuspendedTransaction {
        try {
            val deletedRows = ProductTable.deleteWhere { ProductTable.id eq UUID.fromString(id) }
            deletedRows > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun rowToProduct(row: ResultRow) = Product(
        id = row[ProductTable.id],
        name = row[ProductTable.name],
        code = row[ProductTable.code],
        unitLabel = row[ProductTable.unitLabel],
        minStockThreshold = row[ProductTable.minStockThreshold],
        description = row[ProductTable.description],
        imageUrl = row[ProductTable.imageUrl],
        isActive = row[ProductTable.isActive],
        createdAt = row[ProductTable.createdAt],
        updatedAt = row[ProductTable.updatedAt]
    )
}
