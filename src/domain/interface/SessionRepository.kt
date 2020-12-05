package pl.sose1.domain.`interface`

import io.ktor.http.cio.websocket.*

interface SessionRepository {

    fun save(sessionId: String, socket: WebSocketSession)

    fun findById(sessionId: String): WebSocketSession?

    fun findAllByIds(ids: List<String>): List<WebSocketSession>

    fun delete(sessionId: String)
}