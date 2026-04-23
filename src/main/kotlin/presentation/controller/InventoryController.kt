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
    private val inventoryRepository: domain.repository.InventoryRepository
) {
    @Serializable
    data class ScanRequest(val tag_uid: String, val event_type: String, val note: String? = null)

    @Serializable
    data class RegisterTagRequest(val product_id: String, val tag_uid: String, val tag_label: String? = null)

    suspend fun processScan(call: ApplicationCall) {
        val request = try {
            call.receive<ScanRequest>()
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format or missing required fields (tag_uid, event_type)")
        }

        // 1. Validasi Input
        require(request.tag_uid.isNotBlank()) { "Tag UID is required" }
        require(request.event_type.isNotBlank()) { "Event type (IN/OUT) is required" }
        require(request.event_type in listOf("IN", "OUT")) { "Event type must be either 'IN' or 'OUT'" }

        // Optional: Get adminId if scanning is done via authenticated handheld
        val adminId = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()?.payload?.getClaim("id")?.asString()?.let { UUID.fromString(it) }
        
        val (event, snapshot) = processRfidScanUseCase(
            tagUid = request.tag_uid,
            eventType = request.event_type,
            adminId = adminId,
            note = request.note
        )

        call.respond(HttpStatusCode.OK, BaseResponse(
            success = true,
            data = ScanResponse(
                new_stock = snapshot.currentStock,
                status = snapshot.status
            ),
            message = "Scan processed: ${request.event_type}"
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

        call.respond(HttpStatusCode.Created, BaseResponse(
            success = true,
            data = RegisterTagResponse(
                tag_id = tag.id.toString(),
                current_stock = snapshot.currentStock
            ),
            message = "Tag registered successfully"
        ))
    }

    suspend fun getDashboard(call: ApplicationCall) {
        val dashboardData = getInventoryDashboardUseCase()
        val responseData = dashboardData.map { (product, snapshot) ->
            val (incoming, outgoing) = inventoryRepository.getProductStats(product.id!!)
            DashboardResponse(
                product_id = product.id.toString(),
                product_name = product.name,
                product_code = product.code,
                current_stock = snapshot?.currentStock ?: 0,
                total_incoming = incoming,
                total_outgoing = outgoing,
                status = snapshot?.status ?: "OUT_OF_STOCK",
                unit = product.unitLabel
            )
        }
        call.respond(HttpStatusCode.OK, BaseResponse(success = true, data = responseData))
    }

    suspend fun getHistory(call: ApplicationCall) {
        val productId = call.parameters["productId"] ?: throw IllegalArgumentException("Product ID is required")
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        
        val history = getInventoryHistoryUseCase(productId, limit)
        val responseData = history.map {
            HistoryResponse(
                event_type = it.eventType,
                quantity = it.quantity,
                recorded_at = it.recordedAt.toString(),
                note = it.note
            )
        }
        call.respond(HttpStatusCode.OK, BaseResponse(success = true, data = responseData))
    }
}
