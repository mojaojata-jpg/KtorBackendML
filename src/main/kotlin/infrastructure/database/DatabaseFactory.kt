package infrastructure.database

import infrastructure.database.tables.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig): Database {
        val driverClassName = "org.postgresql.Driver"
        val jdbcUrl = config.property("database.url").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()

        val database = Database.connect(
            url = jdbcUrl,
            driver = driverClassName,
            user = user,
            password = password
        )

        transaction(database) {
            SchemaUtils.create(
                AdminTable,
                ProductTable,
                ProductRfidTagTable,
                InventoryEventTable,
                InventorySnapshotTable,
                PredictionTable
            )
        }
        
        return database
    }
}
