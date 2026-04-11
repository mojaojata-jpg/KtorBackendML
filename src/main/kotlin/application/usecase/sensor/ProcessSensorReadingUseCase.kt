package application.usecase.sensor

import domain.model.SensorReading
import domain.model.StockSnapshot
import domain.repository.IOTDeviceRepository
import domain.repository.ProductRepository
import domain.repository.SensorRepository
import domain.repository.StockRepository
import kotlin.math.abs
import kotlin.math.floor

class ProcessSensorReadingUseCase(
    private val sensorRepository: SensorRepository,
    private val stockRepository: StockRepository,
    private val deviceRepository: IOTDeviceRepository,
    private val productRepository: ProductRepository
) {

    suspend operator fun invoke(
        deviceCode: String,
        rawWeight: Double,
        filteredWeight: Double,
        recordedAt: String
    ): SensorReading {
        // 1. Validasi Input Dasar
        require(deviceCode.isNotBlank()) { "Device code is required" }
        require(rawWeight >= 0) { "Raw weight cannot be negative" }
        require(filteredWeight >= 0) { "Filtered weight cannot be negative" }

        // 2. Business Rules: Pastikan Device dan Product terdaftar
        val device = deviceRepository.findByCode(deviceCode)
            ?: throw IllegalArgumentException("Device not found: $deviceCode")

        val product = productRepository.findById(device.productId)
            ?: throw Exception("Product associated with device $deviceCode not found")

        val deviceId = device.id ?: throw Exception("Internal Error: Device ID missing")
        val productId = product.id ?: throw Exception("Internal Error: Product ID missing")

        // 3. Hitung Noise Level & Estimasi Stok
        val noiseLevel = abs(rawWeight - filteredWeight)
        val estimatedStock = floor(filteredWeight / product.unitWeight).toInt()
        
        // 4. Tentukan Validation Status
        var validationStatus = "VALID"
        if (noiseLevel > 1.0) {
            validationStatus = "UNSTABLE"
        }

        // 5. UPDATE SNAPSHOT (SELALU UPDATE - Agar Dashboard Android tetap "Live" / Real-time)
        // Kita update snapshot agar timestamp 'lastUpdated' dan 'currentWeight' di dashboard selalu baru
        val snapshot = StockSnapshot(
            productId = productId,
            deviceId = deviceId,
            currentWeight = filteredWeight,
            currentStock = estimatedStock,
            status = validationStatus,
            snapshotTime = recordedAt
        )
        stockRepository.updateSnapshot(snapshot)

        // 6. Ambil Data Terakhir untuk Perbandingan Histori (Deduplication)
        val previousReading = sensorRepository.getLatestReading(deviceId)
        
        // --- LOGIC SKIP HISTORY (Deduplication) ---
        // Kita SKIP simpan ke tabel histori (sensor_readings) kalau stok masih sama (hemat storage Neon)
        // Tapi Snapshot di atas tetap terupdate!
        if (previousReading != null && estimatedStock == previousReading.estimatedStock) {
            deviceRepository.updateLastSeen(deviceId)
            return previousReading
        }

        // 7. Simpan Histori (Hanya jika stok berubah)
        val reading = SensorReading(
            deviceId = deviceId,
            productId = productId,
            rawWeight = rawWeight,
            filteredWeight = filteredWeight,
            estimatedStock = estimatedStock,
            validationStatus = validationStatus,
            noiseLevel = noiseLevel,
            isAnomaly = false,
            recordedAt = recordedAt
        )

        val savedReading = sensorRepository.saveReading(reading)

        // 8. Update Last Seen Alat IoT
        deviceRepository.updateLastSeen(deviceId)

        return savedReading
    }
}
