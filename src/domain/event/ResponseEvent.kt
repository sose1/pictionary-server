package domain.event

import pl.sose1.domain.entity.User


sealed class ResponseEvent {

    class NewOwner(val user: User) : ResponseEvent()
    class Message(val content: String, val author: User) : ResponseEvent()
    class NewUser(val user: User) : ResponseEvent()
    class GameStarted(val isStarted: Boolean) : ResponseEvent()
    class Painter(val wordGuess: String): ResponseEvent()
    class FirstRoundStarted(val newWordGuess: String, val isPainter: Boolean ) : ResponseEvent()
    class NextRoundStarted(
        val userNameWhoGuessed: String,
        val oldWordGuess: String,
        val newWordGuess: String,
        val isPainter: Boolean) : ResponseEvent()
}

