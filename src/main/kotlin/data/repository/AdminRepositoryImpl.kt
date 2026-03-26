package data.repository

import domain.model.Admin
import domain.repository.AdminRepository
import infrastructure.database.tables.AdminTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class AdminRepositoryImpl(private val database: Database) : AdminRepository {

    override suspend fun create(admin: Admin): Admin = dbQuery {
        val insertStatement = AdminTable.insert {
            it[id] = UUID.randomUUID()
            it[name] = admin.name
            it[email] = admin.email
            it[passwordHash] = admin.passwordHash
        }
        
        insertStatement.resultedValues?.singleOrNull()?.let(::toAdmin) 
            ?: throw Exception("Failed to create admin")
    }

    override suspend fun findByEmail(email: String): Admin? = dbQuery {
        AdminTable.selectAll().where { AdminTable.email eq email }
            .map(::toAdmin)
            .singleOrNull()
    }

    override suspend fun findById(id: String): Admin? = dbQuery {
        AdminTable.selectAll().where { AdminTable.id eq UUID.fromString(id) }
            .map(::toAdmin)
            .singleOrNull()
    }

    private fun toAdmin(row: ResultRow) = Admin(
        id = row[AdminTable.id].toString(),
        name = row[AdminTable.name],
        email = row[AdminTable.email],
        passwordHash = row[AdminTable.passwordHash]
    )

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }
}
