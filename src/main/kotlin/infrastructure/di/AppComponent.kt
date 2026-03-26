package infrastructure.di

import infrastructure.database.DatabaseFactory
import infrastructure.security.JwtService
import io.ktor.server.application.*

class AppComponent(environment: ApplicationEnvironment) {
    private val database = DatabaseFactory.init(environment)
    val jwtService = JwtService(environment)

    val repositoryModule by lazy { RepositoryModule(database) }
    val useCaseModule by lazy { UseCaseModule(repositoryModule) }
    val controllerModule by lazy { ControllerModule(useCaseModule, jwtService) }
}
