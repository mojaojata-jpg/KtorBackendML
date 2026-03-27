package data.repository

import domain.model.Product
import domain.repository.ProductRepository
import infrastructure.database.tables.ProductTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalDateTime
import java.util.*

class ProductRepositoryImpl(private val database: Database) : ProductRepository {

    override suspend fun create(product: Product): Product = dbQuery {
        val insertStatement = ProductTable.insert {
            it[id] = UUID.randomUUID()
            it[name] = product.name
            it[code] = product.code
            it[unitWeight] = product.unitWeight.toBigDecimal()
            it[minStockThreshold] = product.minStockThreshold
            it[description] = product.description
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }

        insertStatement.resultedValues?.singleOrNull()?.let(::toProduct) 
            ?: throw Exception("Failed to create product")
    }

    override suspend fun findByCode(code: String): Product? = dbQuery {
        ProductTable.selectAll().where { ProductTable.code eq code }
            .map(::toProduct)
            .singleOrNull()
    }

    override suspend fun findById(id: String): Product? = dbQuery {
        ProductTable.selectAll().where { ProductTable.id eq UUID.fromString(id) }
            .map(::toProduct)
            .singleOrNull()
    }

    override suspend fun getAll(page: Int, limit: Int): List<Product> = dbQuery {
        ProductTable.selectAll()
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map(::toProduct)
    }

    override suspend fun update(id: String, product: Product): Boolean = dbQuery {
        ProductTable.update({ ProductTable.id eq UUID.fromString(id) }) {
            it[name] = product.name
            it[code] = product.code
            it[unitWeight] = product.unitWeight.toBigDecimal()
            it[minStockThreshold] = product.minStockThreshold
            it[description] = product.description
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        ProductTable.deleteWhere { ProductTable.id eq UUID.fromString(id) } > 0
    }

    private fun toProduct(row: ResultRow) = Product(
        id = row[ProductTable.id].toString(),
        name = row[ProductTable.name],
        code = row[ProductTable.code],
        unitWeight = row[ProductTable.unitWeight].toDouble(),
        minStockThreshold = row[ProductTable.minStockThreshold],
        description = row[ProductTable.description]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
