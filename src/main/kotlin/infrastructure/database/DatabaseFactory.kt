package infrastructure.database

import infrastructure.database.tables.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(): Database {
        val driverClassName = "org.postgresql.Driver"
        // Force read from environment variables for Production (Railway)
        // Fallback to the known Neon URL if env is missing
        val jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://ep-rapid-haze-ao07neao-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require"
        val user = System.getenv("DATABASE_USER") ?: "neondb_owner"
        val password = System.getenv("DATABASE_PASSWORD") ?: "npg_2okqATC7Nrbm"

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
