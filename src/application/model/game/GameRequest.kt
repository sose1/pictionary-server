package application.model.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GameRequest

@Serializable
@SerialName("SendMessage")
class SendMessage(
        val text: String
) : GameRequest()