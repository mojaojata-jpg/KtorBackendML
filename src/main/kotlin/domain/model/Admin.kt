package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Admin(
    val id: String? = null,
    val name: String,
    val email: String,
    val passwordHash: String
)
