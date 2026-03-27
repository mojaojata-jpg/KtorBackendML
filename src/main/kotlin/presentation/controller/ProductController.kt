package presentation.controller

import application.usecase.product.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import presentation.dto.request.ProductRequest
import presentation.dto.response.BaseResponse
import presentation.dto.response.ProductResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProductController(
    private val createProductUseCase: CreateProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) {
    suspend fun createProduct(call: ApplicationCall) {
        val request = call.receive<ProductRequest>()
        try {
            val product = createProductUseCase(
                name = request.name,
                code = request.code,
                unitWeight = request.unitWeight,
                minStockThreshold = request.minStockThreshold,
                description = request.description
            )
            val response = BaseResponse<ProductResponse>(
                success = true,
                data = ProductResponse(
                    id = product.id!!,
                    name = product.name,
                    code = product.code,
                    unitWeight = product.unitWeight,
                    minStockThreshold = product.minStockThreshold,
                    description = product.description,
                    createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                ),
                message = "Product created successfully"
            )
            call.respond(HttpStatusCode.Created, response)
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.Conflict,
                BaseResponse<Unit>(success = false, message = e.message)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }

    suspend fun getProducts(call: ApplicationCall) {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

        try {
            val products = getProductsUseCase(page, limit)
            val productResponses = products.map {
                ProductResponse(
                    id = it.id!!,
                    name = it.name,
                    code = it.code,
                    unitWeight = it.unitWeight,
                    minStockThreshold = it.minStockThreshold,
                    description = it.description,
                    createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }
            call.respond(HttpStatusCode.OK, BaseResponse<List<ProductResponse>>(success = true, data = productResponses))
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                BaseResponse<Unit>(success = false, message = e.message)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }

    suspend fun getProductById(call: ApplicationCall) {
        val id = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest, BaseResponse<Unit>(success = false, message = "Product ID is required")
        )

        try {
            val product = getProductByIdUseCase(id)
            if (product != null) {
                val response = BaseResponse<ProductResponse>(
                    success = true,
                    data = ProductResponse(
                        id = product.id!!,
                        name = product.name,
                        code = product.code,
                        unitWeight = product.unitWeight,
                        minStockThreshold = product.minStockThreshold,
                        description = product.description,
                        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
                )
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found"))
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }

    suspend fun updateProduct(call: ApplicationCall) {
        val id = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest, BaseResponse<Unit>(success = false, message = "Product ID is required")
        )
        val request = call.receive<ProductRequest>()

        try {
            val updated = updateProductUseCase(
                id = id,
                name = request.name,
                code = request.code,
                unitWeight = request.unitWeight,
                minStockThreshold = request.minStockThreshold,
                description = request.description
            )
            if (updated) {
                call.respond(HttpStatusCode.OK, BaseResponse<Unit>(success = true, message = "Product updated successfully"))
            } else {
                call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found or no changes made"))
            }
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.Conflict,
                BaseResponse<Unit>(success = false, message = e.message)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }

    suspend fun deleteProduct(call: ApplicationCall) {
        val id = call.parameters["id"] ?: return call.respond(
            HttpStatusCode.BadRequest, BaseResponse<Unit>(success = false, message = "Product ID is required")
        )

        try {
            val deleted = deleteProductUseCase(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, BaseResponse<Unit>(success = true, message = "Product deleted successfully"))
            } else {
                call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(success = false, message = "Product not found"))
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }
}
