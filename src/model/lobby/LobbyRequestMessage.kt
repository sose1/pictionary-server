package pl.sose1.model.lobby

import com.fasterxml.jackson.databind.ObjectMapper

data class LobbyRequestMessage(val userName: String, val eventName: String, val code: String? = null)
fun Any.toJSON(): String = ObjectMapper().writeValueAsString(this)