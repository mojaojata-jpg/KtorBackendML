package presentation.controller

import application.usecase.inventory.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import presentation.dto.response.*
import java.util.UUID

class InventoryController(
    private val processRfidScanUseCase: ProcessRfidScanUseCase,
    private val registerRfidTagUseCase: RegisterRfidTagUseCase,
    private val getInventoryDashboardUseCase: GetInventoryDashboardUseCase,
    private val getInventoryHistoryUseCase: GetInventoryHistoryUseCase,
    private val inventoryRepository: domain.repository.InventoryRepository,
    private val iotModeService: infrastructure.service.IotModeService
) {
    @Serializable
    data class ScanRequest(val tag_uid: String, val event_type: String? = null, val note: String? = null)

    @Serializable
    data class RegisterTagRequest(val product_id: String, val tag_uid: String, val tag_label: String? = null)

    @Serializable
    data class IotModeRequest(val mode: String, val product_id: String? = null)

    suspend fun setIotMode(call: ApplicationCall) {
        val request = call.receive<IotModeRequest>()
        val mode = try {
            domain.model.IotOperationMode.valueOf(request.mode.uppercase())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid mode. Use REGISTER, SCAN_OUT, or NORMAL")
        }

        if (mode == domain.model.IotOperationMode.REGISTER && request.product_id == null) {
            throw IllegalArgumentException("Product ID is required for REGISTER mode")
        }

        iotModeService.setMode(mode, request.product_id)
        call.respond(HttpStatusCode.OK, BaseResponse<Unit>(
            success = true,
            message = "IoT Mode updated to $mode for product ${request.product_id ?: "NONE"}"
        ))
    }

    suspend fun processScan(call: ApplicationCall) {
        val request = try {
            call.receive<ScanRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format")
        }

        val iotStatus = iotModeService.getCurrentStatus()

        // OPSI A: SMART BACKEND LOGIC
        if (iotStatus.mode == domain.model.IotOperationMode.REGISTER && iotStatus.targetProductId != null) {
            // Jalankan registrasi otomatis
            val (tag, snapshot) = registerRfidTagUseCase(
                productId = iotStatus.targetProductId,
                tagUid = request.tag_uid,
                tagLabel = "Auto Registered via IotMode",
                adminId = null // Bisa diisi ID admin dari session jika perlu
            )
            
            call.respond(HttpStatusCode.OK, BaseResponse<RegisterTagResponse>(
                success = true,
                message = "Tag ${request.tag_uid} AUTO-REGISTERED to product ${iotStatus.targetProductId}",
                data = RegisterTagResponse(tag.id.toString(), snapshot.currentStock)
            ))
            return
        }

        if (iotStatus.mode == domain.model.IotOperationMode.SCAN_OUT) {
            val (event, snapshot) = processRfidScanUseCase(
                tagUid = request.tag_uid,
                eventType = "OUT",
                note = "Continuous Scan Out Mode",
                isContinuousMode = false
            )

            call.respond(HttpStatusCode.OK, BaseResponse<ScanResponse>(
                success = true,
                data = ScanResponse(new_stock = snapshot.currentStock, status = snapshot.status),
                message = "Scan processed as OUT (Continuous Mode)"
            ))
            return
        }
        
        if (iotStatus.mode == domain.model.IotOperationMode.SCAN_IN) {
            val (event, snapshot) = processRfidScanUseCase(
                tagUid = request.tag_uid,
                eventType = "IN",
                note = "Continuous Scan In Mode",
                isContinuousMode = false
            )

            call.respond(HttpStatusCode.OK, BaseResponse<ScanResponse>(
                success = true,
                data = ScanResponse(new_stock = snapshot.currentStock, status = snapshot.status),
                message = "Scan processed as IN (Continuous Mode)"
            ))
            return
        }

        // Jika mode NORMAL (Atau default ke OUT jika ESP32 gak kirim event_type)
        val eventType = request.event_type ?: "OUT" 
        
        val (event, snapshot) = processRfidScanUseCase(
            tagUid = request.tag_uid,
            eventType = eventType,
            note = request.note
        )

        call.respond(HttpStatusCode.OK, BaseResponse<ScanResponse>(
            success = true,
            data = ScanResponse(new_stock = snapshot.currentStock, status = snapshot.status),
            message = "Scan processed as $eventType"
        ))
    }

    suspend fun registerTag(call: ApplicationCall) {
        val request = try {
            call.receive<RegisterTagRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields (product_id, tag_uid)")
        }

        // 1. Validasi Input
        require(request.product_id.isNotBlank()) { "Product ID is required" }
        require(request.tag_uid.isNotBlank()) { "Tag UID is required" }

        val adminId = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()?.payload?.getClaim("id")?.asString()

        val (tag, snapshot) = registerRfidTagUseCase(
            productId = request.product_id,
            tagUid = request.tag_uid,
            tagLabel = request.tag_label,
            adminId = adminId
        )

        call.respond(HttpStatusCode.Created, BaseResponse<RegisterTagResponse>(
            success = true,
            data = RegisterTagResponse(
                tag_id = tag.id.toString(),
                current_stock = snapshot.currentStock
            ),
            message = "Tag registered successfully"
        ))
    }

    suspend fun deleteTag(call: ApplicationCall) {
        val productId = call.parameters["productId"] ?: throw IllegalArgumentException("Product ID required")
        val isDeleted = inventoryRepository.deleteTagByProductId(UUID.fromString(productId))
        
        if (isDeleted) {
            call.respond(HttpStatusCode.OK, BaseResponse<Unit>(
                success = true,
                message = "Tag unregistered successfully from product"
            ))
        } else {
            call.respond(HttpStatusCode.NotFound, BaseResponse<Unit>(
                success = false,
                message = "No tag found for this product"
            ))
        }
    }

    suspend fun getDashboard(call: ApplicationCall) {
        val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 30
        val startDate = java.time.LocalDate.now().minusDays(days.toLong())
        val today = java.time.LocalDate.now()

        val dashboardData = getInventoryDashboardUseCase()
        val productIds = dashboardData.map { it.first.id!! }
        val batchStats = inventoryRepository.getBatchProductStats(productIds, startDate, today)

        val responseData = dashboardData.map { (product, snapshot) ->
            val stats = batchStats[product.id] ?: Pair(0, 0)
            DashboardResponse(
                product_id = product.id.toString(),
                product_name = product.name,
                product_code = product.code,
                current_stock = snapshot?.currentStock ?: 0,
                total_incoming = stats.first,
                total_outgoing = stats.second,
                status = snapshot?.status ?: "OUT_OF_STOCK",
                unit = product.unitLabel
            )
        }
        call.respond(HttpStatusCode.OK, BaseResponse<List<DashboardResponse>>(success = true, data = responseData))
    }

    suspend fun getHistory(call: ApplicationCall) {
        val productId = call.parameters["productId"] ?: throw IllegalArgumentException("Product ID is required")
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val days = call.request.queryParameters["days"]?.toIntOrNull()
        
        val history = getInventoryHistoryUseCase(productId, limit, days)
        val responseData = history.map {
            HistoryResponse(
                event_type = it.eventType,
                quantity = it.quantity,
                recorded_at = it.recordedAt.toString(),
                note = it.note
            )
        }
        call.respond(HttpStatusCode.OK, BaseResponse<List<HistoryResponse>>(success = true, data = responseData))
    }
}
