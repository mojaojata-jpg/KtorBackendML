package infrastructure.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtService() {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "stok-pintar-secret-key-2026"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "https://ktormachinelearning-production.up.railway.app"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "https://ktormachinelearning-production.up.railway.app"

    fun generateToken(email: String): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
