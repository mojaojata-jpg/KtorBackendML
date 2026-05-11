import infrastructure.di.AppComponent
import io.ktor.server.application.*
import plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Railway/Production: Force port from environment variable if available
    val port = System.getenv("PORT")?.toInt() ?: 8080
    log.info("Server is starting on port: $port")

    val appComponent = AppComponent(environment)

    // 2. Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting(appComponent)
    
    log.info("Inventory RFID System Backend (V4.0) is running...")
}
