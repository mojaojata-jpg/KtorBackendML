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
        val request = call.receive<RegisterRequest>()
        try {
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
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.Conflict,
                BaseResponse<Unit>(success = false, message = e.message)
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse<Unit>(success = false, message = "Internal Server Error")
            )
        }
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receive<LoginRequest>()
        try {
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
        } catch (e: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.Unauthorized,
                BaseResponse<Unit>(success = false, message = e.message)
            )
        }
    }
}
