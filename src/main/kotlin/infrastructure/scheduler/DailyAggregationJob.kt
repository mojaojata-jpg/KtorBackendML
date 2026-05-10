package infrastructure.scheduler

import application.usecase.RunDailyAggregationUseCase
import kotlinx.coroutines.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyAggregationJob(
    private val runDailyAggregationUseCase: RunDailyAggregationUseCase
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val now = LocalDateTime.now()
                val targetTime = LocalTime.of(23, 59)
                
                var targetDateTime = now.toLocalDate().atTime(targetTime)
                if (now.isAfter(targetDateTime)) {
                    // Jika sudah lewat 23:59 hari ini, jadwalkan untuk besok
                    targetDateTime = targetDateTime.plusDays(1)
                }

                val zdtNow = ZonedDateTime.of(now, ZoneId.systemDefault())
                val zdtTarget = ZonedDateTime.of(targetDateTime, ZoneId.systemDefault())
                
                val duration = Duration.between(zdtNow, zdtTarget)
                
                println("DailyAggregationJob: Next run scheduled at $targetDateTime (in ${duration.toMinutes()} minutes)")
                
                delay(duration.toMillis())
                
                try {
                    println("DailyAggregationJob: Running aggregation for ${targetDateTime.toLocalDate()}...")
                    runDailyAggregationUseCase.execute(targetDateTime.toLocalDate())
                    println("DailyAggregationJob: Aggregation completed successfully.")
                } catch (e: Exception) {
                    println("DailyAggregationJob: Error running aggregation - ${e.message}")
                    e.printStackTrace()
                }
                
                // Beri jeda sebentar agar tidak langsung trigger berkali-kali di menit yang sama
                delay(60000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
