package presentation.controller

import application.usecase.auth.LoginUseCase
import application.usecase.auth.RegisterUseCase
import infrastructure.security.JwtService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import presentation.dto.request.LoginRequest
import presentation.dto.request.RegisterRequest
import presentation.dto.response.AdminResponse
import presentation.dto.response.BaseResponse
import presentation.dto.response.LoginResponse

class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val jwtService: JwtService
) {
    suspend fun register(call: ApplicationCall) {
        val request = try {
            call.receive<RegisterRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields")
        }

        // 1. Input Validation (Pesan error spesifik untuk Frontend)
        require(request.name.isNotBlank()) { "Name is required" }
        require(request.name.length >= 3) { "Name must be at least 3 characters" }
        require(request.email.isNotBlank()) { "Email is required" }
        require(request.email.contains("@")) { "Invalid email format" }
        require(request.password.isNotBlank()) { "Password is required" }
        require(request.password.length >= 8) { "Password must be at least 8 characters" }

        val admin = registerUseCase(
            name = request.name,
            email = request.email,
            password = request.password
        )
        val response = BaseResponse(
            success = true,
            data = AdminResponse(
                id = admin.id!!,
                name = admin.name,
                email = admin.email
            ),
            message = "Admin registered successfully"
        )
        call.respond(HttpStatusCode.Created, response)
    }

    suspend fun login(call: ApplicationCall) {
        val request = try {
            call.receive<LoginRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields")
        }

        // 1. Input Validation
        require(request.email.isNotBlank()) { "Email is required" }
        require(request.password.isNotBlank()) { "Password is required" }

        val admin = loginUseCase(request.email, request.password)
        val token = jwtService.generateToken(admin.email)
        val response = BaseResponse(
            success = true,
            data = LoginResponse(
                accessToken = token,
                expiresIn = 3600
            ),
            message = "Login successful"
        )
        call.respond(HttpStatusCode.OK, response)
    }
}
