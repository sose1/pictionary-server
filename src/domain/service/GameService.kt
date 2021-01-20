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
import java.io.File
import javax.imageio.ImageIO


class GameService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) {
    private val system = User("SYSTEM", "0")
    private val file = File("src/domain/resources/words")

    init {
        for (i in 1..5) {
            gameRepository.save(Game(null))
        }
    }

    suspend fun saveUserInGame(userName: String, sessionId: String, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val user = User(userName, sessionId)

        if (game.owner == null) {
            game.owner = user
        }

        game.users.add(user)
        userRepository.save(user)

        eventPublisher.send(user.id, ResponseEvent.NewUser(user))

        eventPublisher.broadcast(gameId,
            ResponseEvent.Message("$userName dołącza do gry!", system)
        )

        if (game.users.size == 2) {
            game.isStarted = true

            val painterId = game.users.first().id
            game.painterId = painterId
            game.newWordGuess(file)

            eventPublisher.send(painterId, ResponseEvent.Painter(game.wordGuess))
            eventPublisher.broadcast(gameId, ResponseEvent.GameStarted(game.isStarted))

            val wordGuessInUnder = game.wordGuess.replace("\\S".toRegex(),"_")
            eventPublisher.broadcast(gameId, ResponseEvent.Guessing(wordGuessInUnder))
        }
        gameRepository.save(game)
    }

    suspend fun removeUserFromGame(sessionId: String, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val disconnectedUser = userRepository.findBySessionId(sessionId) ?: throw Exception()

        game.users.remove(disconnectedUser)

        eventPublisher.broadcast(gameId,
            ResponseEvent.Message("${disconnectedUser.name} wyszedł z gry!", system)
        )

        if (game.owner?.id == disconnectedUser.id) {
            game.owner = game.users.randomOrNull()
            game.owner?.let {
                eventPublisher.send(it.id, ResponseEvent.NewOwner(it))
            }
        }

        if (game.users.size < 2) {
            game.isStarted = false
            game.messages.clear()
            game.image = BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB)
            sendImage(game)

            eventPublisher.broadcast(gameId, ResponseEvent.GameStarted(game.isStarted))
            eventPublisher.broadcast(gameId,
                ResponseEvent.Message("DO ROZPOCZĘCIA GRY POTRZEBA 2 GRACZY", system)
            )
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

        if (author.id != game.painterId) {
            game.messages.add(message)
            eventPublisher.broadcast(gameId, ResponseEvent.Message(message.content, message.author))
        } else {
            eventPublisher.send(author.id, ResponseEvent.Message("Gdy rysujesz nie możesz wysłać wiadomośći na chat!", system))
        }

        if (game.wordGuess == text.toLowerCase() && author.id != game.painterId) {
            eventPublisher.broadcast(gameId,
                ResponseEvent.Message("${author.name} odgadł hasło.\nHasło: ${game.wordGuess}!", system)
            )
            game.image = BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB)

            sendImage(game)

            game.newPainter()
            game.newWordGuess(file)

            sendImage(game)

            eventPublisher.send(game.painterId, ResponseEvent.Painter(game.wordGuess))
            val wordGuessInUnder = game.wordGuess.replace("\\S".toRegex(),"_")
            eventPublisher.broadcast(gameId, ResponseEvent.Guessing(wordGuessInUnder))
        }

        gameRepository.save(game)
    }

    suspend fun onPathDrawn(byteArray: ByteArray, gameId: String) {
        val game = gameRepository.findById(gameId) ?: throw Exception()
        val image = game.image
        val newImage = ImageIO.read(ByteArrayInputStream(byteArray))

        game.image = combinedImage(image, newImage)
        gameRepository.save(game)

        sendImage(game)
    }

    private suspend fun sendImage(game: Game) {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(game.image, "PNG", outputStream)

        val byte = outputStream.toByteArray()

        eventPublisher.byteBroadcast(game.id, byte)
    }

    private fun combinedImage(image: BufferedImage, newImage: BufferedImage): BufferedImage {
        val combined = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = combined.graphics

        graphics.drawImage(image, 0, 0, null)
        graphics.drawImage(newImage, 0, 0, null)
        graphics.dispose()

        return combined
    }
}