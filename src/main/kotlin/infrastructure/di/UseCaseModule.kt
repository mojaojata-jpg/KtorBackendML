package infrastructure.di

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase
import application.usecase.product.*
import application.usecase.iot.*
import application.usecase.sensor.ProcessSensorReadingUseCase
import application.usecase.sensor.GetSensorHistoryUseCase
import application.usecase.prediction.GetAllPredictionsUseCase
import application.usecase.prediction.GetPredictionByProductIdUseCase
import application.usecase.stock.*

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

    // IoT Device UseCases
    val registerDeviceUseCase: RegisterDeviceUseCase by lazy { RegisterDeviceUseCase(repositoryModule.iotDeviceRepository, repositoryModule.productRepository) }
    val getDevicesUseCase: GetDevicesUseCase by lazy { GetDevicesUseCase(repositoryModule.iotDeviceRepository) }
    val getDeviceByIdUseCase: GetDeviceByIdUseCase by lazy { GetDeviceByIdUseCase(repositoryModule.iotDeviceRepository) }
    val updateDeviceUseCase: UpdateDeviceUseCase by lazy { UpdateDeviceUseCase(repositoryModule.iotDeviceRepository, repositoryModule.productRepository) }
    val deleteDeviceUseCase: DeleteDeviceUseCase by lazy { DeleteDeviceUseCase(repositoryModule.iotDeviceRepository) }
    val updateDeviceLastSeenUseCase: UpdateDeviceLastSeenUseCase by lazy { UpdateDeviceLastSeenUseCase(repositoryModule.iotDeviceRepository) }
    val checkInactiveDevicesUseCase: CheckInactiveDevicesUseCase by lazy { CheckInactiveDevicesUseCase(repositoryModule.iotDeviceRepository) }

    // Sensor Data UseCase (PRO VERSION)
    val processSensorReadingUseCase: ProcessSensorReadingUseCase by lazy { 
        ProcessSensorReadingUseCase(repositoryModule.sensorRepository, repositoryModule.stockRepository, repositoryModule.iotDeviceRepository, repositoryModule.productRepository)
    }
    val getSensorHistoryUseCase: GetSensorHistoryUseCase by lazy { GetSensorHistoryUseCase(repositoryModule.sensorRepository) }

    // Stock UseCases
    val getStockDashboardUseCase: GetStockDashboardUseCase by lazy { GetStockDashboardUseCase(repositoryModule.stockRepository) }

    // Prediction UseCases
    val getAllPredictionsUseCase: GetAllPredictionsUseCase by lazy { GetAllPredictionsUseCase(repositoryModule.predictionRepository) }
    val getPredictionByProductIdUseCase: GetPredictionByProductIdUseCase by lazy { GetPredictionByProductIdUseCase(repositoryModule.predictionRepository) }
}
