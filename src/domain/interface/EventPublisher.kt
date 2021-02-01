package pl.sose1.domain.`interface`

import domain.event.ResponseEvent

interface EventPublisher {
    suspend fun send(userId: String, event: ResponseEvent)

    suspend fun broadcast(gameId: String, event: ResponseEvent)
    suspend fun broadcastExcept(gameId: String, painterId: String, event: ResponseEvent)
    suspend fun byteBroadcast(gameId: String, painterId: String, byteArray: ByteArray)

}