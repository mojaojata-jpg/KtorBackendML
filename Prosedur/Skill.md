# 🚀 Backend Engineering Skill - Ktor Clean Architecture (Production Ready)

## 🎯 Objective

Membangun backend system berbasis **Ktor + PostgreSQL (Neon)** dengan standar:

* Clean Architecture
* Scalable & Maintainable
* High Performance
* Secure by Design

---

# 🧠 1. Architecture Principle

Gunakan **Clean Architecture (Layered)**:

```
presentation (routes/controllers)
        ↓
application (use cases)
        ↓
domain (entities)
        ↓
data (repository implementation)
```

---

## 📁 Struktur Folder

```
src/main/kotlin/
 ├── domain/
 │    ├── model/
 │    └── repository/
 │
 ├── application/
 │    └── usecase/
 │
 ├── data/
 │    ├── repository/
 │    └── datasource/
 │
 ├── presentation/
 │    ├── routes/
 │    └── dto/
 │
 ├── infrastructure/
 │    ├── database/
 │    ├── security/
 │    ├── config/
 │    └── di/
```

---

# 🧱 2. Domain Layer (Core)

## Entity Example

```kotlin
data class Admin(
    val id: String,
    val name: String,
    val email: String,
    val passwordHash: String
)
```

---

## Repository Interface

```kotlin
interface AdminRepository {
    suspend fun create(admin: Admin): Admin
    suspend fun findByEmail(email: String): Admin?
    suspend fun findById(id: String): Admin?
}
```

👉 1 entity = 1 repository

---

# ⚙️ 3. Use Case Layer

## Principle:

* 1 Use Case = 1 Action
* Tidak ada logic di controller

---

## Example: CreateAdminUseCase

```kotlin
class CreateAdminUseCase(
    private val repository: AdminRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String): Admin {
        val hashed = hashPassword(password)

        val admin = Admin(
            id = generateId(),
            name = name,
            email = email,
            passwordHash = hashed
        )

        return repository.create(admin)
    }
}
```

---

# 🗄️ 4. Data Layer

## Implementation Repository

```kotlin
class AdminRepositoryImpl(
    private val db: Database
) : AdminRepository {

    override suspend fun create(admin: Admin): Admin {
        return dbQuery {
            // insert logic (Exposed)
        }
    }

    override suspend fun findByEmail(email: String): Admin? {
        return dbQuery {
            // select logic
        }
    }
}
```

---

# ⚡ 5. Coroutine Best Practice

* Semua DB operation → `suspend`
* Gunakan dispatcher:

```kotlin
withContext(Dispatchers.IO) { ... }
```

---

# 🔄 6. Lazy Initialization

Gunakan lazy untuk heavy object:

```kotlin
val dataSource by lazy {
    createDataSource()
}
```

---

# 🧪 7. Input Validation

Gunakan validation layer:

```kotlin
fun validateEmail(email: String) {
    require(email.contains("@")) { "Invalid email" }
}
```

Atau gunakan:

* RequestValidation plugin

---

# 🔐 8. Authentication & Security

## JWT Setup

* Access Token (short-lived)
* Secret key aman
* Expired time

---

## Password Hashing

Gunakan BCrypt:

```kotlin
fun hashPassword(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt())
}
```

---

## Verify Password

```kotlin
BCrypt.checkpw(inputPassword, storedHash)
```

---

# 🚫 9. Rate Limiting

Gunakan:

* Limit endpoint login
* Limit IoT endpoint

Example:

```
10 request / second per device
```

---

# 📊 10. Pagination

Gunakan limit & offset:

```kotlin
fun getProducts(limit: Int, offset: Int): List<Product>
```

---

# ⚡ 11. Performance Optimization

* Gunakan index DB
* Gunakan connection pooling (HikariCP)
* Hindari query berat di endpoint realtime
* Gunakan snapshot table untuk UI

---

# 🧩 12. Dependency Injection

Gunakan manual DI atau framework (Koin/Dagger)

Example:

```kotlin
val adminRepository = AdminRepositoryImpl(db)
val createAdminUseCase = CreateAdminUseCase(adminRepository)
```

---

# 📡 13. API Design

## Endpoint Pattern

```
POST   /admins
POST   /auth/login
GET    /products
POST   /sensor-data
GET    /predictions
```

---

# 🧠 14. IoT Handling

* Validate data
* Reject anomaly
* Store raw & filtered data
* Update snapshot

---

# ⚠️ 15. Error Handling

Gunakan StatusPages:

```kotlin
install(StatusPages) {
    exception<Throwable> { call, cause ->
        call.respond(HttpStatusCode.InternalServerError)
    }
}
```

---

# 🔥 16. Best Practices

✔ Tidak ada business logic di route
✔ Semua logic di use case
✔ Repository hanya akses DB
✔ Gunakan DTO untuk request/response
✔ Jangan expose entity langsung

---

# 🧪 17. Testing Strategy

* Unit test → use case
* Integration test → repository
* API test → endpoint

---

# 🚀 18. Scalability Strategy

* Gunakan queue jika traffic tinggi
* Pisahkan ML service
* Gunakan caching jika perlu

---

# 🔚 Conclusion

Backend harus:

✔ Clean
✔ Modular
✔ Secure
✔ Fast
✔ Easy to scale

---

👉 Ini bukan sekadar backend, tapi **production-grade system design**
