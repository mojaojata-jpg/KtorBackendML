package infrastructure.di

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase
import application.usecase.product.*

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
}
