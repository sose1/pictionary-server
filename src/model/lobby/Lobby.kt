package pl.sose1.model.lobby

import java.util.*

class Lobby {
    private val charPool = ('A'..'Z') +  ('0'..'9')

    val users: MutableList<String>? = null

    val id = UUID.randomUUID();
    val lobbyCode = (1 .. 5)
            .map { charPool.random() }
            .joinToString("")
}