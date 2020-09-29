package pl.sose1.repository

import io.ktor.http.cio.websocket.*
import java.util.*

data class Client(val session: DefaultWebSocketSession) {
    val id = UUID.randomUUID()
    val name = "user$id"
}
