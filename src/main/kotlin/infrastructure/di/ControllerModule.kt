package infrastructure.di

import infrastructure.security.JwtService
import presentation.controller.AuthController

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
}
