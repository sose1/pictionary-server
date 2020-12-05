package pl.sose1.infrastructure.repository

import io.ktor.http.cio.websocket.*
import pl.sose1.domain.`interface`.SessionRepository

class CacheSessionRepository : SessionRepository {

    private val sessions = mutableMapOf<String, WebSocketSession>()

    override fun save(sessionId: String, socket: WebSocketSession) {
        sessions[sessionId] = socket
    }

    override fun findById(sessionId: String): WebSocketSession? =
        sessions[sessionId]

    override fun findAllByIds(ids: List<String>): List<WebSocketSession> =
        sessions.filterKeys { ids.contains(it) }.values.toList()

    override fun delete(sessionId: String) {
        sessions.remove(sessionId)
    }
}