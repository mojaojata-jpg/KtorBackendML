package infrastructure.database

import infrastructure.database.tables.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig): Database {
        val driverClassName = "org.postgresql.Driver"
        // Railway/Production: Read from environment variables first, fallback to config file
        val jdbcUrl = System.getenv("DATABASE_URL") ?: config.property("database.url").getString()
        val user = System.getenv("DATABASE_USER") ?: config.property("database.user").getString()
        val password = System.getenv("DATABASE_PASSWORD") ?: config.property("database.password").getString()

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
                DailyAggregateTable,
                ForecastingResultTable
            )
        }
        
        return database
    }
}
