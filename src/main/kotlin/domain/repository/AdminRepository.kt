package domain.repository

import domain.model.Admin

interface AdminRepository {
    suspend fun create(admin: Admin): Admin
    suspend fun findByEmail(email: String): Admin?
    suspend fun findById(id: String): Admin?
}
