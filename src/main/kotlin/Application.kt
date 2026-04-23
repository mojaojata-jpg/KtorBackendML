import infrastructure.di.AppComponent
import io.ktor.server.application.*
import plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // 1. Inisialisasi Dependency Injection (AppComponent mengelola database & modules)
    // Fokus sistem: RFID-Based Inventory Management & ML Prediction
    val appComponent = AppComponent(environment)

    // 2. Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting(appComponent)
    
    log.info("Inventory RFID System Backend (V4.0) is running...")
}
