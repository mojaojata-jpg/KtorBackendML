package presentation.controller

import application.usecase.product.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import presentation.dto.request.ProductRequest
import presentation.dto.response.BaseResponse
import presentation.dto.response.ProductResponse
import java.time.format.DateTimeFormatter

class ProductController(
    private val createProductUseCase: CreateProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) {
    suspend fun createProduct(call: ApplicationCall) {
        val request = try {
            call.receive<ProductRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields")
        }

        // 1. Validasi Input (Spesifik untuk Frontend)
        require(request.name.isNotBlank()) { "Product name cannot be empty" }
        require(request.code.isNotBlank()) { "Product code (SKU) is required" }
        require(request.unitLabel.isNotBlank()) { "Unit label (e.g., pcs, kg) is required" }
        require(request.minStockThreshold >= 0) { "Minimum stock threshold cannot be negative" }

        val product = createProductUseCase(
            name = request.name,
            code = request.code,
            unitLabel = request.unitLabel,
            minStockThreshold = request.minStockThreshold,
            description = request.description,
            imageUrl = request.imageUrl
        )
        
        val response = BaseResponse(
            success = true,
            data = ProductResponse(
                id = product.id.toString(),
                name = product.name,
                code = product.code,
                unitLabel = product.unitLabel,
                minStockThreshold = product.minStockThreshold,
                description = product.description,
                imageUrl = product.imageUrl,
                createdAt = product.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                updatedAt = product.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ),
            message = "Product created successfully"
        )
        call.respond(HttpStatusCode.Created, response)
    }

    suspend fun getProducts(call: ApplicationCall) {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

        val products = getProductsUseCase(page, limit)
        val productResponses = products.map {
            ProductResponse(
                id = it.id.toString(),
                name = it.name,
                code = it.code,
                unitLabel = it.unitLabel,
                minStockThreshold = it.minStockThreshold,
                description = it.description,
                createdAt = it.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                updatedAt = it.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
        call.respond(HttpStatusCode.OK, BaseResponse(success = true, data = productResponses))
    }

    suspend fun getProductById(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID is required")

        val product = getProductByIdUseCase(id)
        if (product != null) {
            val response = BaseResponse(
                success = true,
                data = ProductResponse(
                    id = product.id.toString(),
                    name = product.name,
                    code = product.code,
                    unitLabel = product.unitLabel,
                    minStockThreshold = product.minStockThreshold,
                    description = product.description,
                    createdAt = product.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt = product.updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
            call.respond(HttpStatusCode.OK, response)
        } else {
            call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found"))
        }
    }

    suspend fun updateProduct(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID is required")
        
        val request = try {
            call.receive<ProductRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields")
        }

        // Validasi Input
        require(request.name.isNotBlank()) { "Product name cannot be empty" }
        require(request.code.isNotBlank()) { "Product code is required" }
        require(request.minStockThreshold >= 0) { "Minimum stock threshold cannot be negative" }

        val updated = updateProductUseCase(
            id = id,
            name = request.name,
            code = request.code,
            unitLabel = request.unitLabel,
            minStockThreshold = request.minStockThreshold,
            description = request.description,
            imageUrl = request.imageUrl
        )
        
        if (updated) {
            call.respond(HttpStatusCode.OK, BaseResponse<Unit>(success = true, message = "Product updated successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found or no changes made"))
        }
    }

    suspend fun deleteProduct(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw IllegalArgumentException("Product ID is required")

        val deleted = deleteProductUseCase(id)
        if (deleted) {
            call.respond(HttpStatusCode.OK, BaseResponse<Unit>(success = true, message = "Product deleted successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found"))
        }
    }
}
