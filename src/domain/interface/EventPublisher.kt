package pl.sose1.domain.`interface`

import domain.event.ResponseEvent

interface EventPublisher {

    suspend fun broadcast(gameId: String, event: ResponseEvent)

    suspend fun send(userId: String, event: ResponseEvent)
}