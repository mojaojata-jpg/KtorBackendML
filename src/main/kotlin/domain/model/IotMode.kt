package domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class IotOperationMode {
    NORMAL,    // Scan IN/OUT biasa
    REGISTER,  // Register tag otomatis ke produk tertentu
    SCAN_OUT   // Scan OUT terus menerus (Continuous Mode)
}

data class IotSystemStatus(
    val mode: IotOperationMode = IotOperationMode.NORMAL,
    val targetProductId: String? = null
)
