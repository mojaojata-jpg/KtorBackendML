package application.usecase.auth

import domain.model.Admin
import domain.repository.AdminRepository
import infrastructure.security.PasswordHasher

class RegisterUseCase(private val repository: AdminRepository) {
    suspend operator fun invoke(name: String, email: String, password: String): Admin {
        // 1. Input Validation
        require(name.isNotBlank()) { "Name cannot be empty" }
        require(email.contains("@") && email.contains(".")) { "Invalid email format" }
        require(password.length >= 8) { "Password must be at least 8 characters" }

        // 2. Business Logic: Check duplicate email
        if (repository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email already exists")
        }
        
        // 3. Security: Hash password
        val hashedPassword = PasswordHasher.hash(password)
        
        // 4. Create Entity & Save
        val admin = Admin(
            name = name,
            email = email,
            passwordHash = hashedPassword
        )
        
        return repository.create(admin)
    }
}
