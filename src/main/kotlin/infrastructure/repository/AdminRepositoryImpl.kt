package infrastructure.repository

import domain.model.Admin
import domain.repository.AdminRepository
import infrastructure.database.tables.AdminTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class AdminRepositoryImpl : AdminRepository {
    override suspend fun findByEmail(email: String): Admin? = newSuspendedTransaction {
        AdminTable.selectAll().where { AdminTable.email eq email }
            .map { rowToAdmin(it) }
            .singleOrNull()
    }

    override suspend fun findById(id: String): Admin? = newSuspendedTransaction {
        try {
            AdminTable.selectAll().where { AdminTable.id eq UUID.fromString(id) }
                .map { rowToAdmin(it) }
                .singleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun create(admin: Admin): Admin = newSuspendedTransaction {
        val newId = UUID.randomUUID()
        AdminTable.insert {
            it[id] = newId
            it[name] = admin.name
            it[email] = admin.email
            it[passwordHash] = admin.passwordHash
        }
        
        admin.copy(id = newId.toString())
    }

    private fun rowToAdmin(row: ResultRow) = Admin(
        id = row[AdminTable.id].toString(),
        name = row[AdminTable.name],
        email = row[AdminTable.email],
        passwordHash = row[AdminTable.passwordHash]
    )
}
