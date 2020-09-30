package pl.sose1.model.lobby

import pl.sose1.model.user.User
import java.util.*

class Lobby {
    private val charPool = ('A'..'Z') +  ('0'..'9')

    var users: MutableList<User> = mutableListOf()
    val lobbyID = UUID.randomUUID();
    val code = (1 .. 5)
            .map { charPool.random() }
            .joinToString("")
}