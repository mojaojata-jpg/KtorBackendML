package infrastructure.di

import infrastructure.security.JwtService
import presentation.controller.*

class ControllerModule(
    private val useCaseModule: UseCaseModule,
    private val repositoryModule: RepositoryModule,
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

    val inventoryController: InventoryController by lazy {
        InventoryController(
            processRfidScanUseCase = useCaseModule.processRfidScanUseCase,
            registerRfidTagUseCase = useCaseModule.registerRfidTagUseCase,
            getInventoryDashboardUseCase = useCaseModule.getInventoryDashboardUseCase,
            getInventoryHistoryUseCase = useCaseModule.getInventoryHistoryUseCase,
            inventoryRepository = repositoryModule.inventoryRepository
        )
    }

    val predictionController: PredictionController by lazy {
        PredictionController(
            getAllPredictionsUseCase = useCaseModule.getAllPredictionsUseCase,
            getPredictionByProductIdUseCase = useCaseModule.getPredictionByProductIdUseCase
        )
    }
}
