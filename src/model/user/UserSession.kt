package pl.sose1.model.user

import io.ktor.http.cio.websocket.*
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserSession(
    val session: DefaultWebSocketSession
) {
    val id = UUID.randomUUID().toString()
}
