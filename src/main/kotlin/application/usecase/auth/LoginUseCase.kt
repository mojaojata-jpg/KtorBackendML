package application.usecase.auth

import domain.model.Admin
import domain.repository.AdminRepository
import infrastructure.security.PasswordHasher

class LoginUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(email: String, password: String): Admin {
        // 1. Cari admin berdasarkan email
        val admin = repository.findByEmail(email) 
            ?: throw IllegalArgumentException("Invalid email or password")
            
        // 2. Verifikasi password yang di-input dengan hash di DB
        if (!PasswordHasher.check(password, admin.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        
        return admin
    }
}
