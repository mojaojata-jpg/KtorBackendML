package infrastructure.service

import domain.model.IotOperationMode
import domain.model.IotSystemStatus
import java.util.concurrent.atomic.AtomicReference

class IotModeService {
    private val status = AtomicReference(IotSystemStatus())

    fun setMode(mode: IotOperationMode, productId: String?) {
        status.set(IotSystemStatus(mode, productId))
    }

    fun getCurrentStatus(): IotSystemStatus {
        return status.get()
    }

    fun resetToNormal() {
        status.set(IotSystemStatus(IotOperationMode.NORMAL, null))
    }
}
