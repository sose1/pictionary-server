package pl.sose1.model.lobby

import kotlinx.serialization.Serializable
import pl.sose1.model.user.User
import java.util.*

@Serializable
class Lobby(
    var creatorId: String) {
    private val charPool = ('A'..'Z') + ('0'..'9')
    var users: MutableList<User> = mutableListOf()
    val lobbyId: String = UUID.randomUUID().toString()
    val code = (1 .. 5)
            .map { charPool.random() }
            .joinToString("")
    val messages: MutableList<Message> = mutableListOf()
}