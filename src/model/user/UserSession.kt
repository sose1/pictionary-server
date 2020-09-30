package pl.sose1.model.user

import io.ktor.http.cio.websocket.*
import java.util.*

data class UserSession(val session: DefaultWebSocketSession) {
    val id = UUID.randomUUID()
}
