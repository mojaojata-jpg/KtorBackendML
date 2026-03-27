package infrastructure.database

import infrastructure.database.tables.AdminTable
import infrastructure.database.tables.ProductTable // <-- Tambahin ini
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationEnvironment): Database {
        val url = config.config.property("database.url").getString()
        val user = config.config.property("database.user").getString()
        val password = config.config.property("database.password").getString()

        val db = Database.connect(
            url = url,
            user = user,
            driver = "org.postgresql.Driver",
            password = password
        )

        transaction(db) {
            exec("CREATE EXTENSION IF NOT EXISTS pgcrypto;")
            SchemaUtils.create(AdminTable, ProductTable) // <-- Tambahin ProductTable
        }
        return db
    }
}
