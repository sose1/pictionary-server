package application.model.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.sose1.domain.entity.User

@Serializable
sealed class GameResponse

@Serializable
@SerialName("Users")
class Users(
        val users: List<User>
) : GameResponse()

@Serializable
@SerialName("NewOwner")
class NewOwner(
        val user: User
) : GameResponse()

@Serializable
@SerialName("Message")
class Message(
        val content: String,
        val author: User
) : GameResponse()

@Serializable
@SerialName("NewUser")
class NewUser(
    val user: User
) : GameResponse()

@Serializable
@SerialName("GameStarted")
class GameStarted(
    val isStarted: Boolean
) : GameResponse()