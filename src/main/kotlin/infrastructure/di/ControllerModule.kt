package infrastructure.di

import infrastructure.security.JwtService
import presentation.controller.AuthController
import presentation.controller.ProductController

class ControllerModule(
    private val useCaseModule: UseCaseModule,
    private val jwtService: JwtService
) {
    val authController: AuthController by lazy { 
        AuthController(
            registerUseCase = useCaseModule.registerUseCase,
            loginUseCase = useCaseModule.loginUseCase,
            jwtService = jwtService
        ) 
    }

    val productController: ProductController by lazy {
        ProductController(
            createProductUseCase = useCaseModule.createProductUseCase,
            getProductsUseCase = useCaseModule.getProductsUseCase,
            getProductByIdUseCase = useCaseModule.getProductByIdUseCase,
            updateProductUseCase = useCaseModule.updateProductUseCase,
            deleteProductUseCase = useCaseModule.deleteProductUseCase
        )
    }
}
