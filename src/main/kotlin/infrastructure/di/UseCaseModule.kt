package infrastructure.di

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase

class UseCaseModule(private val repositoryModule: RepositoryModule) {
    val registerUseCase: RegisterUseCase by lazy { RegisterUseCase(repositoryModule.adminRepository) }
    val loginUseCase: LoginUseCase by lazy { LoginUseCase(repositoryModule.adminRepository) }
}
