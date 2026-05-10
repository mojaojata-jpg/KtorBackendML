package infrastructure.di

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase
import application.usecase.product.*
import application.usecase.inventory.*

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

    // Aggregation & Chart UseCases
    val runDailyAggregationUseCase: application.usecase.RunDailyAggregationUseCase by lazy {
        application.usecase.RunDailyAggregationUseCase(repositoryModule.aggregateRepository)
    }
    val syncAggregationUseCase: application.usecase.SyncAggregationUseCase by lazy {
        application.usecase.SyncAggregationUseCase(repositoryModule.aggregateRepository)
    }
    val getChartDataUseCase: application.usecase.GetChartDataUseCase by lazy {
        application.usecase.GetChartDataUseCase(
            repositoryModule.productRepository,
            repositoryModule.inventoryRepository,
            repositoryModule.aggregateRepository,
            repositoryModule.forecastRepository
        )
    }

    // Report UseCases
    val getMonthlySummaryUseCase: application.usecase.GetMonthlySummaryUseCase by lazy {
        application.usecase.GetMonthlySummaryUseCase(repositoryModule.productRepository, repositoryModule.aggregateRepository)
    }
    val getDailyReportUseCase: application.usecase.GetDailyReportUseCase by lazy {
        application.usecase.GetDailyReportUseCase(repositoryModule.inventoryRepository, repositoryModule.productRepository)
    }
}

