import infrastructure.di.AppComponent
import io.ktor.server.application.*
import plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // 1. Inisialisasi Dependency Injection (Manual)
    // AppComponent akan otomatis inisialisasi Database & JWT menggunakan environment
    val appComponent = AppComponent(environment)

    // 2. Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting(appComponent)
}
