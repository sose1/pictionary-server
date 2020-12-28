package pl.sose1.domain.service

import application.model.game.Message
import domain.event.ResponseEvent
import pl.sose1.domain.`interface`.EventPublisher
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.UserRepository
import pl.sose1.domain.entity.Game
import pl.sose1.domain.entity.User

class GameService(
        private val gameRepository: GameRepository,
        private val userRepository: UserRepository,
        private val eventPublisher: EventPublisher
) {

    init {

        gameRepository.save(Game(null))
        gameRepository.save(Game(null))
        gameRepository.save(Game(null))
    }

    suspend fun saveUserInGame(userName: String, sessionId: String, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val user = User(userName, sessionId)


        if (game.owner == null) {
            game.owner = user
        }

        game.users.add(user)

        userRepository.save(user)
        gameRepository.save(game)

        eventPublisher.send(user.id, ResponseEvent.NewUser(user))
        eventPublisher.broadcast(gameId, ResponseEvent.AllUsers(game.users))
    }

    suspend fun removeUserFromGame(sessionId: String, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val disconnectedUser = userRepository.findBySessionId(sessionId) ?: throw Exception()

        game.users.remove(disconnectedUser)

        if (game.owner?.id == disconnectedUser.id) {
            val newOwner = game.users.randomOrNull()

            game.owner = newOwner
            newOwner?.let {
                eventPublisher.send(it.id, ResponseEvent.NewOwner(it))
            }
        }

        if (game.users.isEmpty()) {
            game.messages.clear()
        }

        userRepository.deleteBySessionId(disconnectedUser.id)
        gameRepository.save(game)

        eventPublisher.broadcast(gameId, ResponseEvent.AllUsers(game.users))
    }

    suspend fun sendMessage(text: String, gameId: String, sessionId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val author = userRepository.findBySessionId(sessionId) ?: throw Exception()

        val message = Message(text, author)

        game.messages.add(message)
        gameRepository.save(game)

        eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))
    }

   suspend fun sendByteArray(byteArray: ByteArray, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()

        eventPublisher.byteBroadcast(gameId, byteArray)
    }
}