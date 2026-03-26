package infrastructure.security

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
    fun check(password: String, hashed: String): Boolean = BCrypt.checkpw(password, hashed)
}
