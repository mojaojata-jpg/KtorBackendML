package presentation.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import presentation.controller.ProductController

fun Route.productRoutes(productController: ProductController) {
    authenticate("auth-jwt") {
        route("/api/v1/products") {
            post {
                productController.createProduct(call)
            }
            get {
                productController.getProducts(call)
            }
            get("/{id}") {
                productController.getProductById(call)
            }
            put("/{id}") {
                productController.updateProduct(call)
            }
            delete("/{id}") {
                productController.deleteProduct(call)
            }
        }
    }
}
