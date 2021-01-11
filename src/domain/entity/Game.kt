package pl.sose1.domain.entity

import application.model.game.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.image.BufferedImage
import java.io.File
import java.util.*

@Serializable
@SerialName("Game")
class Game(
        var owner: User?,
        var isStarted: Boolean = false,
) {
    init {
        owner?.let {
            users.add(it)
        }
    }

    @Transient
    var image = BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB)

    @Transient
    var wordGuess = ""

    val id: String = UUID.randomUUID().toString()
    val code = generateCode()

    val users: MutableList<User> = mutableListOf()
    val messages: MutableList<Message> = mutableListOf()

    var painterId: String = ""

    fun sessionBelongsToGame(sessionId: String): Boolean =
        users.find { sessionId == it.id } != null

    fun newPainter(){
        var painter = users.random()

        while (painterId == painter.id) {
            painter = users.random()
        }
        painterId = painter.id
    }

    fun newWordGuess(file: File) {
        var word = file.readLines().random()

        while (wordGuess == word) {
            word = file.readLines().random()
        }
        wordGuess = word
    }

    companion object {
        private val charPool = ('A'..'Z') + ('0'..'9')

        private fun generateCode(): String =
                (1 .. 5).map { charPool.random() }
                        .joinToString("")
    }
}