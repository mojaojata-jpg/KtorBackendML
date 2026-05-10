# Implementation Plan - Manual Aggregator Sync

This plan outlines the implementation of a new administrative endpoint `POST /api/admin/inventory/aggregate/sync` to manually trigger a real-time synchronization of daily stock aggregates for all active products. This ensures the ML Service has up-to-date data for retraining.

## Proposed Changes

### Domain Layer

#### [ProductRepository.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/domain/repository/ProductRepository.kt)
- Add `suspend fun getAllActive(): List<Product>` to the interface.

#### [AggregateRepository.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/domain/repository/AggregateRepository.kt)
- Add `suspend fun syncProducts(date: LocalDate, productIds: List<UUID>)` to support targeted aggregation.

---

### Data/Infrastructure Layer

#### [ProductRepositoryImpl.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/infrastructure/repository/ProductRepositoryImpl.kt)
- Implement `getAllActive()` using `ProductTable.isActive eq true`.

#### [AggregateRepositoryImpl.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/infrastructure/repository/AggregateRepositoryImpl.kt)
- Implement `syncProducts(date: LocalDate, productIds: List<UUID>)`:
    - Iterate through `productIds`.
    - Calculate `totalIn` (REGISTER, IN) and `totalOut` (OUT) for the specific `date`.
    - Perform UPSERT into `DailyAggregateTable`.
    - **Crucial**: Ensure `updatedAt` is set to `LocalDateTime.now()` to notify ML Service.

#### [JwtService.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/infrastructure/security/JwtService.kt)
- Update `generateToken` to include `id` and `role` claims.
- Default role for `Admin` will be `"ADMIN"`.

---

### Application/UseCase Layer

#### [NEW] [SyncDailyAggregatesUseCase.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/application/usecase/SyncDailyAggregatesUseCase.kt)
- New UseCase that:
    1. Fetches all active products from `ProductRepository`.
    2. Calls `AggregateRepository.syncProducts` with the active product IDs and current date.

#### [UseCaseModule.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/infrastructure/di/UseCaseModule.kt)
- Register the new `SyncDailyAggregatesUseCase`.

---

### Presentation/API Layer

#### [InventoryController.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/presentation/controller/InventoryController.kt)
- Add `syncAggregates(call: ApplicationCall)` method.
- Handle role validation (check `role` claim in JWT principal).
- Call `SyncDailyAggregatesUseCase`.
- Return success response.

#### [AuthController.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/presentation/controller/AuthController.kt)
- Update `login` to pass `admin.id` and `"ADMIN"` role to `jwtService.generateToken`.

#### [ControllerModule.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/infrastructure/di/ControllerModule.kt)
- Inject the new UseCase into `InventoryController`.

#### [InventoryRoutes.kt](file:///D:/2026ai/ktor-MachineL/src/main/kotlin/presentation/routes/InventoryRoutes.kt)
- Define a new route group `/api/admin` (or just the specific endpoint).
- Add `POST /api/admin/inventory/aggregate/sync` protected by JWT and role check.

## Verification Plan

### Automated Tests
- I will create a test script or use `curl` commands to:
    1. Register a new Admin.
    2. Login to get a token (verify token contains `role: ADMIN`).
    3. Create some products and perform scans (IN/OUT).
    4. Call `POST /api/admin/inventory/aggregate/sync`.
    5. Verify `daily_aggregates` table entries for today are updated and `updated_at` reflects the sync time.

### Manual Verification
- Use `test_all_endpoints.ps1` (if applicable) or manual `curl` calls.
- Verify that accessing the sync endpoint without a token or with a non-admin token (if possible to create one) returns 401/403.
- Check DB directly to confirm `updated_at` change.
