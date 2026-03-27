package domain.repository

import domain.model.Product

interface ProductRepository {
    suspend fun create(product: Product): Product
    suspend fun findByCode(code: String): Product?
    suspend fun findById(id: String): Product?
    suspend fun getAll(page: Int, limit: Int): List<Product>
    suspend fun update(id: String, product: Product): Boolean
    suspend fun delete(id: String): Boolean
}
