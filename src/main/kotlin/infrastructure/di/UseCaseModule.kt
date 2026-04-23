package infrastructure.di

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase
import application.usecase.product.*
import application.usecase.inventory.*
import application.usecase.prediction.GetAllPredictionsUseCase
import application.usecase.prediction.GetPredictionByProductIdUseCase

class UseCaseModule(private val repositoryModule: RepositoryModule) {
    // Auth UseCases
    val registerUseCase: RegisterUseCase by lazy { RegisterUseCase(repositoryModule.adminRepository) }
    val loginUseCase: LoginUseCase by lazy { LoginUseCase(repositoryModule.adminRepository) }

    // Product UseCases
    val createProductUseCase: CreateProductUseCase by lazy { CreateProductUseCase(repositoryModule.productRepository) }
    val getProductsUseCase: GetProductsUseCase by lazy { GetProductsUseCase(repositoryModule.productRepository) }
    val getProductByIdUseCase: GetProductByIdUseCase by lazy { GetProductByIdUseCase(repositoryModule.productRepository) }
    val updateProductUseCase: UpdateProductUseCase by lazy { UpdateProductUseCase(repositoryModule.productRepository) }
    val deleteProductUseCase: DeleteProductUseCase by lazy { DeleteProductUseCase(repositoryModule.productRepository) }

    // Inventory UseCases (RFID Based)
    val processRfidScanUseCase: ProcessRfidScanUseCase by lazy { 
        ProcessRfidScanUseCase(repositoryModule.inventoryRepository, repositoryModule.productRepository) 
    }
    val registerRfidTagUseCase: RegisterRfidTagUseCase by lazy {
        RegisterRfidTagUseCase(repositoryModule.inventoryRepository, repositoryModule.productRepository)
    }
    val getInventoryDashboardUseCase: GetInventoryDashboardUseCase by lazy {
        GetInventoryDashboardUseCase(repositoryModule.inventoryRepository)
    }
    val getInventoryHistoryUseCase: GetInventoryHistoryUseCase by lazy {
        GetInventoryHistoryUseCase(repositoryModule.inventoryRepository, repositoryModule.productRepository)
    }

    // Prediction UseCases
    val getAllPredictionsUseCase: GetAllPredictionsUseCase by lazy { GetAllPredictionsUseCase(repositoryModule.predictionRepository) }
    val getPredictionByProductIdUseCase: GetPredictionByProductIdUseCase by lazy { GetPredictionByProductIdUseCase(repositoryModule.predictionRepository) }
}
