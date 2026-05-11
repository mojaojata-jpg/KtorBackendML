package plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

fun Application.configureSecurity() {
    // Railway/Production: Read from environment variables first, fallback to config file
    val jwtSecret = System.getenv("JWT_SECRET") ?: "stok-pintar-secret-key-2026"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "https://ktormachinelearning-production.up.railway.app"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "https://ktormachinelearning-production.up.railway.app"
    val jwtRealm = "ktor-sample-app"

    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
