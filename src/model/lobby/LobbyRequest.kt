package pl.sose1.model.lobby

import java.util.*

data class LobbyRequest(val userId: UUID, val eventName: RequestEventName)