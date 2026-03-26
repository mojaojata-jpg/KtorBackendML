# 🧠 AI Agent Skill — Backend Development (Ktor + Clean Architecture + DI)

---

# 1. 📌 Architecture Principle

Gunakan **Clean Architecture (Layered)** dengan pemisahan tanggung jawab yang jelas:

```
presentation (routes/controllers/dto)
        ↓
application (use cases)
        ↓
domain (entities + repository interface)
        ↓
data (repository implementation)
        ↓
infrastructure (database, security, config, DI)
```

---

# 2. 📁 Struktur Folder (FINAL — WAJIB IKUTI)

```

├── domain/
│   ├── model/
│   │    ├── Admin.kt
│   │    ├── Product.kt
│   │    └── SensorReading.kt
│   │
│   └── repository/
│        ├── AdminRepository.kt
│        ├── ProductRepository.kt
│        └── SensorRepository.kt
│
├── application/
│   └── usecase/
│        ├── auth/
│        │    ├── LoginUseCase.kt
│        │    └── RegisterUseCase.kt
│        │
│        ├── sensor/
│        │    └── ProcessSensorDataUseCase.kt
│        │
│        ├── stock/
│        │    └── GetStockUseCase.kt
│        │
│        └── prediction/
│             └── GetPredictionUseCase.kt
│
├── data/
│   ├── repository/
│   │    ├── AdminRepositoryImpl.kt
│   │    ├── ProductRepositoryImpl.kt
│   │    └── SensorRepositoryImpl.kt
│   │
│   └── datasource/
│        └── DatabaseFactory.kt
│
├── presentation/
│   ├── controller/
│   │    ├── AuthController.kt
│   │    ├── SensorController.kt
│   │    ├── StockController.kt
│   │    └── PredictionController.kt
│   │
│   ├── routes/
│   │    ├── AuthRoutes.kt
│   │    ├── SensorRoutes.kt
│   │    ├── StockRoutes.kt
│   │    └── PredictionRoutes.kt
│   │
│   └── dto/
│        ├── request/
│        │    ├── LoginRequest.kt
│        │    └── SensorDataRequest.kt
│        │
│        └── response/
│             ├── LoginResponse.kt
│             └── SensorDataResponse.kt
│
├── infrastructure/
│   ├── database/
│   │    ├── tables/
│   │    │    ├── AdminTable.kt
│   │    │    ├── ProductTable.kt
│   │    │    └── SensorReadingTable.kt
│   │    │
│   │    └── DatabaseConfig.kt
│   │
│   ├── security/
│   │    ├── JwtService.kt
│   │    └── PasswordHasher.kt
│   │
│   ├── config/
│   │    └── ApplicationConfig.kt
│   │
│   └── di/
│        ├── RepositoryModule.kt
│        ├── UseCaseModule.kt
│        ├── ControllerModule.kt
│        └── AppComponent.kt
│
└── plugins/
     ├── Routing.kt
     ├── Serialization.kt
     ├── Security.kt
     └── Monitoring.kt
```

---

# 3. 🧠 Layer Responsibility

## 🔹 Domain

* Berisi model dan interface repository
* Tidak boleh bergantung ke framework

---

## 🔹 Application (UseCase)

* Berisi business logic utama
* Contoh:

    * login
    * hitung stok
    * validasi sensor

---

## 🔹 Data

* Implementasi repository
* Berinteraksi langsung dengan database

---

## 🔹 Presentation

* Route: endpoint API
* Controller: jembatan ke use case
* DTO: request & response

---

## 🔹 Infrastructure

* Database config (Exposed + Hikari)
* Security (JWT + hashing)
* Config environment
* Dependency Injection (Dagger)

---

## 🔹 Plugins (Ktor Config)

* Routing setup
* JSON serialization
* Authentication
* Logging

---

# 4. 🔄 Flow System (WAJIB IKUTI)

## 📡 IoT Flow

```
IoT → POST /sensor-data
     → Route
     → Controller
     → UseCase
     → Repository
     → Database (sensor_readings + stock_snapshots)
```

---

## 📱 Client Flow

```
Client → GET /stocks
       → Backend
       → Database
       → Response
       → UI
```

---

## 🤖 ML Flow

```
Python → ambil data dari sensor_readings
       → training / prediction
       → simpan ke prediction_results
       → client ambil via GET
```

---

# 5. 🔐 Authentication Flow

```
Client → POST /auth/login
       → Controller
       → UseCase
       → Repository (ambil admin)
       → Password verify (BCrypt)
       → Generate JWT
       → Response ke client
```

---

# 6. 🧩 Dependency Injection Rules (Dagger)

## ❗ WAJIB:

* Tidak boleh `new` dependency di dalam class
* Semua dependency harus di-inject

---

## 🔹 Contoh:

### UseCase

```
class LoginUseCase(
    private val adminRepository: AdminRepository
)
```

---

## 🔹 Repository Binding

```
@Binds
abstract fun bindAdminRepository(
    impl: AdminRepositoryImpl
): AdminRepository
```

---

## 🔹 DI Flow

```
RepositoryImpl
   ↓
UseCase
   ↓
Controller
   ↓
Route
```

---

# 7. 📊 API Standard Response

Semua response harus konsisten:

```
{
  "success": true,
  "data": {},
  "message": "optional"
}
```

---

# 8. ⚙️ Best Practices (WAJIB)

* Gunakan Coroutine (suspend)
* Gunakan pagination untuk data besar
* Gunakan validation di UseCase
* Gunakan hashing password (BCrypt)
* Gunakan JWT untuk auth
* Pisahkan DTO dan Entity
* Jangan bocorkan database ke presentation layer

---

# 9. 🚫 Larangan

* ❌ Logic di route
* ❌ Query DB di controller
* ❌ new Repository di UseCase
* ❌ Campur layer

---

# 10. 🎯 Goal System

* Scalable
* Maintainable
* Clean architecture
* High performance
* Secure (JWT + hashing)

---

# 🔥 FINAL NOTE

Backend ini berfungsi sebagai:

```
Receive → Process → Store → Serve
```

Dan menjadi pusat kontrol untuk:

* IoT (data collector)
* ML (prediction engine)
* Client (monitoring UI)

```
```
