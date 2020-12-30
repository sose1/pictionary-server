package pl.sose1.domain.service

import application.model.game.Message
import domain.event.ResponseEvent
import pl.sose1.domain.`interface`.EventPublisher
import pl.sose1.domain.`interface`.GameRepository
import pl.sose1.domain.`interface`.UserRepository
import pl.sose1.domain.entity.Game
import pl.sose1.domain.entity.User
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class GameService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) {
    private val system = User("SYSTEM", "0")

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

        val message = Message("$userName dołącza do gry!", system)
        eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))


        if (game.users.size >= 2) {
            game.isStarted = true
            eventPublisher.broadcast(gameId, ResponseEvent.GameStarted(game.isStarted))
        } else {
            val message = Message("DO ROZPOCZĘCIA GRY POTRZEBA 2 GRACZY", system)
            eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))
        }

        userRepository.save(user)
        gameRepository.save(game)

        eventPublisher.send(user.id, ResponseEvent.NewUser(user))
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


        val message = Message("${disconnectedUser.name} wyszedł z gry!", system)
        eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))

        if (game.users.size < 2) {
            game.isStarted = false
            eventPublisher.broadcast(gameId, ResponseEvent.GameStarted(game.isStarted))
            game.messages.clear()

            val message = Message("DO ROZPOCZĘCIA GRY POTRZEBA 2 GRACZY", system)
            eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))
        }

        if (game.users.isEmpty()) {
            game.messages.clear()
        }

        userRepository.deleteBySessionId(disconnectedUser.id)
        gameRepository.save(game)
    }

    suspend fun sendMessage(text: String, gameId: String, sessionId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val author = userRepository.findBySessionId(sessionId) ?: throw Exception()

        val message = Message(text, author)

        game.messages.add(message)
        gameRepository.save(game)

        eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))
    }

    suspend fun onPathDrawn(byteArray: ByteArray, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()

        val newImage = ImageIO.read(ByteArrayInputStream(byteArray))
        val image = game.image

        val combined = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

        val graphics = combined.graphics
        graphics.drawImage(image, 0, 0, null)
        graphics.drawImage(newImage, 0, 0, null)
        graphics.dispose()
        game.image = combined

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(game.image, "PNG", outputStream)

        val byte = outputStream.toByteArray()

        gameRepository.save(game)
        eventPublisher.byteBroadcast(gameId, byte)
    }
}