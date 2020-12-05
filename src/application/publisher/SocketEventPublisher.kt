package pl.sose1.application.publisher

import application.model.game.*
import domain.event.ResponseEvent
import pl.sose1.application.utils.sendJson
import pl.sose1.domain.`interface`.EventPublisher
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.SessionRepository

class SocketEventPublisher(
    private val gameRepository: GameRepository,
    private val sessionRepository: SessionRepository
) : EventPublisher {

    override suspend fun broadcast(gameId: String, event: ResponseEvent) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val sockets = sessionRepository.findAllByIds(game.users.map { it.id })

        val gameResponse = event.toApplicationEvent()

        sockets.forEach {
            it.outgoing.sendJson(gameResponse)
        }
    }

    override suspend fun send(userId: String, event: ResponseEvent) {
        val session = sessionRepository.findById(userId)

        session?.outgoing?.sendJson(event.toApplicationEvent())
    }

    private fun ResponseEvent.toApplicationEvent(): GameResponse =
        when (this) {
            is ResponseEvent.NewOwner -> NewOwner(user)
            is ResponseEvent.AllUsers -> Users(users)
            is ResponseEvent.Message -> Message(content, author)
            is ResponseEvent.NewUser -> NewUser(user)
        }
}