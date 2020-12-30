package domain.event

import pl.sose1.domain.entity.User


sealed class ResponseEvent {

    class AllUsers(val users: List<User>) : ResponseEvent()
    class NewOwner(val user: User) : ResponseEvent()
    class Message(val content: String, val author: User) : ResponseEvent()
    class NewUser(val user: User) : ResponseEvent()
    class GameStarted(val isStarted: Boolean) : ResponseEvent()
}