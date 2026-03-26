package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error_code: String? = null
)
