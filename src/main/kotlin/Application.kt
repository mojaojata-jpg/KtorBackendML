import infrastructure.di.AppComponent
import io.ktor.server.application.*
import io.ktor.server.config.*
import plugins.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // 1. Inisialisasi Dependency Injection
    val appComponent = AppComponent(environment)

    // 2. Ambil config secara aman (fallback ke 5 menit)
    val inactivityThresholdMinutesValue = try {
        environment.config.propertyOrNull("iot.inactivityThresholdMinutes")?.getString()?.toLong() ?: 5L
    } catch (e: Exception) {
        5L
    }

    // 3. Start background task (Scheduler) menggunakan lifecycle Application
    launch {
        while (true) {
            delay(1.minutes)
            try {
                // Panggil UseCase PRO untuk ngecek status INACTIVE
                // FIX: Pastikan nama variabel dan parameter sinkron agar tidak abu-abu
                appComponent.useCaseModule.checkInactiveDevicesUseCase.invoke(
                    inactivityThresholdMinutes = inactivityThresholdMinutesValue
                )
            } catch (e: Exception) {
                log.error("Scheduler Job Error: ${e.message}")
            }
        }
    }

    // 4. Configure Plugins
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting(appComponent)
}
