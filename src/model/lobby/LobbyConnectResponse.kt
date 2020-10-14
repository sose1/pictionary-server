package pl.sose1.model.lobby

import pl.sose1.model.user.User
import java.util.*

data class LobbyConnectResponse(val user: User, val lobbyId: UUID, val code: String, val creatorId: UUID)