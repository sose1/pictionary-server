package pl.sose1.application.publisher

import application.model.game.*
import domain.event.ResponseEvent
import io.ktor.http.cio.websocket.*
import pl.sose1.application.utils.sendJson
import pl.sose1.domain.`interface`.EventPublisher
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.SessionRepository

class SocketEventPublisher(
    private val gameRepository: GameRepository,
    private val sessionRepository: SessionRepository
) : EventPublisher {

    override suspend fun send(userId: String, event: ResponseEvent) {
        val session = sessionRepository.findById(userId)
        session?.outgoing?.sendJson(event.toApplicationEvent())
    }

    override suspend fun broadcast(gameId: String, event: ResponseEvent) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val sockets = sessionRepository.findAllByIds(game.users.map { it.id })

        val gameResponse = event.toApplicationEvent()
        sockets.forEach {
            it.outgoing.sendJson(gameResponse)
        }
    }

    override suspend fun broadcastExcept(gameId: String, exceptId: String, event: ResponseEvent) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val sockets = sessionRepository.findAllByIdsExcept(game.users.map { it.id }, exceptId)

        val gameResponse = event.toApplicationEvent()
        sockets.forEach {
            it.outgoing.sendJson(gameResponse)
        }
    }

    override suspend fun byteBroadcast(gameId: String, exceptId: String, byteArray: ByteArray) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val sockets = sessionRepository.findAllByIdsExcept(game.users.map { it.id }, exceptId)

        sockets.forEach {
            it.outgoing.send(Frame.Binary(true,byteArray))
        }
    }

    private fun ResponseEvent.toApplicationEvent(): GameResponse =
        when (this) {
            is ResponseEvent.Message -> Message(content, author)
            is ResponseEvent.NewOwner -> NewOwnerAdded(user)
            is ResponseEvent.NewUser -> NewUserAdded(user)
            is ResponseEvent.GameStarted -> GameStarted(isStarted)
            is ResponseEvent.FirstRoundStarted -> FirstRoundStarted(newWordGuess, isPainter)
            is ResponseEvent.NextRoundStarted -> NextRoundStarted(userNameWhoGuessed, oldWordGuess, newWordGuess, isPainter)
        }
}