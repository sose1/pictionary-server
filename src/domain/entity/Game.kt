package pl.sose1.domain.entity

import application.model.game.Message
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.awt.image.BufferedImage
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

    val id: String = UUID.randomUUID().toString()
    val code = generateCode()

    val users: MutableList<User> = mutableListOf()
    val messages: MutableList<Message> = mutableListOf()


    fun sessionBelongsToGame(sessionId: String): Boolean =
        users.find { sessionId == it.id } != null

    companion object {
        private val charPool = ('A'..'Z') + ('0'..'9')

        private fun generateCode(): String =
                (1 .. 5).map { charPool.random() }
                        .joinToString("")
    }
}