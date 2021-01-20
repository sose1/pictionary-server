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

    override suspend fun broadcast(gameId: String, event: ResponseEvent) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val sockets = sessionRepository.findAllByIds(game.users.map { it.id })

        when (val gameResponse = event.toApplicationEvent()) {
            is Guessing -> {
                val users = game.users.minus(
                    game.users.find { user -> user.id == game.painterId }
                )
                users.forEach { println(it?.name) }
                val sockets = sessionRepository.findAllByIds(users.map { it!!.id })
                sockets.forEach {
                    it.outgoing.sendJson(gameResponse as GameResponse)
                }
            }
            else -> {
                sockets.forEach {
                    it.outgoing.sendJson(gameResponse)
                }
            }
        }
    }

    override suspend fun send(userId: String, event: ResponseEvent) {
        val session = sessionRepository.findById(userId)
        session?.outgoing?.sendJson(event.toApplicationEvent())
    }

    override suspend fun byteBroadcast(gameId: String, byteArray: ByteArray) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val users = game.users.minus(
            game.users.find { user -> user.id == game.painterId }
        )

        val sockets = sessionRepository.findAllByIds(users.map { it!!.id })

        sockets.forEach {
            it.outgoing.send(Frame.Binary(true,byteArray))
        }
    }

    private fun ResponseEvent.toApplicationEvent(): GameResponse =
        when (this) {
            is ResponseEvent.NewOwner -> NewOwner(user)
            is ResponseEvent.AllUsers -> Users(users)
            is ResponseEvent.Message -> Message(content, author)
            is ResponseEvent.NewUser -> NewUser(user)
            is ResponseEvent.GameStarted -> GameStarted(isStarted)
            is ResponseEvent.Painter -> Painter(wordGuess)
            is ResponseEvent.Guessing -> Guessing(wordGuessInUnder)
        }
}