package presentation.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class AdminResponse(
    val id: String,
    val name: String,
    val email: String
)
